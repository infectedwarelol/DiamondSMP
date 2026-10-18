package dev.diamondsmp.core.storage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface Storage {

    void open();

    void close();

    Optional<ProfileSnapshot> loadProfile(UUID uuid);

    Optional<ProfileSnapshot> findProfile(String name);

    void saveProfile(ProfileSnapshot profile);

    List<ProfileSnapshot> top(LeaderboardType type, int limit);

    Map<String, StoredLocation> loadPoints(String kind);

    void savePoint(String kind, String name, StoredLocation location);

    void deletePoint(String kind, String name);

    List<PlacedBlock> loadPlacedBlocks();

    void replacePlacedBlocks(Collection<PlacedBlock> blocks);
}
