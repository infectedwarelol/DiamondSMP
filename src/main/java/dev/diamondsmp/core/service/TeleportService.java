package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class TeleportService implements Service {

    public static final String BYPASS_PERMISSION = "diamondsmp.teleport.bypass";

    private record Warmup(Task task, String world, int blockX, int blockY, int blockZ) {
    }

    private final DiamondSMP plugin;
    private final Map<UUID, Warmup> warmups = new ConcurrentHashMap<>();
    private final Map<UUID, Map<TeleportKind, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();

    private volatile Map<TeleportKind, Integer> warmupSeconds = new EnumMap<>(TeleportKind.class);
    private volatile Map<TeleportKind, Integer> cooldownSeconds = new EnumMap<>(TeleportKind.class);
    private volatile boolean cancelOnMove = true;
    private volatile boolean cancelOnDamage = true;

    public TeleportService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        Map<TeleportKind, Integer> warmup = new EnumMap<>(TeleportKind.class);
        Map<TeleportKind, Integer> cooldown = new EnumMap<>(TeleportKind.class);
        for (TeleportKind kind : TeleportKind.values()) {
            warmup.put(kind, (int) Format.parseSeconds(config.getString("teleport.warmup." + kind.id()), 0L));
            cooldown.put(kind, (int) Format.parseSeconds(config.getString("teleport.cooldown." + kind.id()), 0L));
        }
        warmupSeconds = warmup;
        cooldownSeconds = cooldown;
        cancelOnMove = config.getBoolean("teleport.cancel-on-move", true);
        cancelOnDamage = config.getBoolean("teleport.cancel-on-damage", true);
    }

    public boolean canStart(Player player, TeleportKind kind) {
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }
        long remaining = cooldownRemaining(player.getUniqueId(), kind);
        if (remaining > 0) {
            plugin.messages().error(player, "teleport.cooldown", "time", Format.duration(remaining));
            return false;
        }
        return true;
    }

    public void request(Player player, TeleportKind kind, Supplier<Location> destination,
                        String arrivedKey, Object... placeholders) {
        if (!canStart(player, kind)) {
            return;
        }
        int delay = player.hasPermission(BYPASS_PERMISSION) ? 0 : warmupSeconds.getOrDefault(kind, 0);
        plugin.scheduler().entity(player, () -> {
            cancelWarmup(player, null);
            if (delay <= 0) {
                finish(player, kind, destination, arrivedKey, placeholders);
            } else {
                startWarmup(player, kind, delay, destination, arrivedKey, placeholders);
            }
        });
    }

    private void startWarmup(Player player, TeleportKind kind, int seconds, Supplier<Location> destination,
                             String arrivedKey, Object[] placeholders) {
        UUID uuid = player.getUniqueId();
        countdown(player, seconds);
        int[] countdown = {seconds};
        Task[] holder = new Task[1];
        holder[0] = plugin.scheduler().entityTimer(player, () -> {
            Warmup current = warmups.get(uuid);
            if (current == null || current.task() != holder[0] || !player.isOnline()) {
                holder[0].cancel();
                return;
            }
            countdown[0]--;
            if (countdown[0] > 0) {
                countdown(player, countdown[0]);
                return;
            }
            holder[0].cancel();
            warmups.remove(uuid, current);
            finish(player, kind, destination, arrivedKey, placeholders);
        }, 20L, 20L);
        Location start = player.getLocation();
        World world = start.getWorld();
        warmups.put(uuid, new Warmup(holder[0], world == null ? "" : world.getName(),
                start.getBlockX(), start.getBlockY(), start.getBlockZ()));
    }

    private void countdown(Player player, int seconds) {
        plugin.messages().send(player, "teleport.warmup", "time", seconds);
        plugin.messages().actionBar(player, "teleport.warmup-bar", "time", seconds);
    }

    private void finish(Player player, TeleportKind kind, Supplier<Location> destination,
                        String arrivedKey, Object[] placeholders) {
        Location target = destination.get();
        if (target == null || target.getWorld() == null) {
            plugin.messages().error(player, "teleport.invalid-destination");
            return;
        }
        if (!player.hasPermission(BYPASS_PERMISSION)) {
            startCooldown(player.getUniqueId(), kind);
        }
        teleport(player, target).thenAccept(success -> plugin.messages()
                .outcome(player, success, arrivedKey, "teleport.failed", placeholders));
    }

    public CompletableFuture<Boolean> teleport(Player player, Location destination) {
        return teleport(player, destination, true);
    }

    public CompletableFuture<Boolean> teleport(Player player, Location destination, boolean rememberBack) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        plugin.scheduler().entity(player, () -> {
            if (player.isInsideVehicle()) {
                player.leaveVehicle();
            }
            if (rememberBack) {
                rememberBack(player, player.getLocation());
            }
            player.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    .whenComplete((success, error) -> result.complete(error == null && Boolean.TRUE.equals(success)));
        }, () -> result.complete(false));
        return result;
    }

    public void moved(Player player, Location to) {
        if (!cancelOnMove) {
            return;
        }
        Warmup warmup = warmups.get(player.getUniqueId());
        if (warmup == null || to == null || to.getWorld() == null) {
            return;
        }
        boolean changed = to.getBlockX() != warmup.blockX() || to.getBlockY() != warmup.blockY()
                || to.getBlockZ() != warmup.blockZ() || !to.getWorld().getName().equals(warmup.world());
        if (changed) {
            cancelWarmup(player, "teleport.cancelled-move");
        }
    }

    public void damaged(Player player) {
        if (cancelOnDamage) {
            cancelWarmup(player, "teleport.cancelled-damage");
        }
    }

    public void cancelWarmup(Player player, String reasonKey) {
        Warmup warmup = warmups.remove(player.getUniqueId());
        if (warmup == null) {
            return;
        }
        warmup.task().cancel();
        if (reasonKey != null) {
            plugin.messages().error(player, reasonKey);
        }
    }

    public void rememberBack(Player player, Location location) {
        if (location != null && location.getWorld() != null) {
            backLocations.put(player.getUniqueId(), location.clone());
        }
    }

    public Location back(Player player) {
        Location location = backLocations.get(player.getUniqueId());
        return location == null ? null : location.clone();
    }

    public void quit(Player player) {
        cancelWarmup(player, null);
        UUID uuid = player.getUniqueId();
        Map<TeleportKind, Long> active = cooldowns.get(uuid);
        if (active != null) {
            long now = System.currentTimeMillis();
            active.values().removeIf(until -> until <= now);
            if (active.isEmpty()) {
                cooldowns.remove(uuid, active);
            }
        }
    }

    private long cooldownRemaining(UUID uuid, TeleportKind kind) {
        Map<TeleportKind, Long> active = cooldowns.get(uuid);
        Long until = active == null ? null : active.get(kind);
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        return remaining <= 0 ? 0L : (remaining + 999L) / 1000L;
    }

    private void startCooldown(UUID uuid, TeleportKind kind) {
        int seconds = cooldownSeconds.getOrDefault(kind, 0);
        if (seconds > 0) {
            cooldowns.computeIfAbsent(uuid, key -> new ConcurrentHashMap<>())
                    .put(kind, System.currentTimeMillis() + seconds * 1000L);
        }
    }

    @Override
    public void shutdown() {
        for (Warmup warmup : warmups.values()) {
            warmup.task().cancel();
        }
        warmups.clear();
    }
}
