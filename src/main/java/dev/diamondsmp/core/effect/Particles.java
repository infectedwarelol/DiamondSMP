package dev.diamondsmp.core.effect;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Particles {

    public static final Object UNSUPPORTED = new Object();

    private static final Map<String, Particle> BY_NAME = index();

    public static final Particle DUST = find("DUST", "REDSTONE");
    public static final Particle FLAME = find("FLAME");
    public static final Particle SOUL = find("SOUL");
    public static final Particle SOUL_FLAME = find("SOUL_FIRE_FLAME");
    public static final Particle SCULK_SOUL = find("SCULK_SOUL");
    public static final Particle TOTEM = find("TOTEM_OF_UNDYING", "TOTEM");
    public static final Particle EXPLOSION = find("EXPLOSION", "EXPLOSION_LARGE");
    public static final Particle EXPLOSION_EMITTER = find("EXPLOSION_EMITTER", "EXPLOSION_HUGE");
    public static final Particle SMOKE = find("SMOKE", "SMOKE_NORMAL");
    public static final Particle LARGE_SMOKE = find("LARGE_SMOKE", "SMOKE_LARGE");
    public static final Particle SIGNAL_SMOKE = find("CAMPFIRE_SIGNAL_SMOKE");
    public static final Particle ENCHANT = find("ENCHANT", "ENCHANTMENT_TABLE");
    public static final Particle REVERSE_PORTAL = find("REVERSE_PORTAL");
    public static final Particle CLOUD = find("CLOUD");
    public static final Particle CRIT = find("CRIT");
    public static final Particle ENCHANTED_HIT = find("ENCHANTED_HIT", "CRIT_MAGIC");
    public static final Particle END_ROD = find("END_ROD");
    public static final Particle FIREWORK = find("FIREWORK", "FIREWORKS_SPARK");
    public static final Particle FLASH = find("FLASH");
    public static final Particle ELECTRIC_SPARK = find("ELECTRIC_SPARK");
    public static final Particle SQUID_INK = find("SQUID_INK");
    public static final Particle SONIC_BOOM = find("SONIC_BOOM");
    public static final Particle SNOWFLAKE = find("SNOWFLAKE");
    public static final Particle WHITE_ASH = find("WHITE_ASH");
    public static final Particle ASH = find("ASH");
    public static final Particle LAVA = find("LAVA");
    public static final Particle LANDING_LAVA = find("LANDING_LAVA");
    public static final Particle DRAGON_BREATH = find("DRAGON_BREATH");
    public static final Particle CRIMSON_SPORE = find("CRIMSON_SPORE");
    public static final Particle DAMAGE = find("DAMAGE_INDICATOR");

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

    public static Object data(Particle particle, Color color, float size) {
        Class<?> type = particle.getDataType();
        Color safe = color == null ? Color.WHITE : color;
        if (type == Void.class) {
            return null;
        }
        if (type == Particle.DustOptions.class) {
            return new Particle.DustOptions(safe, size);
        }
        if (type == Particle.DustTransition.class) {
            return new Particle.DustTransition(safe, Color.WHITE, size);
        }
        if (type == Color.class) {
            return safe;
        }
        if (type == Float.class) {
            return 1.0F;
        }
        if (type == Integer.class) {
            return 0;
        }
        return UNSUPPORTED;
    }

    public static void spawn(World world, Particle particle, Location at, int count, double offsetX,
                             double offsetY, double offsetZ, double speed, Color color, float size) {
        if (particle == null || world == null) {
            return;
        }
        Object data = data(particle, color, size);
        if (data == UNSUPPORTED) {
            return;
        }
        if (data == null) {
            world.spawnParticle(particle, at, count, offsetX, offsetY, offsetZ, speed);
        } else {
            world.spawnParticle(particle, at, count, offsetX, offsetY, offsetZ, speed, data);
        }
    }
}
