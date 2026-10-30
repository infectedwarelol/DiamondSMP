package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PearlListener implements Listener {

    private static final double STANDING_WIDTH = 0.6D;
    private static final double STANDING_HEIGHT = 1.8D;
    private static final double MAX_CORRECTION_SQUARED = 1.0D;
    private static final double TOLERANCE = 1.0E-3D;
    private static final int SCAN_RADIUS = 2;

    private final DiamondSMP plugin;

    public PearlListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || !plugin.settings().getBoolean("pearl-glitch.enabled", true)) {
            return;
        }
        Location to = event.getTo();
        World world = to.getWorld();
        if (world == null || !ownsArea(world, to.getBlockX(), to.getBlockZ())) {
            return;
        }
        Player player = event.getPlayer();
        double width = STANDING_WIDTH;
        double height = STANDING_HEIGHT;
        if (player.isSneaking() || player.isSwimming()) {
            BoundingBox current = player.getBoundingBox();
            width = Math.max(current.getWidthX(), current.getWidthZ());
            height = current.getHeight();
        }
        double halfWidth = width / 2.0D;
        if (!collides(world, to.getX(), to.getY(), to.getZ(), halfWidth, height)) {
            return;
        }
        Location safe = findSafe(world, to, halfWidth, height);
        if (safe != null) {
            event.setTo(safe);
            return;
        }
        event.setCancelled(true);
        if (plugin.settings().getBoolean("pearl-glitch.refund-on-cancel", true)
                && player.getGameMode() != GameMode.CREATIVE) {
            for (ItemStack leftover : player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL)).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
        plugin.messages().error(player, "pearl-glitch.cancelled");
    }

    private static boolean ownsArea(World world, int blockX, int blockZ) {
        for (int x = blockX - SCAN_RADIUS; x <= blockX + SCAN_RADIUS; x += SCAN_RADIUS * 2) {
            for (int z = blockZ - SCAN_RADIUS; z <= blockZ + SCAN_RADIUS; z += SCAN_RADIUS * 2) {
                if (!Bukkit.isOwnedByCurrentRegion(world, x >> 4, z >> 4)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Location findSafe(World world, Location to, double halfWidth, double height) {
        BoundingBox box = standingBox(to.getX(), to.getY(), to.getZ(), halfWidth, height);
        List<BoundingBox> obstacles = obstacles(world, box);
        List<Double> shiftsX = new ArrayList<>();
        List<Double> shiftsY = new ArrayList<>();
        List<Double> shiftsZ = new ArrayList<>();
        shiftsX.add(0.0D);
        shiftsY.add(0.0D);
        shiftsZ.add(0.0D);
        for (BoundingBox obstacle : obstacles) {
            addShift(shiftsX, obstacle.getMaxX() - box.getMinX());
            addShift(shiftsX, obstacle.getMinX() - box.getMaxX());
            addShift(shiftsY, obstacle.getMaxY() - box.getMinY());
            addShift(shiftsY, obstacle.getMinY() - box.getMaxY());
            addShift(shiftsZ, obstacle.getMaxZ() - box.getMinZ());
            addShift(shiftsZ, obstacle.getMinZ() - box.getMaxZ());
        }
        List<Shift> shifts = new ArrayList<>();
        for (double shiftX : shiftsX) {
            for (double shiftY : shiftsY) {
                for (double shiftZ : shiftsZ) {
                    double distanceSquared = shiftX * shiftX + shiftY * shiftY + shiftZ * shiftZ;
                    if (distanceSquared > 0.0D && distanceSquared <= MAX_CORRECTION_SQUARED) {
                        shifts.add(new Shift(shiftX, shiftY, shiftZ, distanceSquared));
                    }
                }
            }
        }
        shifts.sort(Comparator.comparingDouble(Shift::distanceSquared));
        for (Shift shift : shifts) {
            double x = to.getX() + shift.x();
            double y = to.getY() + shift.y();
            double z = to.getZ() + shift.z();
            if (!collides(world, x, y, z, halfWidth, height)) {
                return new Location(to.getWorld(), x, y, z, to.getYaw(), to.getPitch());
            }
        }
        return null;
    }

    private static void addShift(List<Double> shifts, double shift) {
        if (Math.abs(shift) <= 1.0D && !shifts.contains(shift)) {
            shifts.add(shift);
        }
    }

    private static BoundingBox standingBox(double x, double y, double z, double halfWidth, double height) {
        return new BoundingBox(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
    }

    private static boolean collides(World world, double x, double y, double z, double halfWidth, double height) {
        return !obstacles(world, standingBox(x, y, z, halfWidth, height)).isEmpty();
    }

    private static List<BoundingBox> obstacles(World world, BoundingBox box) {
        BoundingBox inner = box.clone().expand(-TOLERANCE);
        List<BoundingBox> found = new ArrayList<>();
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.floor(box.getMaxX());
        int minY = (int) Math.floor(box.getMinY()) - 1;
        int maxY = (int) Math.floor(box.getMaxY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.floor(box.getMaxZ());
        for (int blockX = minX; blockX <= maxX; blockX++) {
            for (int blockY = minY; blockY <= maxY; blockY++) {
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    Block block = world.getBlockAt(blockX, blockY, blockZ);
                    if (block.getType().isAir()) {
                        continue;
                    }
                    for (BoundingBox part : block.getCollisionShape().getBoundingBoxes()) {
                        BoundingBox placed = part.clone().shift(blockX, blockY, blockZ);
                        if (placed.overlaps(inner)) {
                            found.add(placed);
                        }
                    }
                }
            }
        }
        return found;
    }

    private record Shift(double x, double y, double z, double distanceSquared) {
    }
}
