package dev.diamondsmp.core.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public static StoredLocation of(Location location) {
        World world = location.getWorld();
        return new StoredLocation(world == null ? "" : world.getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Location toLocation() {
        World resolved = Bukkit.getWorld(world);
        return resolved == null ? null : new Location(resolved, x, y, z, yaw, pitch);
    }
}
