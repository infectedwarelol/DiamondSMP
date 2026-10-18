package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.storage.LeaderboardPage;
import dev.diamondsmp.core.storage.LeaderboardType;
import dev.diamondsmp.core.storage.PlayerProfile;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import dev.diamondsmp.core.storage.StorageException;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ProfileService implements Service {

    private static final long LOAD_TIMEOUT_SECONDS = 10L;

    private final DiamondSMP plugin;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private Task autosave = Task.NONE;

    public ProfileService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        autosave.cancel();
        long seconds = Math.max(30L, Format.parseSeconds(plugin.settings().getString("storage.autosave"), 300L));
        autosave = plugin.scheduler().asyncTimer(this::saveDirty, seconds * 20L, seconds * 20L);
    }

    public void loadBlocking(UUID uuid, String name) {
        if (profiles.containsKey(uuid)) {
            return;
        }
        PlayerProfile profile = plugin.database().await(load(uuid, name), LOAD_TIMEOUT_SECONDS);
        profiles.putIfAbsent(uuid, profile);
    }

    public void loadLater(Player player) {
        UUID uuid = player.getUniqueId();
        if (profiles.containsKey(uuid)) {
            return;
        }
        load(uuid, player.getName()).thenAccept(profile -> {
            if (Bukkit.getPlayer(uuid) != null) {
                profiles.putIfAbsent(uuid, profile);
            }
        });
    }

    private CompletableFuture<PlayerProfile> load(UUID uuid, String name) {
        return plugin.database().supply("load the profile of " + name, storage -> {
            Optional<ProfileSnapshot> snapshot = storage.loadProfile(uuid);
            return snapshot.map(found -> PlayerProfile.from(found, name))
                    .orElseGet(() -> PlayerProfile.fresh(uuid, name));
        });
    }

    public void forget(UUID uuid) {
        profiles.remove(uuid);
    }

    public void unload(Player player) {
        PlayerProfile profile = profiles.remove(player.getUniqueId());
        if (profile == null) {
            return;
        }
        profile.touch(player.getName(), playtime(player));
        save(profile);
    }

    public static long playtime(Player player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L;
    }

    public PlayerProfile profile(UUID uuid) {
        return profiles.get(uuid);
    }

    public PlayerProfile profile(Player player) {
        return profiles.get(player.getUniqueId());
    }

    public CompletableFuture<Optional<ProfileSnapshot>> lookup(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            PlayerProfile profile = profile(online);
            if (profile != null) {
                return CompletableFuture.completedFuture(Optional.of(profile.snapshot()));
            }
        }
        return plugin.database().supply("look up " + name, storage -> storage.findProfile(name));
    }

    public CompletableFuture<List<ProfileSnapshot>> top(LeaderboardType type, int limit) {
        saveDirty();
        return plugin.database().supply("read the " + type.id() + " leaderboard",
                storage -> storage.top(type, 0, limit));
    }

    public CompletableFuture<LeaderboardPage> page(LeaderboardType type, int offset, int limit) {
        saveDirty();
        return plugin.database().supply("read a page of the " + type.id() + " leaderboard",
                storage -> new LeaderboardPage(storage.countProfiles(), storage.top(type, offset, limit)));
    }

    public void saveDirty() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profiles.get(online.getUniqueId());
            if (profile != null) {
                profile.recordPlaytime(playtime(online));
            }
        }
        for (PlayerProfile profile : profiles.values()) {
            if (profile.consumeDirty()) {
                save(profile);
            }
        }
    }

    private CompletableFuture<Void> save(PlayerProfile profile) {
        ProfileSnapshot snapshot = profile.snapshot();
        return plugin.database().run("save the profile of " + snapshot.name(), storage -> storage.saveProfile(snapshot));
    }

    @Override
    public void shutdown() {
        autosave.cancel();
        List<CompletableFuture<Void>> saves = new ArrayList<>(profiles.size());
        for (Player online : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profiles.get(online.getUniqueId());
            if (profile != null) {
                profile.touch(online.getName(), playtime(online));
            }
        }
        for (PlayerProfile profile : profiles.values()) {
            saves.add(save(profile));
        }
        try {
            plugin.database().await(CompletableFuture.allOf(saves.toArray(new CompletableFuture[0])), 30L);
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.SEVERE, "Some player profiles could not be saved on shutdown", ex);
        }
        profiles.clear();
    }
}
