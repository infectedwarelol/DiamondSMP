package dev.diamondsmp.core.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public record SoundSpec(boolean enabled, String key, float volume, float pitch) {

    public static final SoundSpec NONE = new SoundSpec(false, "", 1.0F, 1.0F);

    public static SoundSpec from(ConfigurationSection section) {
        if (section == null) {
            return NONE;
        }
        return new SoundSpec(section.getBoolean("enabled", true), section.getString("sound", ""),
                (float) section.getDouble("volume", 1.0D), (float) section.getDouble("pitch", 1.0D));
    }

    public void play(Object target) {
        if (enabled && target instanceof Player player) {
            Lookup.sound(player, key, volume, pitch);
        }
    }
}
