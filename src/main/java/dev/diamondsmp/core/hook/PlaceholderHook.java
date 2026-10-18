package dev.diamondsmp.core.hook;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PlaceholderHook {

    private final DiamondSMP plugin;
    private final List<DiamondExpansion> expansions = new ArrayList<>();

    public PlaceholderHook(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    public static boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void register() {
        Set<String> identifiers = new LinkedHashSet<>();
        identifiers.add("diamondsmp");
        for (String identifier : plugin.settings().getStringList("placeholders.extra-identifiers")) {
            if (identifier != null && !identifier.isBlank()) {
                identifiers.add(identifier.trim().toLowerCase(Locale.ROOT));
            }
        }
        for (String identifier : identifiers) {
            DiamondExpansion expansion = new DiamondExpansion(plugin, identifier);
            if (expansion.register()) {
                expansions.add(expansion);
            } else {
                plugin.getLogger().warning("The placeholder identifier '" + identifier + "' is already taken.");
            }
        }
        plugin.getLogger().info("Hooked into PlaceholderAPI (" + String.join(", ", identifiers) + ").");
    }

    public void unregister() {
        for (DiamondExpansion expansion : expansions) {
            expansion.unregister();
        }
        expansions.clear();
    }
}
