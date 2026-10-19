package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.storage.StoredLocation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class WarpService implements Service {

    private static final String KIND = "warp";
    private static final Pattern NAME = Pattern.compile("[a-zA-Z0-9_-]{1,32}");

    private final DiamondSMP plugin;
    private final Map<String, StoredLocation> warps = new ConcurrentHashMap<>();
    private volatile boolean perWarpPermission;

    public WarpService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> load() {
        return plugin.database().supply("load the warps", storage -> storage.loadPoints(KIND))
                .thenAccept(loaded -> loaded.forEach(warps::putIfAbsent));
    }

    @Override
    public void reload() {
        perWarpPermission = plugin.settings().getBoolean("warps.per-warp-permission", false);
    }

    public boolean isValidName(String name) {
        return NAME.matcher(name).matches();
    }

    public void set(String rawName, Location location) {
        String name = rawName.toLowerCase(Locale.ROOT);
        StoredLocation stored = StoredLocation.of(location);
        warps.put(name, stored);
        plugin.database().run("save the warp " + name, storage -> storage.savePoint(KIND, name, stored));
    }

    public StoredLocation find(String name) {
        return warps.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean delete(String rawName) {
        String name = rawName.toLowerCase(Locale.ROOT);
        if (warps.remove(name) == null) {
            return false;
        }
        plugin.database().run("delete the warp " + name, storage -> storage.deletePoint(KIND, name));
        return true;
    }

    public boolean canUse(Player player, String name) {
        return !perWarpPermission || player.hasPermission("diamondsmp.warp." + name.toLowerCase(Locale.ROOT));
    }

    public List<String> names(Player viewer) {
        List<String> names = new ArrayList<>(warps.size());
        for (String name : warps.keySet()) {
            if (viewer == null || canUse(viewer, name)) {
                names.add(name);
            }
        }
        Collections.sort(names);
        return names;
    }
}
