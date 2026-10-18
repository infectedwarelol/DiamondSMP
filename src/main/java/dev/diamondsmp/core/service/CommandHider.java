package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.util.SoundSpec;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CommandHider implements Service {

    public static final String BYPASS_PERMISSION = "diamondsmp.commandhider.bypass";

    private final DiamondSMP plugin;
    private volatile boolean enabled = true;
    private volatile boolean hideNamespaced = true;
    private volatile Set<String> hidden = Set.of();
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
        if (!player.hasPermission(BYPASS_PERMISSION)
                && (hidden.contains(base) || hideNamespaced && namespace >= 0)) {
            return true;
        }
        Command command = Bukkit.getCommandMap().getCommand(label);
        return command != null && !command.testPermissionSilent(player);
    }

    private static String normalize(String label) {
        String value = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
