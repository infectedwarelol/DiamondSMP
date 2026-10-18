package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReactiveDistanceService implements Service {

    private static final int MIN_CHUNKS = 2;
    private static final int MAX_CHUNKS = 32;
    private static final int MAX_PLAYER_DISTANCE = 512;

    private record Tier(int players, int viewDistance, int playerDistance) {
    }

    private final DiamondSMP plugin;
    private Task checker = Task.NONE;

    private volatile List<Tier> tiers = List.of();
    private volatile int hysteresis = 5;
    private volatile int current = -1;
    private volatile boolean enabled;

    public ReactiveDistanceService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        checker.cancel();
        FileConfiguration config = plugin.settings();
        enabled = config.getBoolean("reactive-distance.enabled", true);
        hysteresis = Math.max(0, config.getInt("reactive-distance.hysteresis", 5));
        tiers = parseTiers(config.getConfigurationSection("reactive-distance.thresholds"));
        current = -1;
        if (!enabled || tiers.isEmpty()) {
            applyAll();
            return;
        }
        check();
        long interval = Math.max(1L, Format.parseSeconds(config.getString("reactive-distance.check-interval"), 5L));
        checker = plugin.scheduler().globalTimer(this::check, interval * 20L, interval * 20L);
    }

    private List<Tier> parseTiers(ConfigurationSection section) {
        if (section == null) {
            return List.of();
        }
        List<Tier> parsed = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection tier = section.getConfigurationSection(key);
            int players;
            try {
                players = Integer.parseInt(key.trim());
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("reactive-distance threshold '" + key + "' is not a player count.");
                continue;
            }
            if (tier == null || players < 0) {
                continue;
            }
            parsed.add(new Tier(players, Math.max(0, tier.getInt("view-distance", 0)),
                    Math.max(0, Math.min(MAX_PLAYER_DISTANCE, tier.getInt("player-distance", 0)))));
        }
        parsed.sort(Comparator.comparingInt(Tier::players));
        return List.copyOf(parsed);
    }

    public int playerDistance() {
        int index = current;
        List<Tier> snapshot = tiers;
        return !enabled || index < 0 || index >= snapshot.size() ? 0 : snapshot.get(index).playerDistance();
    }

    private void check() {
        int online = Bukkit.getOnlinePlayers().size();
        int raised = tierFor(online);
        int next = raised > current ? raised : Math.min(current, tierFor(online + hysteresis));
        if (next == current) {
            return;
        }
        current = next;
        if (next >= 0) {
            Tier tier = tiers.get(next);
            plugin.getLogger().info("Reactive distance tier " + tier.players() + "+ active at " + online
                    + " players (view " + describe(tier.viewDistance(), " chunks") + ", players "
                    + describe(tier.playerDistance(), " blocks") + ").");
        }
        applyAll();
    }

    private int tierFor(int online) {
        int found = -1;
        List<Tier> snapshot = tiers;
        for (int index = 0; index < snapshot.size(); index++) {
            if (snapshot.get(index).players() <= online) {
                found = index;
            }
        }
        return found;
    }

    private void applyAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    public void apply(Player player) {
        int index = current;
        List<Tier> snapshot = tiers;
        int chunks = !enabled || index < 0 || index >= snapshot.size() ? 0 : snapshot.get(index).viewDistance();
        plugin.scheduler().entity(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            int world = player.getWorld().getSendViewDistance();
            int target = chunks <= 0 ? world : Math.min(world, Math.max(MIN_CHUNKS, Math.min(MAX_CHUNKS, chunks)));
            if (player.getSendViewDistance() != target) {
                player.setSendViewDistance(target);
            }
        });
    }

    private static String describe(int value, String unit) {
        return value <= 0 ? "unchanged" : value + unit;
    }

    @Override
    public void shutdown() {
        checker.cancel();
        enabled = false;
        current = -1;
        applyAll();
    }
}
