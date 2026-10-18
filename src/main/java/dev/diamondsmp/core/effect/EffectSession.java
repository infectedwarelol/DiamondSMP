package dev.diamondsmp.core.effect;

import dev.diamondsmp.core.scheduler.Scheduler;
import dev.diamondsmp.core.scheduler.Task;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import java.time.Duration;
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
    private final String victimName;
    private final List<Player> viewers;
    private final List<Player> hidden;
    private final Settings settings;
    private final Runnable onFinish;
    private final List<Step> steps = new ArrayList<>();
    private List<Step> timeline = List.of();
    private final List<Display> displays = new ArrayList<>();
    private final Set<Location> illusions = new LinkedHashSet<>();
    private final AtomicBoolean finished = new AtomicBoolean();
    private Task task = Task.NONE;
    private int tick;
    private int lastTick;
    private int particlesThisTick;

    public EffectSession(Plugin plugin, Scheduler scheduler, Location origin, Player killer, String victimName,
                         List<Player> viewers, List<Player> hidden, Settings settings, Runnable onFinish) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.origin = new Location(origin.getWorld(), origin.getX(), origin.getY(), origin.getZ(), 0.0F, 0.0F);
        this.world = origin.getWorld();
        this.killer = killer;
        this.victimName = victimName;
        this.viewers = List.copyOf(viewers);
        this.hidden = List.copyOf(hidden);
        this.settings = settings;
        this.onFinish = onFinish;
    }

    public World world() {
        return world;
    }

    public String victimName() {
        return victimName;
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
        for (Display display : displays) {
            if (display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
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

    @SuppressWarnings("deprecation")
    public TextDisplay text(Component text, Transformation start) {
        TextDisplay display = spawn(TextDisplay.class, start);
        if (display != null) {
            display.text(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setShadowed(true);
            display.setSeeThrough(false);
        }
        return display;
    }

    @SuppressWarnings("UnstableApiUsage")
    private <T extends Display> T spawn(Class<T> type, Transformation start) {
        if (finished.get() || !Bukkit.isOwnedByCurrentRegion(origin)) {
            return null;
        }
        T display = world.spawn(origin, type);
        display.setPersistent(false);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setTransformation(start);
        displays.add(display);
        for (Player player : hidden) {
            scheduler.entity(player, () -> player.hideEntity(plugin, display));
        }
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

    public void remove(Display display) {
        if (display != null && display.isValid()) {
            display.remove();
            displays.remove(display);
        }
    }

    public void floor(int dx, int dz, Material material) {
        Location column = origin.clone().add(dx, 0.0D, dz);
        if (!Bukkit.isOwnedByCurrentRegion(column)) {
            return;
        }
        Block feet = column.getBlock();
        for (int offset = 0; offset < 3; offset++) {
            Block ground = feet.getRelative(0, -1 - offset, 0);
            Block above = ground.getRelative(0, 1, 0);
            if (ground.getType().isSolid() && above.isPassable()) {
                illusion(ground.getLocation(), material);
                return;
            }
        }
    }

    public void air(int dx, int dy, int dz, Material material) {
        Location location = origin.clone().add(dx, dy, dz);
        if (Bukkit.isOwnedByCurrentRegion(location) && location.getBlock().isEmpty()) {
            illusion(location.getBlock().getLocation(), material);
        }
    }

    private void illusion(Location location, Material material) {
        if (finished.get()) {
            return;
        }
        BlockData data = material.createBlockData();
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

    public void flair(Component title, Component subtitle) {
        Player target = killer;
        if (target != null && target.isOnline()) {
            target.showTitle(Title.title(title, subtitle, Title.Times.times(Duration.ZERO,
                    Duration.ofMillis(1200), Duration.ofMillis(400))));
        }
    }
}
