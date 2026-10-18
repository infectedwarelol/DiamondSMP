package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class SpawnListener implements Listener {

    private final DiamondSMP plugin;

    public SpawnListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.spawn().teleportOnRespawn()) {
            return;
        }
        Location spawn = plugin.spawn().spawn();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.spawn().blocksBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "spawn.protected");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.spawn().blocksBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "spawn.protected");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (plugin.spawn().blocksBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "spawn.protected");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProtectedHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        if (plugin.spawn().blocksPvp(victim.getLocation()) || plugin.spawn().blocksPvp(attacker.getLocation())) {
            event.setCancelled(true);
        }
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
