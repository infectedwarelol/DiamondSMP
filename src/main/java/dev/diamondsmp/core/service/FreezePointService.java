package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.storage.StoredLocation;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public final class FreezePointService implements Service {

    private static final String KIND = "freeze";

    public record FreezePoint(int id, StoredLocation location) {

        public Location toLocation() {
            Location resolved = location.toLocation();
            if (resolved == null) {
                return null;
            }
            World world = resolved.getWorld();
            return resolved.getY() < world.getMinHeight() || resolved.getY() >= world.getMaxHeight() ? null : resolved;
        }

        public Object[] placeholders() {
            return new Object[]{
                    "freeze_point", id,
                    "world", location.world(),
                    "x", Format.decimal(location.x()),
                    "y", Format.decimal(location.y()),
                    "z", Format.decimal(location.z()),
                    "yaw", Format.decimal(location.yaw()),
                    "pitch", Format.decimal(location.pitch())
            };
        }
    }

    private final DiamondSMP plugin;
    private final NavigableMap<Integer, FreezePoint> points = new ConcurrentSkipListMap<>();

    public FreezePointService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> load() {
        return plugin.database().supply("load the freeze points", storage -> storage.loadPoints(KIND)).thenAccept(stored -> {
            for (Map.Entry<String, StoredLocation> entry : stored.entrySet()) {
                try {
                    int id = Integer.parseInt(entry.getKey());
                    if (id > 0) {
                        points.putIfAbsent(id, new FreezePoint(id, entry.getValue()));
                    }
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("Skipped the freeze point '" + entry.getKey() + "': its id is not a number.");
                }
            }
        });
    }

    public synchronized FreezePoint create(Location location) {
        int id = 1;
        while (points.containsKey(id)) {
            id++;
        }
        FreezePoint point = new FreezePoint(id, StoredLocation.of(location));
        points.put(id, point);
        String name = String.valueOf(id);
        plugin.database().run("save the freeze point #" + id, storage -> storage.savePoint(KIND, name, point.location()));
        return point;
    }

    public boolean delete(FreezePoint point) {
        if (!points.remove(point.id(), point)) {
            return false;
        }
        String name = String.valueOf(point.id());
        plugin.database().run("delete the freeze point #" + name, storage -> storage.deletePoint(KIND, name));
        return true;
    }

    public FreezePoint find(String id) {
        try {
            return points.get(Integer.parseInt(id.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Collection<FreezePoint> all() {
        return points.values();
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>(points.size());
        for (Integer id : points.keySet()) {
            ids.add(String.valueOf(id));
        }
        return ids;
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public int size() {
        return points.size();
    }
}
