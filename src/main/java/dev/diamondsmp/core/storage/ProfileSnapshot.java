package dev.diamondsmp.core.storage;

import java.util.Set;
import java.util.UUID;

public record ProfileSnapshot(UUID uuid, String name, int kills, int deaths, int streak, int bestStreak,
                              String killEffect, boolean socialSpy, Set<UUID> ignored, Set<UUID> blocked,
                              long firstJoin, long lastSeen, long playtime, boolean hidesEffects) {

    public ProfileSnapshot {
        ignored = Set.copyOf(ignored);
        blocked = Set.copyOf(blocked);
    }

    public ProfileSnapshot withStreak(int value) {
        int current = Math.max(0, value);
        return new ProfileSnapshot(uuid, name, kills, deaths, current, Math.max(bestStreak, current),
                killEffect, socialSpy, ignored, blocked, firstJoin, lastSeen, playtime, hidesEffects);
    }

    public double ratio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}
