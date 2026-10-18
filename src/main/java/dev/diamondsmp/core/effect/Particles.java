package dev.diamondsmp.core.effect;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Particles {

    private static final Map<String, Particle> BY_NAME = index();

    public static final Particle DUST = find("DUST", "REDSTONE");
    public static final Particle FLAME = find("FLAME");
    public static final Particle SOUL = find("SOUL");
    public static final Particle SOUL_FLAME = find("SOUL_FIRE_FLAME");
    public static final Particle TOTEM = find("TOTEM_OF_UNDYING", "TOTEM");
    public static final Particle HEART = find("HEART");
    public static final Particle POOF = find("POOF", "EXPLOSION_NORMAL");
    public static final Particle EXPLOSION = find("EXPLOSION", "EXPLOSION_LARGE");
    public static final Particle LARGE_SMOKE = find("LARGE_SMOKE", "SMOKE_LARGE");
    public static final Particle ENCHANT = find("ENCHANT", "ENCHANTMENT_TABLE");
    public static final Particle PORTAL = find("PORTAL");
    public static final Particle REVERSE_PORTAL = find("REVERSE_PORTAL");
    public static final Particle SPLASH = find("SPLASH", "WATER_SPLASH");
    public static final Particle FALLING_WATER = find("FALLING_WATER", "DRIP_WATER");
    public static final Particle NOTE = find("NOTE");
    public static final Particle CLOUD = find("CLOUD");
    public static final Particle HAPPY_VILLAGER = find("HAPPY_VILLAGER", "VILLAGER_HAPPY");
    public static final Particle WITCH = find("WITCH", "SPELL_WITCH");
    public static final Particle CRIT = find("CRIT");
    public static final Particle ENCHANTED_HIT = find("ENCHANTED_HIT", "CRIT_MAGIC");
    public static final Particle END_ROD = find("END_ROD");

    private Particles() {
    }

    private static Map<String, Particle> index() {
        Map<String, Particle> index = new HashMap<>();
        for (Particle particle : Particle.values()) {
            index.put(particle.name().toUpperCase(Locale.ROOT), particle);
        }
        return index;
    }

    public static Particle byName(String name, Particle fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        int namespace = normalized.indexOf(':');
        Particle found = BY_NAME.get(namespace < 0 ? normalized : normalized.substring(namespace + 1));
        if (found == null && (normalized.equals("DUST") || normalized.equals("REDSTONE"))) {
            found = DUST;
        }
        return found == null ? fallback : found;
    }

    private static Particle find(String... names) {
        for (String name : names) {
            Particle particle = BY_NAME.get(name);
            if (particle != null) {
                return particle;
            }
        }
        return null;
    }

    public static void spawn(World world, Particle particle, Location at, int count, double spread, double speed) {
        spawn(world, particle, at, count, spread, spread, spread, speed, Color.RED);
    }

    public static void spawn(World world, Particle particle, Location at, int count,
                             double offsetX, double offsetY, double offsetZ, double speed, Color color) {
        spawn(world, particle, at, count, offsetX, offsetY, offsetZ, speed, color, 1.4F);
    }

    public static void spawn(World world, Particle particle, Location at, int count, double offsetX,
                             double offsetY, double offsetZ, double speed, Color color, float size) {
        if (particle == null || world == null) {
            return;
        }
        Class<?> data = particle.getDataType();
        if (data == Void.class) {
            world.spawnParticle(particle, at, count, offsetX, offsetY, offsetZ, speed);
        } else if (data == Particle.DustOptions.class) {
            world.spawnParticle(particle, at, count, offsetX, offsetY, offsetZ, speed,
                    new Particle.DustOptions(color, size));
        } else if (data == Color.class) {
            world.spawnParticle(particle, at, count, offsetX, offsetY, offsetZ, speed, color);
        } else if (data == Float.class) {
            world.spawnParticle(particle, at, count, offsetX, offsetY, offsetZ, speed, 1.0F);
        } else if (data == Integer.class) {
            world.spawnParticle(particle, at, count, offsetX, offsetY, offsetZ, speed, 0);
        }
    }
}
