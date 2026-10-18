package dev.diamondsmp.core.hook;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AjLeaderboardsHook {

    private static final String PLUGIN_NAME = "ajLeaderboards";
    private static final int MAX_PENDING = 2000;

    private final DiamondSMP plugin;
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private Task pushTask = Task.NONE;

    private volatile List<String> boards = List.of();
    private volatile Object cache;
    private volatile Method updateStat;
    private volatile boolean active;

    public AjLeaderboardsHook(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        shutdown();
        if (!plugin.settings().getBoolean("ajleaderboards.enabled", true)
                || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        boards = List.copyOf(plugin.settings().getStringList("ajleaderboards.boards"));
        if (boards.isEmpty()) {
            return;
        }
        plugin.scheduler().asyncLater(() -> {
            Plugin ajlb = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (ajlb == null || !ajlb.isEnabled()) {
                return;
            }
            attach(ajlb);
        }, 100L);
    }

    private void attach(Plugin ajlb) {
        try {
            Object foundCache = ajlb.getClass().getMethod("getCache").invoke(ajlb);
            Object topManager = ajlb.getClass().getMethod("getTopManager").invoke(ajlb);
            if (foundCache == null || topManager == null) {
                plugin.getLogger().warning("ajLeaderboards is not fully started; its boards are not registered.");
                return;
            }
            Method boardExists = foundCache.getClass().getMethod("boardExists", String.class);
            Method createBoard = foundCache.getClass().getMethod("createBoard", String.class);
            boolean created = false;
            for (String board : boards) {
                if (Boolean.TRUE.equals(boardExists.invoke(foundCache, board))) {
                    continue;
                }
                if (Boolean.TRUE.equals(createBoard.invoke(foundCache, board))) {
                    created = true;
                    plugin.getLogger().info("Created the ajLeaderboards board '" + board + "'.");
                } else {
                    plugin.getLogger().warning("ajLeaderboards refused to create the board '" + board + "'.");
                }
            }
            if (created) {
                topManager.getClass().getMethod("fetchBoards").invoke(topManager);
            }
            updateStat = foundCache.getClass().getMethod("updateStat", String.class, OfflinePlayer.class);
            cache = foundCache;
            active = true;
            long interval = Math.max(1L, Format.parseSeconds(
                    plugin.settings().getString("ajleaderboards.push-interval"), 5L));
            pushTask = plugin.scheduler().asyncTimer(this::flush, interval * 20L, interval * 20L);
            plugin.getLogger().info("Hooked into ajLeaderboards (boards: " + String.join(", ", boards) + ").");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            active = false;
            plugin.getLogger().warning("Could not hook into ajLeaderboards (" + ex.getClass().getSimpleName()
                    + ": " + ex.getMessage() + "). Add the boards manually with /ajlb add.");
        }
    }

    public boolean isActive() {
        return active;
    }

    public void queue(UUID uuid) {
        if (active && uuid != null && pending.size() < MAX_PENDING) {
            pending.add(uuid);
        }
    }

    private void flush() {
        if (!active || pending.isEmpty()) {
            return;
        }
        for (UUID uuid : pending) {
            pending.remove(uuid);
            OfflinePlayer target = resolve(uuid);
            if (target == null) {
                continue;
            }
            try {
                for (String board : boards) {
                    updateStat.invoke(cache, board, target);
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                pending.add(uuid);
                plugin.getLogger().warning("ajLeaderboards update failed, retrying on the next push: "
                        + ex.getMessage());
                return;
            }
            plugin.killStreaks().forgetOffline(uuid);
        }
    }

    private static OfflinePlayer resolve(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() == null ? null : offline;
    }

    public void shutdown() {
        active = false;
        pushTask.cancel();
        pushTask = Task.NONE;
        pending.clear();
        cache = null;
        updateStat = null;
    }
}
