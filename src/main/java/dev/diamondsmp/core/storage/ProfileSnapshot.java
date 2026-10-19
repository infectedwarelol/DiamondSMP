package dev.diamondsmp.core.storage;

import java.util.Set;
import java.util.UUID;

public record ProfileSnapshot(UUID uuid, String name, int kills, int deaths, int streak, int bestStreak,
                              String killEffect, boolean socialSpy, Set<UUID> ignored, Set<UUID> blocked,
                              long firstJoin, long lastSeen, long playtime, boolean hidesEffects,
                              boolean privateEffects, Set<String> ownedEffects) {

    public ProfileSnapshot {
        ignored = Set.copyOf(ignored);
        blocked = Set.copyOf(blocked);
        ownedEffects = Set.copyOf(ownedEffects);
    }

    public ProfileSnapshot withStreak(int value) {
        int current = Math.max(0, value);
        return new ProfileSnapshot(uuid, name, kills, deaths, current, Math.max(bestStreak, current),
                killEffect, socialSpy, ignored, blocked, firstJoin, lastSeen, playtime, hidesEffects, privateEffects,
                ownedEffects);
    }

    public ProfileSnapshot withOwnedEffects(Set<String> owned) {
        return new ProfileSnapshot(uuid, name, kills, deaths, streak, bestStreak, killEffect, socialSpy, ignored,
                blocked, firstJoin, lastSeen, playtime, hidesEffects, privateEffects, owned);
    }

    public double ratio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}
