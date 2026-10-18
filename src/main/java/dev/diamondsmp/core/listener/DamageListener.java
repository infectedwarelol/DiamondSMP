package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public final class DamageListener implements Listener {

    private final DiamondSMP plugin;

    public DamageListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onGodDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && plugin.god().isGod(player)) {
            event.setCancelled(true);
            player.setFireTicks(0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            plugin.teleports().damaged(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        boolean protectedFromHunger = plugin.ffaRules().noHunger() || plugin.god().isGod(player);
        if (protectedFromHunger && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
            player.setSaturation(Math.max(player.getSaturation(), 5.0F));
        }
    }
}
