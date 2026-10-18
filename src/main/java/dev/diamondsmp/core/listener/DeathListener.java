package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.WorldLoadEvent;

public final class DeathListener implements Listener {

    private final DiamondSMP plugin;

    public DeathListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        plugin.ffaRules().applyDeath(event);
        plugin.stats().handleDeath(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.ffaRules().apply(event.getWorld());
    }
}
