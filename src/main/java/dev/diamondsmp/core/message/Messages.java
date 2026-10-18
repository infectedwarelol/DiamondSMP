package dev.diamondsmp.core.message;

import dev.diamondsmp.core.config.ConfigFile;
import dev.diamondsmp.core.util.SoundSpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Messages {

    private final ConfigFile file;
    private volatile String prefix = "";
    private volatile String primary = "";
    private volatile String accent = "";
    private volatile Map<String, String> tokens = Map.of();
    private volatile SoundSpec errorSound = SoundSpec.NONE;

    public Messages(Plugin plugin) {
        this.file = new ConfigFile(plugin, "messages.yml", Set.of());
    }

    public void reload() {
        file.load();
        prefix = file.get().getString("prefix", "");
        primary = file.get().getString("theme.primary", "&b");
        accent = file.get().getString("theme.accent", "&f");
        errorSound = SoundSpec.from(file.get().getConfigurationSection("error-sound"));
    }

    public void tokens(Map<String, String> values) {
        Map<String, String> wrapped = new HashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            wrapped.put("%" + entry.getKey() + "%", entry.getValue() == null ? "" : entry.getValue());
        }
        tokens = Map.copyOf(wrapped);
    }

    public String raw(String key) {
        return file.get().getString(key);
    }

    public List<String> rawList(String key) {
        return file.get().getStringList(key);
    }

    public Component get(String key, Object... placeholders) {
        return format(raw(key), placeholders);
    }

    public Component format(String template, Object... placeholders) {
        if (Text.isBlank(template)) {
            return null;
        }
        String themed = theme(template);
        return Text.replace(Text.parse(themed), themed, placeholders);
    }

    public Component display(String key, Object... placeholders) {
        Component component = get(key, placeholders);
        return component == null ? Component.empty() : Text.display("").append(component);
    }

    public List<Component> lore(String key, Object... placeholders) {
        List<String> lines = rawList(key);
        List<Component> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            String themed = theme(line);
            result.add(Text.replace(Text.display(themed), themed, placeholders));
        }
        return result;
    }

    public List<Component> lines(String key, Object... placeholders) {
        if (!file.get().isList(key)) {
            Component single = get(key, placeholders);
            return single == null ? List.of() : List.of(single);
        }
        List<String> raw = rawList(key);
        List<Component> result = new ArrayList<>(raw.size());
        for (String line : raw) {
            result.add(line.isEmpty() ? Component.empty() : format(line, placeholders));
        }
        return result;
    }

    public void send(CommandSender target, String key, Object... placeholders) {
        if (target == null) {
            return;
        }
        for (Component line : lines(key, placeholders)) {
            target.sendMessage(line);
        }
    }

    public void error(CommandSender target, String key, Object... placeholders) {
        send(target, key, placeholders);
        errorSound.play(target);
    }

    public void outcome(CommandSender target, boolean success, String successKey, String failureKey,
                        Object... placeholders) {
        if (success) {
            send(target, successKey, placeholders);
        } else {
            error(target, failureKey, placeholders);
        }
    }

    public void errorSound(CommandSender target) {
        errorSound.play(target);
    }

    public Title title(String key, Title.Times times, Object... placeholders) {
        Component title = get(key + ".title", placeholders);
        Component subtitle = get(key + ".subtitle", placeholders);
        if (title == null && subtitle == null) {
            return null;
        }
        return Title.title(title == null ? Component.empty() : title,
                subtitle == null ? Component.empty() : subtitle, times);
    }

    public void title(Player target, String key, Title.Times times, Object... placeholders) {
        Title title = title(key, times, placeholders);
        if (target != null && title != null) {
            target.showTitle(title);
        }
    }

    public void actionBar(Player target, String key, Object... placeholders) {
        Component component = get(key, placeholders);
        if (component != null && target != null) {
            target.sendActionBar(component);
        }
    }

    public void broadcast(String key, Object... placeholders) {
        for (Component line : lines(key, placeholders)) {
            broadcast(line);
        }
    }

    public void notify(String permission, String key, Object... placeholders) {
        List<Component> lines = lines(key, placeholders);
        if (lines.isEmpty()) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                for (Component line : lines) {
                    online.sendMessage(line);
                }
            }
        }
        for (Component line : lines) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    public void broadcast(Component component) {
        if (component == null) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    public String theme(String raw) {
        if (raw == null || raw.indexOf('%') < 0) {
            return raw;
        }
        String themed = raw.replace("%prefix%", prefix)
                .replace("%primary%", primary)
                .replace("%accent%", accent);
        for (Map.Entry<String, String> token : tokens.entrySet()) {
            if (themed.contains(token.getKey())) {
                themed = themed.replace(token.getKey(), token.getValue());
            }
        }
        return themed;
    }
}
