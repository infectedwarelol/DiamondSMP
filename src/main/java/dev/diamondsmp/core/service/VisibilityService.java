package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.scheduler.Task;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VisibilityService implements Service {

    public static final String SEE_VANISHED = "diamondsmp.vanish.see";
    private static final double REVEAL_MARGIN = 6.0D;

    private final DiamondSMP plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> hiddenByViewer = new ConcurrentHashMap<>();
    private Task pass = Task.NONE;

    public VisibilityService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        pass.cancel();
        long interval = Math.max(5L, plugin.settings().getLong("reactive-distance.visibility-interval", 10L));
        pass = plugin.scheduler().globalTimer(this::refreshEveryone, interval, interval);
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void setVanished(Player player, boolean hidden) {
        boolean changed = hidden ? vanished.add(player.getUniqueId()) : vanished.remove(player.getUniqueId());
        if (changed) {
            refreshTarget(player);
        }
    }

    public boolean canSee(Object viewer, Player target) {
        if (!vanished.contains(target.getUniqueId())) {
            return true;
        }
        return !(viewer instanceof Player player) || player.equals(target) || player.hasPermission(SEE_VANISHED);
    }

    public void joined(Player player) {
        refreshViewer(player);
        refreshTarget(player);
    }

    public void quit(Player player) {
        hiddenByViewer.remove(player.getUniqueId());
    }

    private void refreshEveryone() {
        if (isIdle()) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            refreshViewer(viewer);
        }
    }

    private boolean isIdle() {
        if (!vanished.isEmpty() || plugin.reactiveDistance().playerDistance() > 0) {
            return false;
        }
        for (Set<UUID> hidden : hiddenByViewer.values()) {
            if (!hidden.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void refreshViewer(Player viewer) {
        plugin.scheduler().entity(viewer, () -> {
            if (!viewer.isOnline()) {
                return;
            }
            int distance = plugin.reactiveDistance().playerDistance();
            Location origin = viewer.getLocation();
            Location scratch = origin.clone();
            for (Player target : Bukkit.getOnlinePlayers()) {
                update(viewer, origin, target, scratch, distance);
            }
        });
    }

    private void refreshTarget(Player target) {
        int distance = plugin.reactiveDistance().playerDistance();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            plugin.scheduler().entity(viewer, () -> {
                if (viewer.isOnline() && target.isOnline()) {
                    Location origin = viewer.getLocation();
                    update(viewer, origin, target, origin.clone(), distance);
                }
            });
        }
    }

    private void update(Player viewer, Location origin, Player target, Location scratch, int distance) {
        if (viewer.equals(target)) {
            return;
        }
        Set<UUID> hidden = hiddenByViewer.computeIfAbsent(viewer.getUniqueId(), key -> ConcurrentHashMap.newKeySet());
        boolean currentlyHidden = hidden.contains(target.getUniqueId());
        boolean visible = canSee(viewer, target)
                && withinDistance(origin, target.getLocation(scratch), distance, currentlyHidden);
        if (visible && currentlyHidden) {
            hidden.remove(target.getUniqueId());
            viewer.showPlayer(plugin, target);
        } else if (!visible && !currentlyHidden) {
            hidden.add(target.getUniqueId());
            viewer.hidePlayer(plugin, target);
        }
    }

    private static boolean withinDistance(Location from, Location to, int distance, boolean currentlyHidden) {
        if (distance <= 0 || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return true;
        }
        double limit = currentlyHidden ? Math.max(1.0D, distance - REVEAL_MARGIN) : distance;
        return from.distanceSquared(to) <= limit * limit;
    }

    @Override
    public void shutdown() {
        pass.cancel();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Set<UUID> hidden = hiddenByViewer.remove(viewer.getUniqueId());
            if (hidden == null || hidden.isEmpty()) {
                continue;
            }
            for (UUID targetId : hidden) {
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) {
                    plugin.scheduler().entity(viewer, () -> viewer.showPlayer(plugin, target));
                }
            }
        }
        hiddenByViewer.clear();
        vanished.clear();
    }
}
