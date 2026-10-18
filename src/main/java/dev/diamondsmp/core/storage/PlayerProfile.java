package dev.diamondsmp.core.storage;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class PlayerProfile {

    private final UUID uuid;
    private final boolean firstJoin;
    private final long firstJoinTime;
    private final AtomicInteger kills;
    private final AtomicInteger deaths;
    private final AtomicInteger streak;
    private final AtomicInteger bestStreak;
    private final Set<UUID> ignored = ConcurrentHashMap.newKeySet();
    private final Set<UUID> blocked = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean dirty = new AtomicBoolean();

    private volatile String name;
    private volatile String killEffect;
    private volatile boolean socialSpy;
    private volatile long lastSeen;
    private final AtomicLong playtime = new AtomicLong();

    private PlayerProfile(UUID uuid, String name, boolean firstJoin, long firstJoinTime, int kills, int deaths,
                          int streak, int bestStreak, String killEffect, boolean socialSpy, long lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = firstJoin;
        this.firstJoinTime = firstJoinTime;
        this.kills = new AtomicInteger(kills);
        this.deaths = new AtomicInteger(deaths);
        this.streak = new AtomicInteger(streak);
        this.bestStreak = new AtomicInteger(bestStreak);
        this.killEffect = killEffect;
        this.socialSpy = socialSpy;
        this.lastSeen = lastSeen;
    }

    public static PlayerProfile fresh(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        PlayerProfile profile = new PlayerProfile(uuid, name, true, now, 0, 0, 0, 0, null, false, now);
        profile.dirty.set(true);
        return profile;
    }

    public static PlayerProfile from(ProfileSnapshot snapshot, String currentName) {
        PlayerProfile profile = new PlayerProfile(snapshot.uuid(), currentName, false, snapshot.firstJoin(),
                snapshot.kills(), snapshot.deaths(), snapshot.streak(), snapshot.bestStreak(),
                snapshot.killEffect(), snapshot.socialSpy(), snapshot.lastSeen());
        profile.ignored.addAll(snapshot.ignored());
        profile.blocked.addAll(snapshot.blocked());
        profile.playtime.set(snapshot.playtime());
        if (!currentName.equals(snapshot.name())) {
            profile.dirty.set(true);
        }
        return profile;
    }

    public ProfileSnapshot snapshot() {
        return new ProfileSnapshot(uuid, name, kills.get(), deaths.get(), streak.get(), bestStreak.get(),
                killEffect, socialSpy, ignored, blocked, firstJoinTime, lastSeen, playtime.get());
    }

    public String name() {
        return name;
    }

    public boolean isFirstJoin() {
        return firstJoin;
    }

    public int kills() {
        return kills.get();
    }

    public int deaths() {
        return deaths.get();
    }

    public int streak() {
        return streak.get();
    }

    public int bestStreak() {
        return bestStreak.get();
    }

    public double ratio() {
        int currentDeaths = deaths.get();
        return currentDeaths == 0 ? kills.get() : (double) kills.get() / currentDeaths;
    }

    public int recordKill() {
        kills.incrementAndGet();
        int current = streak.incrementAndGet();
        bestStreak.accumulateAndGet(current, Math::max);
        dirty.set(true);
        return current;
    }

    public int recordDeath() {
        deaths.incrementAndGet();
        dirty.set(true);
        return streak.getAndSet(0);
    }

    public String killEffect() {
        return killEffect;
    }

    public void killEffect(String effect) {
        this.killEffect = effect;
        dirty.set(true);
    }

    public boolean socialSpy() {
        return socialSpy;
    }

    public void socialSpy(boolean enabled) {
        this.socialSpy = enabled;
        dirty.set(true);
    }

    public boolean isIgnoring(UUID other) {
        return ignored.contains(other);
    }

    public boolean toggleIgnore(UUID other) {
        return toggle(ignored, other);
    }

    public boolean isBlocking(UUID other) {
        return blocked.contains(other);
    }

    public boolean toggleBlock(UUID other) {
        return toggle(blocked, other);
    }

    private boolean toggle(Set<UUID> relation, UUID other) {
        boolean added = relation.add(other);
        if (!added) {
            relation.remove(other);
        }
        dirty.set(true);
        return added;
    }

    public void setStreak(int value) {
        int current = Math.max(0, value);
        streak.set(current);
        bestStreak.accumulateAndGet(current, Math::max);
        dirty.set(true);
    }

    public void addStreak(int delta) {
        int current = streak.updateAndGet(value -> Math.max(0, value + delta));
        bestStreak.accumulateAndGet(current, Math::max);
        dirty.set(true);
    }

    public void recordPlaytime(long currentPlaytime) {
        if (playtime.getAndAccumulate(currentPlaytime, Math::max) < currentPlaytime) {
            dirty.set(true);
        }
    }

    public void touch(String currentName, long currentPlaytime) {
        this.name = currentName;
        this.lastSeen = System.currentTimeMillis();
        playtime.accumulateAndGet(currentPlaytime, Math::max);
        dirty.set(true);
    }

    public boolean consumeDirty() {
        return dirty.getAndSet(false);
    }
}
