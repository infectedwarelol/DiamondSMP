package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.storage.StoredLocation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;

public final class SpawnService implements Service {

    public static final String PROTECTION_BYPASS = "diamondsmp.spawn.protection.bypass";
    private static final String KIND = "spawn";
    private static final String NAME = "spawn";

    private final DiamondSMP plugin;
    private volatile StoredLocation spawn;

    private volatile boolean teleportOnFirstJoin = true;
    private volatile boolean teleportOnJoin = true;
    private volatile boolean teleportOnRespawn = true;
    private volatile boolean protectionEnabled = true;
    private volatile double protectionRadiusSquared = 900.0D;
    private volatile boolean protectPvp = true;
    private volatile boolean protectBuild = true;

    public SpawnService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    public void load() {
        Map<String, StoredLocation> points = plugin.database().await(
                plugin.database().supply("load the spawn", storage -> storage.loadPoints(KIND)), 30L);
        spawn = points.get(NAME);
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        teleportOnFirstJoin = config.getBoolean("spawn.teleport-on-first-join", true);
        teleportOnJoin = config.getBoolean("spawn.teleport-on-join", true);
        teleportOnRespawn = config.getBoolean("spawn.teleport-on-respawn", true);
        protectionEnabled = config.getBoolean("spawn.protection.enabled", true);
        double radius = Math.max(0.0D, config.getDouble("spawn.protection.radius", 30.0D));
        protectionRadiusSquared = radius * radius;
        protectPvp = config.getBoolean("spawn.protection.disable-pvp", true);
        protectBuild = config.getBoolean("spawn.protection.disable-build", true);
    }

    public Location spawn() {
        StoredLocation stored = spawn;
        return stored == null ? null : stored.toLocation();
    }

    public boolean isSet() {
        return spawn != null;
    }

    public void set(Location location) {
        StoredLocation stored = StoredLocation.of(location);
        spawn = stored;
        plugin.database().run("save the spawn", storage -> storage.savePoint(KIND, NAME, stored));
    }

    public boolean delete() {
        if (spawn == null) {
            return false;
        }
        spawn = null;
        plugin.database().run("delete the spawn", storage -> storage.deletePoint(KIND, NAME));
        return true;
    }

    public void send(Player player) {
        Location target = spawn();
        if (target != null) {
            plugin.teleports().teleport(player, target);
        }
    }

    public boolean teleportOnFirstJoin() {
        return teleportOnFirstJoin;
    }

    public boolean teleportOnJoin() {
        return teleportOnJoin;
    }

    public boolean teleportOnRespawn() {
        return teleportOnRespawn;
    }

    public boolean blocksPvp(Location location) {
        return protectPvp && isProtected(location);
    }

    public boolean blocksBuild(Player player, Location location) {
        return protectBuild && !player.hasPermission(PROTECTION_BYPASS) && isProtected(location);
    }

    private boolean isProtected(Location location) {
        StoredLocation stored = spawn;
        if (!protectionEnabled || stored == null || location == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null || !world.getName().equals(stored.world())) {
            return false;
        }
        double dx = location.getX() - stored.x();
        double dz = location.getZ() - stored.z();
        return dx * dx + dz * dz <= protectionRadiusSquared;
    }
}
