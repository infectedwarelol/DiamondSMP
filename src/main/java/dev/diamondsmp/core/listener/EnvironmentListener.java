package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.potion.PotionEffectType;

public final class EnvironmentListener implements Listener {

    private final DiamondSMP plugin;

    public EnvironmentListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.environment().refreshNightVision(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.environment().refreshNightVisionLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.environment().refreshNightVisionLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.environment().enforceDifficulty(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getModifiedType().equals(PotionEffectType.NIGHT_VISION)
                && (event.getAction() == EntityPotionEffectEvent.Action.REMOVED
                || event.getAction() == EntityPotionEffectEvent.Action.CLEARED)) {
            plugin.environment().refreshNightVisionLater(player);
        }
    }
}
