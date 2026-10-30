package dev.diamondsmp.core.command.admin;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DiamondSMPCommand extends BaseCommand {

    private static final String ENABLED_KEY = "enabled";

    public DiamondSMPCommand(DiamondSMP plugin) {
        super(plugin, "diamondsmp", "diamondsmp.admin", false);
    }

    @Override
    protected void execute(CommandContext context) {
        String action = context.arg(0, "version").toLowerCase(Locale.ROOT);
        switch (action) {
            case "reload" -> {
                long started = System.nanoTime();
                plugin.reloadAll(() -> context.reply("admin.reloaded",
                        "time", (System.nanoTime() - started) / 1_000_000L));
            }
            case "version" -> context.reply("admin.version",
                    "version", plugin.version(),
                    "server", plugin.serverVersion().describe());
            case "enable" -> toggle(context, action, true);
            case "disable" -> toggle(context, action, false);
            default -> throw context.usage();
        }
    }

    private void toggle(CommandContext context, String action, boolean enable) {
        if (!context.has(1)) {
            throw context.fail("admin.feature-usage", "action", action);
        }
        String feature = context.arg(1).toLowerCase(Locale.ROOT);
        Map<String, Boolean> features = features();
        Boolean current = features.get(feature);
        if (current == null) {
            throw context.fail("admin.feature-unknown", "feature", feature);
        }
        if (current == enable) {
            throw context.fail(enable ? "admin.feature-already-enabled" : "admin.feature-already-disabled",
                    "feature", feature);
        }
        String path = feature + "." + ENABLED_KEY;
        plugin.scheduler().async(() -> {
            boolean saved = plugin.configFile().set(path, enable);
            plugin.scheduler().global(() -> {
                if (!saved) {
                    context.error("admin.feature-save-failed", "feature", feature);
                    return;
                }
                long started = System.nanoTime();
                plugin.reloadAll(() -> context.reply(enable ? "admin.feature-enabled" : "admin.feature-disabled",
                        "feature", feature, "time", (System.nanoTime() - started) / 1_000_000L));
            });
        });
    }

    private Map<String, Boolean> features() {
        ConfigurationSection settings = plugin.settings();
        List<String> toggles = new ArrayList<>();
        for (String key : settings.getKeys(true)) {
            if (!key.endsWith("." + ENABLED_KEY) || !settings.isBoolean(key)) {
                continue;
            }
            String section = key.substring(0, key.length() - ENABLED_KEY.length() - 1);
            if (toggles.stream().noneMatch(toggle -> section.startsWith(toggle + "."))) {
                toggles.add(section);
            }
        }
        Map<String, Integer> perRoot = new LinkedHashMap<>();
        for (String toggle : toggles) {
            perRoot.merge(root(toggle), 1, Integer::sum);
        }
        Map<String, Boolean> features = new LinkedHashMap<>();
        for (String toggle : toggles) {
            if (toggle.equals(root(toggle)) || perRoot.get(root(toggle)) == 1) {
                features.put(toggle, settings.getBoolean(toggle + "." + ENABLED_KEY));
            }
        }
        return features;
    }

    private static String root(String path) {
        int dot = path.indexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 2) {
            String action = context.arg(0).toLowerCase(Locale.ROOT);
            boolean enable = action.equals("enable");
            if (!enable && !action.equals("disable")) {
                return List.of();
            }
            List<String> options = new ArrayList<>();
            for (Map.Entry<String, Boolean> entry : features().entrySet()) {
                if (entry.getValue() != enable) {
                    options.add(entry.getKey());
                }
            }
            return matching(context, 1, options);
        }
        return matching(context, 0, List.of("reload", "version", "enable", "disable"));
    }
}
