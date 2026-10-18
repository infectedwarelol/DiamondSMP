package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class ChatFilter implements Service {

    public static final String BYPASS_PERMISSION = "diamondsmp.chatfilter.bypass";
    public static final String NOTIFY_PERMISSION = "diamondsmp.chatfilter.notify";
    private static final int JOINED_MATCH_MIN_LENGTH = 5;

    private final DiamondSMP plugin;
    private volatile boolean enabled = true;
    private volatile List<String> words = List.of();
    private volatile List<String> commands = List.of();

    public ChatFilter(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        enabled = config.getBoolean("chat-filter.enabled", true);
        List<String> parsed = new ArrayList<>();
        for (String word : config.getStringList("chat-filter.words")) {
            String normalized = normalize(word);
            if (!normalized.isEmpty()) {
                parsed.add(normalized);
            }
        }
        words = List.copyOf(parsed);
        List<String> parsedCommands = new ArrayList<>();
        for (String command : config.getStringList("chat-filter.commands")) {
            String trimmed = command.startsWith("/") ? command.substring(1) : command;
            if (!trimmed.isBlank()) {
                parsedCommands.add(trimmed);
            }
        }
        commands = List.copyOf(parsedCommands);
    }

    public boolean blocks(Player player, String message) {
        if (!enabled || words.isEmpty() || player.hasPermission(BYPASS_PERMISSION)) {
            return false;
        }
        String matched = match(message);
        if (matched == null) {
            return false;
        }
        punish(player, matched);
        return true;
    }

    private String match(String message) {
        String normalized = normalize(message);
        String joined = normalized.replace(" ", "");
        for (String token : normalized.split(" ")) {
            for (String word : words) {
                if (token.equals(word)) {
                    return word;
                }
            }
        }
        for (String word : words) {
            if (word.length() >= JOINED_MATCH_MIN_LENGTH && joined.contains(word)) {
                return word;
            }
        }
        return null;
    }

    private void punish(Player player, String word) {
        String name = player.getName();
        plugin.messages().error(player, "chat-filter.blocked");
        plugin.messages().notify(NOTIFY_PERMISSION, "chat-filter.notify", "player", name, "word", word);
        List<String> toRun = new ArrayList<>(commands.size());
        for (String command : commands) {
            toRun.add(command.replace("%player%", name).replace("%uuid%", player.getUniqueId().toString()));
        }
        plugin.scheduler().global(() -> {
            for (String command : toRun) {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } catch (RuntimeException ex) {
                    plugin.getLogger().log(Level.WARNING, "The chat filter command failed: " + command, ex);
                }
            }
        });
    }

    static String normalize(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lower.length());
        char previous = 0;
        for (int index = 0; index < lower.length(); index++) {
            char character = switch (lower.charAt(index)) {
                case '0' -> 'o';
                case '1', '!', '|' -> 'i';
                case '3' -> 'e';
                case '4', '@' -> 'a';
                case '5', '$' -> 's';
                case '7' -> 't';
                default -> lower.charAt(index);
            };
            if (character < 'a' || character > 'z') {
                character = ' ';
            }
            if (character == previous) {
                continue;
            }
            builder.append(character);
            previous = character;
        }
        return builder.toString().trim();
    }
}
