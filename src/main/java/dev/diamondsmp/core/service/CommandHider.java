package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.util.SoundSpec;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CommandHider implements Service {

    public static final String BYPASS_PERMISSION = "diamondsmp.commandhider.bypass";
    private static final String GROUP_PERMISSION = "diamondsmp.commandhider.group.";

    private final DiamondSMP plugin;
    private volatile boolean enabled = true;
    private volatile boolean hideNamespaced = true;
    private volatile Set<String> hidden = Set.of();
    private volatile boolean whitelist;
    private volatile Set<String> visible = Set.of();
    private volatile Map<String, Set<String>> groups = Map.of();
    private volatile SoundSpec sound = SoundSpec.NONE;

    public CommandHider(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        enabled = config.getBoolean("command-hider.enabled", true);
        hideNamespaced = config.getBoolean("command-hider.hide-namespaced", true);
        Set<String> labels = new HashSet<>();
        for (String label : config.getStringList("command-hider.hidden-commands")) {
            labels.add(normalize(label));
        }
        hidden = Set.copyOf(labels);
        whitelist = "whitelist".equalsIgnoreCase(config.getString("command-hider.mode", "permission"));
        visible = labels(config.getStringList("command-hider.visible-commands"));
        Map<String, Set<String>> parsedGroups = new HashMap<>();
        ConfigurationSection groupSection = config.getConfigurationSection("command-hider.groups");
        if (groupSection != null) {
            for (String group : groupSection.getKeys(false)) {
                parsedGroups.put(group.toLowerCase(Locale.ROOT), labels(groupSection.getStringList(group)));
            }
        }
        groups = Map.copyOf(parsedGroups);
        sound = SoundSpec.from(config.getConfigurationSection("command-hider.sound"));
        if (enabled) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                plugin.scheduler().entity(online, online::updateCommands);
            }
        }
    }

    public void filter(Player player, Collection<String> labels) {
        if (!enabled) {
            return;
        }
        labels.removeIf(label -> isHidden(player, label));
    }

    public boolean blocks(Player player, String label) {
        return enabled && isHidden(player, label);
    }

    public void deny(Player player) {
        plugin.messages().send(player, "general.unknown-command");
        sound.play(player);
    }

    public Component unknown(CommandSender sender) {
        if (!enabled) {
            return null;
        }
        sound.play(sender);
        return plugin.messages().get("general.unknown-command");
    }

    private boolean isHidden(Player player, String rawLabel) {
        String label = normalize(rawLabel);
        int namespace = label.indexOf(':');
        String base = namespace < 0 ? label : label.substring(namespace + 1);
        Command command = Bukkit.getCommandMap().getCommand(label);
        if (!player.hasPermission(BYPASS_PERMISSION)) {
            if (hidden.contains(base) || hideNamespaced && namespace >= 0) {
                return true;
            }
            if (whitelist && !allowed(player, base, command)) {
                return true;
            }
        }
        return command != null && !command.testPermissionSilent(player);
    }

    private boolean allowed(Player player, String base, Command command) {
        String name = command == null ? base : command.getName().toLowerCase(Locale.ROOT);
        if (visible.contains(base) || visible.contains(name)) {
            return true;
        }
        for (Map.Entry<String, Set<String>> group : groups.entrySet()) {
            Set<String> commands = group.getValue();
            if ((commands.contains(base) || commands.contains(name))
                    && player.hasPermission(GROUP_PERMISSION + group.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> labels(List<String> raw) {
        Set<String> result = new HashSet<>();
        for (String label : raw) {
            String normalized = normalize(label);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return Set.copyOf(result);
    }

    private static String normalize(String label) {
        String value = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
