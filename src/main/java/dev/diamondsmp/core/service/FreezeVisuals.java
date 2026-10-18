package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.effect.Particles;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.util.Lookup;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;

public final class FreezeVisuals {

    private static final double FULL_TURN = Math.PI * 2.0D;
    private static final double PHASE_STEP = 0.35D;

    private final DiamondSMP plugin;
    private Task particleTask = Task.NONE;
    private volatile double phase;

    private volatile boolean iceHead = true;
    private volatile Material iceMaterial = Material.ICE;
    private volatile float iceScale = 0.62F;
    private volatile float iceOffset = -0.2F;
    private volatile boolean particles = true;
    private volatile Particle particle = Particles.DUST;
    private volatile Color color = Color.fromRGB(0x8BD7FF);
    private volatile float size = 0.8F;
    private volatile int amount = 2;
    private volatile double radius = 0.45D;
    private volatile double height = 1.8D;
    private volatile long interval = 10L;

    FreezeVisuals(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    void reload() {
        FileConfiguration config = plugin.settings();
        String path = "screenshare.visuals.";
        iceHead = config.getBoolean(path + "ice-head.enabled", true);
        Material material = Lookup.material(config.getString(path + "ice-head.material"), Material.ICE);
        iceMaterial = material.isItem() ? material : Material.ICE;
        iceScale = (float) clamp(config.getDouble(path + "ice-head.scale", 0.62D), 0.1D, 2.0D);
        iceOffset = (float) clamp(config.getDouble(path + "ice-head.offset", -0.2D), -2.0D, 2.0D);
        particles = config.getBoolean(path + "particles.enabled", true);
        particle = Particles.byName(config.getString(path + "particles.type"), Particles.DUST);
        color = parseColor(config.getString(path + "particles.color", "#8BD7FF"));
        size = (float) clamp(config.getDouble(path + "particles.size", 0.8D), 0.1D, 4.0D);
        amount = (int) clamp(config.getInt(path + "particles.amount", 2), 1, 16);
        radius = clamp(config.getDouble(path + "particles.radius", 0.45D), 0.0D, 3.0D);
        height = clamp(config.getDouble(path + "particles.height", 1.8D), 0.0D, 4.0D);
        interval = (long) clamp(config.getInt(path + "particles.interval", 10), 1, 100);
        stop();
    }

    void attach(ScreenShareSession session, Player target) {
        if (!iceHead) {
            detach(session);
            return;
        }
        ItemDisplay current = session.iceHead();
        if (current != null && current.isValid() && target.equals(current.getVehicle())) {
            return;
        }
        detach(session);
        ItemDisplay display = target.getWorld().spawn(target.getLocation(), ItemDisplay.class);
        display.setPersistent(false);
        display.setItemStack(new ItemStack(iceMaterial));
        display.setTransformation(new Transformation(new Vector3f(0.0F, iceOffset, 0.0F), new AxisAngle4f(),
                new Vector3f(iceScale), new AxisAngle4f()));
        if (target.addPassenger(display)) {
            session.iceHead(display);
        } else {
            display.remove();
        }
    }

    void detach(ScreenShareSession session) {
        ItemDisplay display = session.iceHead();
        session.iceHead(null);
        if (display != null) {
            plugin.scheduler().entity(display, display::remove);
        }
    }

    void start() {
        if (particles && particleTask == Task.NONE) {
            particleTask = plugin.scheduler().globalTimer(this::pulse, interval, interval);
        }
    }

    void stop() {
        particleTask.cancel();
        particleTask = Task.NONE;
    }

    private void pulse() {
        Collection<ScreenShareSession> sessions = plugin.screenShare().sessions();
        if (sessions.isEmpty()) {
            return;
        }
        double current = (phase + PHASE_STEP) % (FULL_TURN * 2.0D);
        phase = current;
        for (ScreenShareSession session : sessions) {
            Player target = Bukkit.getPlayer(session.targetId());
            if (target != null) {
                plugin.scheduler().entity(target, () -> spawn(target, current));
            }
        }
    }

    private void spawn(Player target, double current) {
        if (!target.isOnline()) {
            return;
        }
        Location position = target.getLocation();
        double spacing = FULL_TURN / amount;
        for (int index = 0; index < amount; index++) {
            double angle = current + spacing * index;
            double offset = height * (0.5D + 0.5D * Math.sin(current / 2.0D + index));
            Location point = new Location(position.getWorld(), position.getX() + Math.cos(angle) * radius,
                    position.getY() + offset, position.getZ() + Math.sin(angle) * radius);
            Particles.spawn(position.getWorld(), particle, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, color, size);
        }
    }

    private static Color parseColor(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.matches("[0-9a-fA-F]{6}")) {
            return Color.fromRGB(Integer.parseInt(value, 16));
        }
        return Color.fromRGB(0x8BD7FF);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
