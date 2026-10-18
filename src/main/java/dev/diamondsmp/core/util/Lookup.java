package dev.diamondsmp.core.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Lookup {

    private static final Enchantment GLOW = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));

    private Lookup() {
    }

    public static Material material(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material found = Material.matchMaterial(name.trim());
        return found == null || found.isLegacy() ? fallback : found;
    }

    public static Set<Material> materials(Collection<String> patterns) {
        Set<Material> result = EnumSet.noneOf(Material.class);
        List<String> wildcards = new ArrayList<>();
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String normalized = pattern.trim().toUpperCase(Locale.ROOT);
            if (normalized.indexOf('*') >= 0) {
                wildcards.add(normalized);
                continue;
            }
            Material material = material(normalized, null);
            if (material != null) {
                result.add(material);
            }
        }
        if (wildcards.isEmpty()) {
            return result;
        }
        for (Material material : Material.values()) {
            if (material.isLegacy()) {
                continue;
            }
            for (String wildcard : wildcards) {
                if (matches(wildcard, material.name())) {
                    result.add(material);
                    break;
                }
            }
        }
        return result;
    }

    public static Enchantment glowEnchantment() {
        return GLOW;
    }

    public static void sound(Player player, String key, float volume, float pitch) {
        if (key == null || key.isBlank()) {
            return;
        }
        player.playSound(player.getLocation(), key, SoundCategory.MASTER, volume, pitch);
    }

    private static boolean matches(String wildcard, String name) {
        String[] parts = wildcard.split("\\*", -1);
        int position = 0;
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty()) {
                continue;
            }
            int found = name.indexOf(part, position);
            if (found < 0 || (index == 0 && found != 0)) {
                return false;
            }
            position = found + part.length();
        }
        String last = parts[parts.length - 1];
        return last.isEmpty() || name.endsWith(last);
    }
}
