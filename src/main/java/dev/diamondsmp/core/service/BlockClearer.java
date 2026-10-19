package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.storage.PlacedBlock;
import dev.diamondsmp.core.storage.Storage;
import dev.diamondsmp.core.storage.StorageException;
import dev.diamondsmp.core.util.Format;
import dev.diamondsmp.core.util.Lookup;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class BlockClearer implements Service {

    private record ChunkKey(String world, int x, int z) {
    }

    private record BlockPosition(int x, int y, int z) {
    }

    private final DiamondSMP plugin;
    private final Map<ChunkKey, Map<BlockPosition, Material>> tracked = new ConcurrentHashMap<>();
    private final Map<UUID, Item> droppedItems = new ConcurrentHashMap<>();
    private final AtomicInteger size = new AtomicInteger();
    private final AtomicInteger countdown = new AtomicInteger();
    private Task ticker = Task.NONE;
    private Task saver = Task.NONE;

    private volatile boolean enabled = true;
    private volatile int intervalSeconds = 300;
    private volatile Set<Integer> warnings = Set.of();
    private volatile Set<Material> materials = Set.of();
    private volatile Set<String> worlds = Set.of();
    private volatile int maxTracked = 500_000;
    private volatile boolean warnedFull;
    private volatile boolean loaded;
    private volatile boolean clearItems = true;
    private volatile Set<Material> protectedItems = Set.of();

    public BlockClearer(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> load() {
        return plugin.database().supply("load the tracked blocks", Storage::loadPlacedBlocks).thenAccept(stored -> {
            for (PlacedBlock block : stored) {
                Material material = Lookup.material(block.material(), null);
                if (material != null) {
                    put(block.world(), block.x(), block.y(), block.z(), material);
                }
            }
            loaded = true;
        });
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        enabled = config.getBoolean("clearer.enabled", true);
        intervalSeconds = (int) Math.max(10L, Format.parseSeconds(config.getString("clearer.interval"), 300L));
        Set<Integer> parsedWarnings = new HashSet<>();
        for (Integer warning : config.getIntegerList("clearer.warnings")) {
            if (warning != null && warning > 0) {
                parsedWarnings.add(warning);
            }
        }
        warnings = Set.copyOf(parsedWarnings);
        materials = Set.copyOf(Lookup.materials(config.getStringList("clearer.blocks")));
        worlds = Set.copyOf(config.getStringList("clearer.worlds"));
        maxTracked = Math.max(1_000, config.getInt("clearer.max-tracked", 500_000));
        clearItems = config.getBoolean("clearer.items.enabled", true);
        protectedItems = Set.copyOf(Lookup.materials(config.getStringList("clearer.items.protected")));
        warnedFull = false;
        countdown.set(intervalSeconds);
        ticker.cancel();
        ticker = enabled ? plugin.scheduler().globalTimer(this::tick, 20L, 20L) : Task.NONE;
        long saveSeconds = Math.max(30L, Format.parseSeconds(config.getString("storage.autosave"), 300L));
        saver.cancel();
        saver = plugin.scheduler().asyncTimer(this::saveAsync, saveSeconds * 20L, saveSeconds * 20L);
    }

    public boolean tracks(Material material) {
        return enabled && materials.contains(material);
    }

    public void track(Block block, Material material) {
        if (!tracks(material) || !worldAllowed(block.getWorld())) {
            return;
        }
        put(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), material);
    }

    public void trackFlow(Block from, Block to) {
        if (!enabled) {
            return;
        }
        Map<BlockPosition, Material> source = tracked.get(chunkOf(from));
        Material material = source == null ? null : source.get(new BlockPosition(from.getX(), from.getY(), from.getZ()));
        if (material != null) {
            put(to.getWorld().getName(), to.getX(), to.getY(), to.getZ(), material);
        }
    }

    public void trackItem(Item item) {
        if (enabled && clearItems && worldAllowed(item.getWorld())) {
            droppedItems.put(item.getUniqueId(), item);
        }
    }

    public void forgetItem(Item item) {
        droppedItems.remove(item.getUniqueId());
    }

    private void clearItems() {
        for (Item item : new ArrayList<>(droppedItems.values())) {
            plugin.scheduler().entity(item, () -> {
                droppedItems.remove(item.getUniqueId());
                if (item.isValid() && !isProtected(item.getItemStack())) {
                    item.remove();
                }
            }, () -> droppedItems.remove(item.getUniqueId()));
        }
    }

    private boolean isProtected(ItemStack stack) {
        return protectedItems.contains(stack.getType()) || plugin.killTokens().isToken(stack);
    }

    public void forget(Block block) {
        ChunkKey key = chunkOf(block);
        Map<BlockPosition, Material> blocks = tracked.get(key);
        if (blocks != null && blocks.remove(new BlockPosition(block.getX(), block.getY(), block.getZ())) != null) {
            size.decrementAndGet();
            if (blocks.isEmpty()) {
                tracked.computeIfPresent(key, (ignored, value) -> value.isEmpty() ? null : value);
            }
        }
    }

    private void put(String world, int x, int y, int z, Material material) {
        if (size.get() >= maxTracked) {
            if (!warnedFull) {
                warnedFull = true;
                plugin.getLogger().warning("The clearer is tracking " + maxTracked
                        + " blocks; new placements are ignored until the next clear.");
            }
            return;
        }
        ChunkKey key = new ChunkKey(world, x >> 4, z >> 4);
        BlockPosition position = new BlockPosition(x, y, z);
        tracked.compute(key, (ignored, blocks) -> {
            Map<BlockPosition, Material> target = blocks == null ? new ConcurrentHashMap<>() : blocks;
            if (target.put(position, material) == null) {
                size.incrementAndGet();
            }
            return target;
        });
    }

    private void tick() {
        int remaining = countdown.decrementAndGet();
        if (remaining <= 0) {
            countdown.set(intervalSeconds);
            clear();
            return;
        }
        if (warnings.contains(remaining)) {
            plugin.messages().broadcast("clearer.warning", "time", remaining);
        }
    }

    public void clear() {
        for (ChunkKey key : new ArrayList<>(tracked.keySet())) {
            World world = Bukkit.getWorld(key.world());
            if (world == null) {
                continue;
            }
            plugin.scheduler().region(world, key.x(), key.z(), () -> clearChunk(world, key));
        }
        if (clearItems) {
            clearItems();
        }
        warnedFull = false;
        plugin.messages().broadcast("clearer.cleared");
    }

    private void clearChunk(World world, ChunkKey key) {
        if (!world.isChunkLoaded(key.x(), key.z())) {
            return;
        }
        Map<BlockPosition, Material> blocks = tracked.remove(key);
        if (blocks == null) {
            return;
        }
        size.addAndGet(-blocks.size());
        for (Map.Entry<BlockPosition, Material> entry : blocks.entrySet()) {
            BlockPosition position = entry.getKey();
            Material material = entry.getValue();
            Block block = world.getBlockAt(position.x(), position.y(), position.z());
            if (block.getType() == material) {
                block.setType(Material.AIR, block.isLiquid());
            }
        }
    }

    private boolean worldAllowed(World world) {
        Set<String> allowed = worlds;
        return allowed.isEmpty() || allowed.contains(world.getName());
    }

    private static ChunkKey chunkOf(Block block) {
        return new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
    }

    private void saveAsync() {
        if (!loaded) {
            return;
        }
        List<PlacedBlock> snapshot = snapshot();
        plugin.database().run("save the tracked blocks", storage -> storage.replacePlacedBlocks(snapshot));
    }

    private List<PlacedBlock> snapshot() {
        List<PlacedBlock> snapshot = new ArrayList<>(Math.max(16, size.get()));
        for (Map.Entry<ChunkKey, Map<BlockPosition, Material>> chunk : tracked.entrySet()) {
            String world = chunk.getKey().world();
            for (Map.Entry<BlockPosition, Material> entry : chunk.getValue().entrySet()) {
                BlockPosition position = entry.getKey();
                snapshot.add(new PlacedBlock(world, position.x(), position.y(), position.z(),
                        entry.getValue().name()));
            }
        }
        return snapshot;
    }

    @Override
    public void shutdown() {
        ticker.cancel();
        saver.cancel();
        if (!loaded) {
            tracked.clear();
            droppedItems.clear();
            size.set(0);
            return;
        }
        List<PlacedBlock> snapshot = snapshot();
        try {
            plugin.database().await(plugin.database().run("save the tracked blocks",
                    storage -> storage.replacePlacedBlocks(snapshot)), 60L);
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.SEVERE, "The clearer could not save its tracked blocks", ex);
        }
        tracked.clear();
        droppedItems.clear();
        size.set(0);
    }
}
