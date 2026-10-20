package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class TeamListener implements Listener {

    private final DiamondSMP plugin;

    public TeamListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeammateHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attacker(event.getDamager());
        if (attacker != null && !attacker.equals(victim) && plugin.teams().protects(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
