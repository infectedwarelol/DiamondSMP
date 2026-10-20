package dev.diamondsmp.core.storage;

import java.util.Set;
import java.util.UUID;

public record TeamRecord(String name, UUID owner, String color, boolean pvp, Set<UUID> members) {

    public TeamRecord {
        members = Set.copyOf(members);
    }
}
