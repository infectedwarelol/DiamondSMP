package dev.diamondsmp.core.listener;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.service.ScreenShareService;
import dev.diamondsmp.core.service.ScreenShareSession;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import java.util.Locale;

public final class FreezeListener implements Listener {

    private static final double ANCHOR_TOLERANCE_SQUARED = 1.0E-4D;

    private final DiamondSMP plugin;

    public FreezeListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    private ScreenShareService sessions() {
        return plugin.screenShare();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) {
            return;
        }
        ScreenShareSession session = sessions().session(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        Location to = event.getTo();
        Location anchor = session.anchor();
        if (!session.hasArrived() || anchor.getWorld() == null || !anchor.getWorld().equals(to.getWorld())) {
            event.setCancelled(true);
        } else if (to.distanceSquared(anchor) > ANCHOR_TOLERANCE_SQUARED) {
            event.setTo(new Location(anchor.getWorld(), anchor.getX(), anchor.getY(), anchor.getZ(),
                    to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        ScreenShareSession session = sessions().session(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        Location to = event.getTo();
        Location anchor = session.anchor();
        if (anchor.getWorld() == null || !anchor.getWorld().equals(to.getWorld())
                || to.distanceSquared(anchor) > ANCHOR_TOLERANCE_SQUARED) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!sessions().isFrozen(player)) {
            return;
        }
        String message = event.getMessage();
        int space = message.indexOf(' ');
        String label = (space < 0 ? message.substring(1) : message.substring(1, space)).toLowerCase(Locale.ROOT);
        label = label.substring(label.indexOf(':') + 1);
        if (!sessions().allowsCommand(label)) {
            event.setCancelled(true);
            plugin.messages().send(player, "screenshare.command-blocked");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (sessions().isFrozen(event.getPlayer())) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (sessions().invulnerable() && isFrozen(event.getEntity())) {
            event.setCancelled(true);
        } else if (event instanceof EntityDamageByEntityEvent byEntity) {
            cancel(event, source(byEntity.getDamager()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        cancel(event, source(event.getAttacker()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        cancel(event, event.getEntered());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        cancel(event, source(event.getRemover()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        cancel(event, source(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        cancel(event, event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        cancel(event, event.getWhoClicked());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        cancel(event, event.getWhoClicked());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        cancel(event, event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.isGliding()) {
            cancel(event, event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpectate(PlayerStartSpectatingEntityEvent event) {
        cancel(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        sessions().kicked(event.getPlayer(), event.getCause());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onQuit(PlayerQuitEvent event) {
        sessions().handleQuit(event.getPlayer(), event.getReason());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Location anchor = sessions().respawnLocation(event.getPlayer());
        if (anchor != null) {
            event.setRespawnLocation(anchor);
        }
    }

    private void cancel(Cancellable event, Entity entity) {
        if (isFrozen(entity)) {
            event.setCancelled(true);
        }
    }

    private boolean isFrozen(Entity entity) {
        return entity instanceof Player player && sessions().isFrozen(player);
    }

    private static Entity source(Entity entity) {
        return entity instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter
                ? shooter : entity;
    }
}
