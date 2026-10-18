package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.hook.AjLeaderboardsHook;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.storage.LeaderboardType;
import dev.diamondsmp.core.storage.PlayerProfile;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import dev.diamondsmp.core.util.Format;
import dev.diamondsmp.core.util.SoundSpec;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class KillStreakService implements Service {

    public enum Change {
        SET,
        ADD,
        CLEAR
    }

    public record Entry(UUID uuid, String name, int value) {
    }

    public record Known(String name, int current, int highest) {
    }

    private final DiamondSMP plugin;
    private final Map<UUID, Known> known = new ConcurrentHashMap<>();
    private final AtomicBoolean leaderboardDirty = new AtomicBoolean(true);
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private final AjLeaderboardsHook leaderboards;
    private Task refresher = Task.NONE;

    private volatile List<Entry> topCurrent = List.of();
    private volatile List<Entry> topHighest = List.of();

    private volatile int[] milestones = new int[0];
    private volatile int repeatAboveHighest = 25;
    private volatile int announceEndMinimum = 5;
    private volatile boolean actionBarOnKill = true;
    private volatile boolean titles = true;
    private volatile Title.Times titleTimes = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2),
            Duration.ofMillis(500));
    private volatile SoundSpec milestoneSound = SoundSpec.NONE;
    private volatile SoundSpec endSound = SoundSpec.NONE;
    private volatile int leaderboardSize = 10;
    private volatile String emptyName = "---";
    private volatile String emptyValue = "0";

    public KillStreakService(DiamondSMP plugin) {
        this.plugin = plugin;
        this.leaderboards = new AjLeaderboardsHook(plugin);
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        milestones = config.getIntegerList("killstreak.milestones").stream()
                .filter(value -> value != null && value > 0).mapToInt(Integer::intValue).distinct().sorted().toArray();
        repeatAboveHighest = Math.max(0, config.getInt("killstreak.repeat-above-highest", 25));
        announceEndMinimum = Math.max(0, config.getInt("killstreak.announce-end-minimum", 5));
        actionBarOnKill = config.getBoolean("killstreak.action-bar.enabled", true)
                && config.getBoolean("killstreak.action-bar.on-kill", true);
        titles = config.getBoolean("killstreak.titles.enabled", true);
        titleTimes = Title.Times.times(ticks(config.getInt("killstreak.titles.fade-in-ticks", 10)),
                ticks(config.getInt("killstreak.titles.stay-ticks", 40)),
                ticks(config.getInt("killstreak.titles.fade-out-ticks", 10)));
        milestoneSound = SoundSpec.from(config.getConfigurationSection("killstreak.sounds.milestone"));
        endSound = SoundSpec.from(config.getConfigurationSection("killstreak.sounds.streak-end"));
        leaderboardSize = Math.max(1, Math.min(100, config.getInt("killstreak.leaderboard.size", 10)));
        emptyName = config.getString("killstreak.leaderboard.empty-name", "---");
        emptyValue = config.getString("killstreak.leaderboard.empty-value", "0");
        long refresh = Math.max(1L, Format.parseSeconds(config.getString("killstreak.leaderboard.refresh"), 5L));
        refresher.cancel();
        leaderboardDirty.set(true);
        refresher = plugin.scheduler().asyncTimer(this::refreshLeaderboards, 20L, refresh * 20L);
        leaderboards.reload();
    }

    public void killed(Player killer, int streak) {
        remember(killer);
        if (actionBarOnKill) {
            plugin.messages().actionBar(killer, "killstreak.action-bar", "streak", streak);
        }
        if (!isMilestone(streak)) {
            return;
        }
        plugin.messages().broadcast("killstreak.milestone", "player", killer.getName(), "killer", killer.getName(),
                "streak", streak);
        plugin.scheduler().entity(killer, () -> {
            if (titles) {
                plugin.messages().title(killer, "killstreak.title", titleTimes, "streak", streak);
            }
            milestoneSound.play(killer);
        });
    }

    public void ended(Player victim, Player killer, int endedStreak) {
        remember(victim);
        if (announceEndMinimum <= 0 || endedStreak < announceEndMinimum) {
            return;
        }
        if (killer == null || killer.equals(victim)) {
            plugin.messages().broadcast("killstreak.ended-self", "player", victim.getName(),
                    "victim", victim.getName(), "streak", endedStreak);
        } else {
            plugin.messages().broadcast("killstreak.ended", "player", victim.getName(), "victim", victim.getName(),
                    "killer", killer.getName(), "streak", endedStreak);
        }
        plugin.scheduler().entity(victim, () -> endSound.play(victim));
    }

    public boolean isMilestone(int streak) {
        int[] values = milestones;
        if (streak <= 0 || values.length == 0) {
            return false;
        }
        int highest = values[values.length - 1];
        if (streak <= highest) {
            return Arrays.binarySearch(values, streak) >= 0;
        }
        return repeatAboveHighest > 0 && (streak - highest) % repeatAboveHighest == 0;
    }

    public Known lookup(UUID uuid) {
        PlayerProfile profile = plugin.profiles().profile(uuid);
        if (profile != null) {
            return new Known(profile.name(), profile.streak(), profile.bestStreak());
        }
        return known.get(uuid);
    }

    public CompletableFuture<Optional<Known>> change(String name, Change change, int amount) {
        Player online = Bukkit.getPlayerExact(name);
        PlayerProfile profile = online == null ? null : plugin.profiles().profile(online);
        if (profile != null) {
            switch (change) {
                case SET -> profile.setStreak(amount);
                case ADD -> profile.addStreak(amount);
                case CLEAR -> profile.setStreak(0);
            }
            remember(online);
            return CompletableFuture.completedFuture(Optional.of(lookup(online.getUniqueId())));
        }
        return plugin.database().supply("change the streak of " + name, storage -> {
            Optional<ProfileSnapshot> found = storage.findProfile(name);
            if (found.isEmpty()) {
                return Optional.<ProfileSnapshot>empty();
            }
            ProfileSnapshot current = found.get();
            ProfileSnapshot updated = current.withStreak(switch (change) {
                case SET -> amount;
                case ADD -> current.streak() + amount;
                case CLEAR -> 0;
            });
            storage.saveProfile(updated);
            return Optional.of(updated);
        }).thenApply(updated -> updated.map(snapshot -> {
            Known result = new Known(snapshot.name(), snapshot.streak(), snapshot.bestStreak());
            known.put(snapshot.uuid(), result);
            changed(snapshot.uuid());
            return result;
        }));
    }

    private void remember(Player player) {
        PlayerProfile profile = plugin.profiles().profile(player);
        if (profile == null) {
            return;
        }
        known.put(player.getUniqueId(), new Known(player.getName(), profile.streak(), profile.bestStreak()));
        changed(player.getUniqueId());
    }

    private void changed(UUID uuid) {
        leaderboardDirty.set(true);
        leaderboards.queue(uuid);
    }

    public void forgetOffline(UUID uuid) {
        if (Bukkit.getPlayer(uuid) == null) {
            known.remove(uuid);
        }
    }

    private void refreshLeaderboards() {
        if (!leaderboardDirty.compareAndSet(true, false) || !refreshing.compareAndSet(false, true)) {
            return;
        }
        int size = leaderboardSize;
        plugin.profiles().top(LeaderboardType.CURRENT, size)
                .thenCombine(plugin.profiles().top(LeaderboardType.STREAK, size), (current, highest) -> {
                    topCurrent = entries(current, false);
                    topHighest = entries(highest, true);
                    return null;
                })
                .whenComplete((ignored, error) -> {
                    refreshing.set(false);
                    if (!leaderboards.isActive()) {
                        known.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
                    }
                    if (error != null) {
                        leaderboardDirty.set(true);
                    }
                });
    }

    private static List<Entry> entries(List<ProfileSnapshot> snapshots, boolean highest) {
        List<Entry> entries = new ArrayList<>(snapshots.size());
        for (ProfileSnapshot snapshot : snapshots) {
            int value = highest ? snapshot.bestStreak() : snapshot.streak();
            if (value <= 0) {
                break;
            }
            entries.add(new Entry(snapshot.uuid(), snapshot.name(), value));
        }
        return List.copyOf(entries);
    }

    public Entry top(boolean highest, int rank) {
        List<Entry> board = highest ? topHighest : topCurrent;
        return rank >= 1 && rank <= board.size() ? board.get(rank - 1) : null;
    }

    public String emptyName() {
        return emptyName;
    }

    public String emptyValue() {
        return emptyValue;
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    @Override
    public void shutdown() {
        refresher.cancel();
        leaderboards.shutdown();
        known.clear();
    }
}
