package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.effect.Hologram;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.KillEffectRegistry;
import dev.diamondsmp.core.gui.KillEffectMenu;
import dev.diamondsmp.core.storage.PlayerProfile;
import dev.diamondsmp.core.util.Format;
import dev.diamondsmp.core.util.Lookup;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KillEffectService implements Service {

    public static final String NONE = "none";
    public static final String PERMISSION_PREFIX = "diamondsmp.killeffect.";

    public record Configured(KillEffect effect, String name, Material icon, List<String> description) {

        public String id() {
            return effect.id();
        }
    }

    private final DiamondSMP plugin;
    private final KillEffectRegistry registry;
    private volatile Map<String, Configured> enabled = Map.of();
    private volatile Material lockedIcon = Material.GRAY_DYE;
    private volatile Material noneIcon = Material.BARRIER;
    private volatile Hologram hologram = new Hologram(true, 2.5F, 50, 1.6D, 1.2D, 0.35D);

    public KillEffectService(DiamondSMP plugin) {
        this.plugin = plugin;
        this.registry = new KillEffectRegistry(new NamespacedKey(plugin, "kill_effect"));
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        lockedIcon = Lookup.material(config.getString("kill-effects.locked-icon"), Material.GRAY_DYE);
        noneIcon = Lookup.material(config.getString("kill-effects.none-icon"), Material.BARRIER);
        hologram = new Hologram(config.getBoolean("kill-effects.hologram.enabled", true),
                (float) Math.max(0.1D, Math.min(8.0D, config.getDouble("kill-effects.hologram.scale", 2.5D))),
                (int) Math.max(10L, Format.parseSeconds(config.getString("kill-effects.hologram.duration"), 3L) * 20L),
                config.getDouble("kill-effects.hologram.height", 1.6D),
                config.getDouble("kill-effects.hologram.rise", 1.2D),
                config.getDouble("kill-effects.hologram.spin", 0.35D));
        Map<String, Configured> parsed = new LinkedHashMap<>();
        for (KillEffect effect : registry.all()) {
            ConfigurationSection section = config.getConfigurationSection("kill-effects.effects." + effect.id());
            if (section != null && !section.getBoolean("enabled", true)) {
                continue;
            }
            String name = section == null ? effect.name() : section.getString("name", effect.name());
            Material icon = section == null ? effect.icon() : Lookup.material(section.getString("icon"), effect.icon());
            List<String> description = section == null || !section.isList("description")
                    ? effect.description() : section.getStringList("description");
            parsed.put(effect.id(), new Configured(effect, name, icon, List.copyOf(description)));
        }
        enabled = parsed;
    }

    public NamespacedKey fireworkKey() {
        return registry.fireworkKey();
    }

    public List<Configured> available() {
        return new ArrayList<>(enabled.values());
    }

    public Configured find(String id) {
        return id == null ? null : enabled.get(id.toLowerCase(Locale.ROOT));
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>(enabled.size() + 1);
        ids.add(NONE);
        ids.addAll(enabled.keySet());
        return ids;
    }

    public Material lockedIcon() {
        return lockedIcon;
    }

    public Material noneIcon() {
        return noneIcon;
    }

    public boolean isLocked(Player player, String id) {
        return !NONE.equals(id) && !player.hasPermission(PERMISSION_PREFIX + id);
    }

    public String selected(Player player) {
        PlayerProfile profile = plugin.profiles().profile(player);
        String effect = profile == null ? null : profile.killEffect();
        return effect == null || find(effect) == null ? NONE : effect;
    }

    public boolean select(Player player, String id) {
        PlayerProfile profile = plugin.profiles().profile(player);
        if (profile == null) {
            return false;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        if (!NONE.equals(normalized) && find(normalized) == null) {
            return false;
        }
        profile.killEffect(NONE.equals(normalized) ? null : normalized);
        return true;
    }

    public void play(Player killer, Location at) {
        String id = selected(killer);
        Configured configured = find(id);
        if (configured == null || isLocked(killer, id) || at.getWorld() == null) {
            return;
        }
        Location location = at.clone();
        Hologram style = hologram;
        plugin.scheduler().region(location, () -> {
            configured.effect().animation().play(plugin.scheduler(), location);
            style.show(plugin.scheduler(), location, configured.icon());
        });
    }

    public void open(Player player, int page) {
        plugin.scheduler().entity(player, () -> new KillEffectMenu(plugin, player, page).open(player));
    }
}
