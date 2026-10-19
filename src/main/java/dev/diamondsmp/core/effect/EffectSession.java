package dev.diamondsmp.core.effect;

import dev.diamondsmp.core.scheduler.Scheduler;
import dev.diamondsmp.core.scheduler.Task;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;
import java.util.logging.Level;

public final class EffectSession {

    public record Settings(double density, double durationScale, float volume, int particleBudget, int maxTicks) {
    }

    private record Step(int start, int period, int times, IntConsumer action) {
    }

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final Location origin;
    private final World world;
    private final Player killer;
    private final Corpse corpse;
    private final List<Player> viewers;
    private final List<Player> hidden;
    private final Settings settings;
    private final Runnable onFinish;
    private final List<Step> steps = new ArrayList<>();
    private List<Step> timeline = List.of();
    private final List<Entity> entities = new ArrayList<>();
    private final Set<Location> illusions = new LinkedHashSet<>();
    private final AtomicBoolean finished = new AtomicBoolean();
    private Task task = Task.NONE;
    private int tick;
    private int lastTick;
    private int particlesThisTick;

    public EffectSession(Plugin plugin, Scheduler scheduler, Location origin, Player killer, Corpse corpse,
                         List<Player> viewers, List<Player> hidden, Settings settings, Runnable onFinish) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.origin = new Location(origin.getWorld(), origin.getX(), origin.getY(), origin.getZ(), 0.0F, 0.0F);
        this.world = origin.getWorld();
        this.killer = killer;
        this.corpse = corpse;
        this.viewers = List.copyOf(viewers);
        this.hidden = List.copyOf(hidden);
        this.settings = settings;
        this.onFinish = onFinish;
    }

    public World world() {
        return world;
    }

    public int time(int baseTicks) {
        return Math.max(1, (int) Math.round(baseTicks * settings.durationScale()));
    }

    public int count(int base) {
        return Math.max(1, (int) Math.round(base * settings.density()));
    }

    public double random(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }

    public void at(int baseTick, Runnable action) {
        every(baseTick, 1, 1, index -> action.run());
    }

    public void every(int baseStart, int basePeriod, int times, IntConsumer action) {
        if (times <= 0) {
            return;
        }
        int start = baseStart <= 0 ? 0 : time(baseStart);
        int period = time(basePeriod);
        steps.add(new Step(start, period, times, action));
        lastTick = Math.max(lastTick, start + period * (times - 1) + 2);
    }

    public void start() {
        if (steps.isEmpty()) {
            finish();
            return;
        }
        lastTick = Math.min(lastTick, settings.maxTicks());
        timeline = List.copyOf(steps);
        task = scheduler.regionTimer(origin, this::tick, 1L, 1L);
        if (task == Task.NONE) {
            finish();
        }
    }

    private void tick() {
        if (finished.get()) {
            return;
        }
        if (Bukkit.getWorld(world.getUID()) == null) {
            finish();
            return;
        }
        particlesThisTick = 0;
        try {
            for (Step step : timeline) {
                int elapsed = tick - step.start();
                if (elapsed < 0 || elapsed % step.period() != 0) {
                    continue;
                }
                int iteration = elapsed / step.period();
                if (iteration < step.times()) {
                    step.action().accept(iteration);
                }
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "A kill effect failed and was stopped", ex);
            finish();
            return;
        }
        tick++;
        if (tick > lastTick) {
            finish();
        }
    }

    public void finish() {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        task.cancel();
        if (Bukkit.isOwnedByCurrentRegion(origin)) {
            cleanup();
        } else {
            scheduler.region(origin, this::cleanup);
        }
        onFinish.run();
    }

    private void cleanup() {
        for (Entity entity : entities) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
        entities.clear();
        for (Location location : illusions) {
            if (!Bukkit.isOwnedByCurrentRegion(location)) {
                continue;
            }
            BlockData real = location.getBlock().getBlockData();
            for (Player viewer : viewers) {
                if (viewer.isOnline() && viewer.getWorld().equals(world)) {
                    viewer.sendBlockChange(location, real);
                }
            }
        }
        illusions.clear();
    }

    public void particle(Particle particle, double dx, double dy, double dz) {
        particle(particle, dx, dy, dz, 1, 0.0D, 0.0D, 0.0D, 0.0D, null, 1.0F);
    }

    public void particle(Particle particle, double dx, double dy, double dz, int count, double spread, double speed) {
        particle(particle, dx, dy, dz, count, spread, spread, spread, speed, null, 1.0F);
    }

    public void dust(double dx, double dy, double dz, Color color, float size) {
        particle(Particles.DUST, dx, dy, dz, 1, 0.0D, 0.0D, 0.0D, 0.0D, color, size);
    }

    public void motion(Particle particle, double dx, double dy, double dz, double vx, double vy, double vz,
                       double speed) {
        particle(particle, dx, dy, dz, 0, vx, vy, vz, speed, null, 1.0F);
    }

    public void particle(Particle particle, double dx, double dy, double dz, int count, double offsetX,
                         double offsetY, double offsetZ, double speed, Color color, float size) {
        if (particle == null || finished.get()) {
            return;
        }
        int cost = Math.max(1, count);
        if (particlesThisTick + cost > settings.particleBudget()) {
            return;
        }
        Object data = Particles.data(particle, color, size);
        if (data == Particles.UNSUPPORTED) {
            return;
        }
        particlesThisTick += cost;
        double x = origin.getX() + dx;
        double y = origin.getY() + dy;
        double z = origin.getZ() + dz;
        for (Player viewer : viewers) {
            if (!viewer.isOnline() || !viewer.getWorld().equals(world)) {
                continue;
            }
            if (data == null) {
                viewer.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, speed);
            } else {
                viewer.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, speed, data);
            }
        }
    }

    public void ring(Particle particle, double y, double radius, int points, double phase, Color color, float size) {
        int total = count(points);
        for (int index = 0; index < total; index++) {
            double angle = phase + EffectMath.TAU * index / total;
            particle(particle, Math.cos(angle) * radius, y, Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D,
                    color, size);
        }
    }

    public void line(Particle particle, double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
                     double spacing, Color color, float size) {
        double length = Math.sqrt(Math.pow(toX - fromX, 2) + Math.pow(toY - fromY, 2) + Math.pow(toZ - fromZ, 2));
        int points = Math.max(1, (int) (length / Math.max(0.05D, spacing)));
        for (int index = 0; index <= points; index++) {
            double t = (double) index / points;
            particle(particle, EffectMath.lerp(fromX, toX, t), EffectMath.lerp(fromY, toY, t),
                    EffectMath.lerp(fromZ, toZ, t), 1, 0.0D, 0.0D, 0.0D, 0.0D, color, size);
        }
    }

    public void sound(String key, float volume, float pitch) {
        if (finished.get() || settings.volume() <= 0.0F) {
            return;
        }
        float scaled = volume * settings.volume();
        for (Player viewer : viewers) {
            if (viewer.isOnline() && viewer.getWorld().equals(world)) {
                viewer.playSound(origin, key, SoundCategory.PLAYERS, scaled, pitch);
            }
        }
    }

    public BlockDisplay block(Material material, Transformation start) {
        BlockDisplay display = spawn(BlockDisplay.class, start);
        if (display != null) {
            display.setBlock(material.createBlockData());
        }
        return display;
    }

    public ItemDisplay item(Material material, Transformation start) {
        ItemDisplay display = spawn(ItemDisplay.class, start);
        if (display != null) {
            display.setItemStack(new ItemStack(material));
        }
        return display;
    }

    public TextDisplay text(Component text, Transformation start) {
        return text(text, start, Display.Billboard.CENTER);
    }

    @SuppressWarnings("deprecation")
    public TextDisplay text(Component text, Transformation start, Display.Billboard billboard) {
        TextDisplay display = spawn(TextDisplay.class, start);
        if (display != null) {
            display.text(text);
            display.setBillboard(billboard);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setShadowed(true);
            display.setSeeThrough(false);
        }
        return display;
    }

    private <T extends Display> T spawn(Class<T> type, Transformation start) {
        if (finished.get() || !Bukkit.isOwnedByCurrentRegion(origin)) {
            return null;
        }
        T display = world.spawn(origin, type);
        display.setPersistent(false);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setTransformation(start);
        track(display);
        return display;
    }

    public void move(Display display, Transformation target, int baseTicks) {
        if (display == null || !display.isValid() || finished.get()) {
            return;
        }
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(time(baseTicks));
        display.setTransformation(target);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void track(Entity entity) {
        entities.add(entity);
        for (Player player : hidden) {
            scheduler.entity(player, () -> player.hideEntity(plugin, entity));
        }
    }

    public void remove(Entity entity) {
        if (entity != null && entity.isValid()) {
            entity.remove();
            entities.remove(entity);
        }
    }

    public String victimName() {
        return corpse == null ? "?" : corpse.name();
    }

    public ArmorStand corpse(CorpsePose pose, double dx, double dz, float yaw) {
        return corpse(pose, dx, pose.floorOffset(), dz, yaw);
    }

    public ArmorStand corpse(CorpsePose pose, double dx, double dy, double dz, float yaw) {
        Location spot = new Location(world, origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz, yaw, 0.0F);
        if (corpse == null || finished.get() || !Bukkit.isOwnedByCurrentRegion(spot)) {
            return null;
        }
        ArmorStand stand = world.spawn(spot, ArmorStand.class);
        stand.setPersistent(false);
        stand.setMarker(true);
        stand.setInvulnerable(true);
        stand.setGravity(false);
        stand.setSilent(true);
        stand.setBasePlate(false);
        stand.setArms(true);
        stand.setCollidable(false);
        stand.setCanTick(false);
        EntityEquipment equipment = stand.getEquipment();
        equipment.setHelmet(corpse.head().clone());
        equipment.setChestplate(corpse.chest().clone());
        equipment.setLeggings(corpse.legs().clone());
        equipment.setBoots(corpse.boots().clone());
        if (corpse.weapon() != null) {
            equipment.setItemInMainHand(corpse.weapon().clone());
        }
        pose.apply(stand);
        track(stand);
        return stand;
    }

    public void decapitate(ArmorStand stand) {
        if (stand != null && stand.isValid()) {
            stand.getEquipment().setHelmet(null);
        }
    }

    public void pose(ArmorStand stand, CorpsePose pose) {
        if (stand != null && stand.isValid() && !finished.get()) {
            pose.apply(stand);
        }
    }

    public void place(Entity entity, double dx, double dy, double dz, float yaw) {
        if (entity == null || !entity.isValid() || finished.get()) {
            return;
        }
        Location target = new Location(world, origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz, yaw, 0.0F);
        if (Bukkit.isOwnedByCurrentRegion(target)) {
            entity.teleportAsync(target);
        }
    }

    public ItemDisplay head(Transformation start) {
        ItemDisplay display = spawn(ItemDisplay.class, start);
        if (display != null && corpse != null) {
            display.setItemStack(corpse.head().clone());
        }
        return display;
    }

    public void floor(int dx, int dz, Material material) {
        floor(dx, dz, material.createBlockData());
    }

    public void floor(int dx, int dz, BlockData data) {
        Location column = origin.clone().add(dx, 0.0D, dz);
        if (!Bukkit.isOwnedByCurrentRegion(column)) {
            return;
        }
        Block feet = column.getBlock();
        for (int offset = 0; offset < 3; offset++) {
            Block ground = feet.getRelative(0, -1 - offset, 0);
            Block above = ground.getRelative(0, 1, 0);
            if (ground.getType().isSolid() && above.isPassable()) {
                illusion(ground.getLocation(), data);
                return;
            }
        }
    }

    public void air(int dx, int dy, int dz, Material material) {
        Location location = origin.clone().add(dx, dy, dz);
        if (Bukkit.isOwnedByCurrentRegion(location) && location.getBlock().isEmpty()) {
            illusion(location.getBlock().getLocation(), material.createBlockData());
        }
    }

    private void illusion(Location location, BlockData data) {
        if (finished.get()) {
            return;
        }
        illusions.add(location);
        for (Player viewer : viewers) {
            if (viewer.isOnline() && viewer.getWorld().equals(world)) {
                viewer.sendBlockChange(location, data);
            }
        }
    }

    public void lightning(double dx, double dz) {
        Location strike = origin.clone().add(dx, 0.0D, dz);
        if (!finished.get() && Bukkit.isOwnedByCurrentRegion(strike)) {
            world.strikeLightningEffect(strike);
        }
    }

    public void shake() {
        Player target = killer;
        if (target != null && target.isOnline()) {
            scheduler.entity(target, () -> target.sendHurtAnimation(0.0F));
        }
    }
}
