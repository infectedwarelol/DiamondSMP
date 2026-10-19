package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.effect.Corpse;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.KillEffectRegistry;
import dev.diamondsmp.core.gui.KillEffectMenu;
import dev.diamondsmp.core.message.Text;
import dev.diamondsmp.core.storage.PlayerProfile;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import dev.diamondsmp.core.util.Format;
import dev.diamondsmp.core.util.Lookup;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public final class KillEffectService implements Service {

    public static final String NONE = "none";
    public static final String RANDOM = "random";
    public static final String ALL = "all";
    public static final String PERMISSION_PREFIX = "diamondsmp.killeffect.";

    public enum Grant {
        GRANTED,
        REVOKED,
        UNCHANGED,
        UNKNOWN_PLAYER
    }

    public record Configured(KillEffect effect, String name, Material icon, List<String> description,
                             double intensity, double duration, float volume) {
    }

    private final DiamondSMP plugin;
    private final KillEffectRegistry registry = new KillEffectRegistry();
    private final Set<EffectSession> active = ConcurrentHashMap.newKeySet();
    private volatile Map<String, Configured> enabled = Map.of();
    private volatile Material lockedIcon = Material.GRAY_DYE;
    private volatile Material noneIcon = Material.BARRIER;
    private volatile Material randomIcon = Material.ENDER_CHEST;
    private volatile double density = 1.0D;
    private volatile int particleBudget = 350;
    private volatile int maxTicks = 140;
    private volatile double viewDistanceSquared = 48.0D * 48.0D;
    private volatile int maxActive = 6;
    private volatile int reduceAbove = 3;
    private volatile double reducedDensity = 0.5D;

    public KillEffectService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        lockedIcon = Lookup.material(config.getString("kill-effects.locked-icon"), Material.GRAY_DYE);
        noneIcon = Lookup.material(config.getString("kill-effects.none-icon"), Material.BARRIER);
        randomIcon = Lookup.material(config.getString("kill-effects.random-icon"), Material.ENDER_CHEST);
        density = clamp(config.getDouble("kill-effects.density", 1.0D), 0.1D, 3.0D);
        particleBudget = Math.max(20, config.getInt("kill-effects.performance.particles-per-tick", 350));
        maxTicks = (int) Math.max(20L, Format.parseSeconds(config.getString("kill-effects.performance.max-duration"), 7L) * 20L);
        double viewDistance = clamp(config.getDouble("kill-effects.performance.view-distance", 48.0D), 8.0D, 128.0D);
        viewDistanceSquared = viewDistance * viewDistance;
        maxActive = Math.max(1, config.getInt("kill-effects.performance.max-active", 6));
        reduceAbove = Math.max(0, config.getInt("kill-effects.performance.reduce-above", 3));
        reducedDensity = clamp(config.getDouble("kill-effects.performance.reduced-density", 0.5D), 0.1D, 1.0D);
        Map<String, Configured> parsed = new LinkedHashMap<>();
        for (KillEffect effect : registry.all()) {
            ConfigurationSection section = config.getConfigurationSection("kill-effects.effects." + effect.id());
            if (section != null && !section.getBoolean("enabled", true)) {
                continue;
            }
            String name = section == null ? effect.name() : section.getString("name", effect.name());
            Material icon = section == null ? effect.icon() : Lookup.material(section.getString("icon"), effect.icon());
            List<String> description = section == null || !section.isList("description")
                    ? effect.description() : section.getStringList("description");
            double intensity = section == null ? 1.0D : clamp(section.getDouble("intensity", 1.0D), 0.1D, 3.0D);
            double duration = section == null ? 1.0D : clamp(section.getDouble("duration", 1.0D), 0.5D, 2.0D);
            float volume = section == null ? 1.0F : (float) clamp(section.getDouble("volume", 1.0D), 0.0D, 2.0D);
            parsed.put(effect.id(), new Configured(effect, name, icon, List.copyOf(description), intensity, duration, volume));
        }
        enabled = parsed;
    }

    public List<Configured> available() {
        return new ArrayList<>(enabled.values());
    }

    public Configured find(String id) {
        return id == null ? null : enabled.get(id.toLowerCase(Locale.ROOT));
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>(enabled.size() + 2);
        ids.add(NONE);
        ids.add(RANDOM);
        ids.addAll(enabled.keySet());
        return ids;
    }

    public boolean isUnknown(String id) {
        return !NONE.equals(id) && !RANDOM.equals(id) && find(id) == null;
    }

    public Material lockedIcon() {
        return lockedIcon;
    }

    public Material noneIcon() {
        return noneIcon;
    }

    public Material randomIcon() {
        return randomIcon;
    }

    public Component displayName(String id) {
        if (NONE.equals(id)) {
            return plugin.messages().display("menus.killeffect.none-name");
        }
        if (RANDOM.equals(id)) {
            return plugin.messages().display("menus.killeffect.random-name");
        }
        Configured effect = find(id);
        return effect == null ? Component.text(id) : Text.display(plugin.messages().theme(effect.name()));
    }

    public boolean isLocked(Player player, String id) {
        if (NONE.equals(id) || player.hasPermission(PERMISSION_PREFIX + id)) {
            return false;
        }
        PlayerProfile profile = plugin.profiles().profile(player);
        return profile == null || !profile.ownsEffect(id);
    }

    public List<String> effectIds() {
        return new ArrayList<>(enabled.keySet());
    }

    public CompletableFuture<Grant> grant(String playerName, String id, boolean give) {
        List<String> targets = ALL.equals(id) ? effectIds() : List.of(id);
        Player online = Bukkit.getPlayerExact(playerName);
        PlayerProfile profile = online == null ? null : plugin.profiles().profile(online);
        if (profile != null) {
            boolean changed = false;
            for (String target : targets) {
                changed |= give ? profile.grantEffect(target) : profile.revokeEffect(target);
            }
            return CompletableFuture.completedFuture(outcome(changed, give));
        }
        return plugin.database().supply("change the kill effects of " + playerName, storage -> {
            Optional<ProfileSnapshot> found = storage.findProfile(playerName);
            if (found.isEmpty()) {
                return Grant.UNKNOWN_PLAYER;
            }
            Set<String> owned = new HashSet<>(found.get().ownedEffects());
            boolean changed = give ? owned.addAll(targets) : owned.removeAll(new HashSet<>(targets));
            if (changed) {
                storage.saveProfile(found.get().withOwnedEffects(owned));
            }
            return outcome(changed, give);
        });
    }

    private static Grant outcome(boolean changed, boolean give) {
        if (!changed) {
            return Grant.UNCHANGED;
        }
        return give ? Grant.GRANTED : Grant.REVOKED;
    }

    public String selected(Player player) {
        PlayerProfile profile = plugin.profiles().profile(player);
        String effect = profile == null ? null : profile.killEffect();
        return effect == null || isUnknown(effect) ? NONE : effect;
    }

    public boolean select(Player player, String id) {
        PlayerProfile profile = plugin.profiles().profile(player);
        String normalized = id.toLowerCase(Locale.ROOT);
        if (profile == null || isUnknown(normalized)) {
            return false;
        }
        profile.killEffect(NONE.equals(normalized) ? null : normalized);
        return true;
    }

    public boolean hidesEffects(Player player) {
        PlayerProfile profile = plugin.profiles().profile(player);
        return profile != null && profile.hidesEffects();
    }

    public void play(Player killer, Player victim, Location at) {
        if (killer == null || at == null || at.getWorld() == null || active.size() >= maxActive) {
            return;
        }
        String id = selected(killer);
        if (NONE.equals(id) || isLocked(killer, id)) {
            return;
        }
        Configured configured = RANDOM.equals(id) ? pick(killer) : find(id);
        if (configured == null) {
            return;
        }
        Location origin = safeOrigin(at);
        Corpse corpse = victim == null ? null : Corpse.of(victim);
        double load = active.size() >= reduceAbove ? reducedDensity : 1.0D;
        plugin.scheduler().region(origin, () -> start(configured, origin, killer, corpse, load));
    }

    private Configured pick(Player killer) {
        List<Configured> unlocked = new ArrayList<>();
        for (Configured configured : enabled.values()) {
            if (!isLocked(killer, configured.effect().id())) {
                unlocked.add(configured);
            }
        }
        return unlocked.isEmpty() ? null : unlocked.get(ThreadLocalRandom.current().nextInt(unlocked.size()));
    }

    private static Location safeOrigin(Location at) {
        World world = at.getWorld();
        double y = Math.max(world.getMinHeight() + 1.0D, Math.min(world.getMaxHeight() - 4.0D, at.getY()));
        return new Location(world, at.getX(), y, at.getZ());
    }

    private void start(Configured configured, Location origin, Player killer, Corpse corpse, double load) {
        World world = origin.getWorld();
        if (world == null || !world.isChunkLoaded(origin.getBlockX() >> 4, origin.getBlockZ() >> 4)
                || active.size() >= maxActive) {
            return;
        }
        List<Player> viewers = new ArrayList<>();
        List<Player> hidden = new ArrayList<>();
        PlayerProfile killerProfile = plugin.profiles().profile(killer);
        boolean onlyKiller = killerProfile != null && killerProfile.privateEffects();
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(origin) > viewDistanceSquared) {
                continue;
            }
            if (player.equals(killer)) {
                viewers.add(player);
            } else if (onlyKiller || hidesEffects(player)) {
                hidden.add(player);
            } else {
                viewers.add(player);
            }
        }
        if (viewers.isEmpty()) {
            return;
        }
        EffectSession.Settings settings = new EffectSession.Settings(density * configured.intensity() * load,
                configured.duration(), configured.volume(), particleBudget, maxTicks);
        EffectSession[] holder = new EffectSession[1];
        holder[0] = new EffectSession(plugin, plugin.scheduler(), origin, killer, corpse, viewers, hidden, settings,
                () -> active.remove(holder[0]));
        active.add(holder[0]);
        try {
            configured.effect().play(holder[0]);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "The kill effect " + configured.effect().id() + " failed to start", ex);
            holder[0].finish();
            return;
        }
        holder[0].start();
    }

    public void worldUnloaded(World world) {
        for (EffectSession session : List.copyOf(active)) {
            if (session.world().equals(world)) {
                session.finish();
            }
        }
    }

    public void open(Player player, int page) {
        plugin.scheduler().entity(player, () -> new KillEffectMenu(plugin, player, page).open(player));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void shutdown() {
        for (EffectSession session : List.copyOf(active)) {
            session.finish();
        }
        active.clear();
    }
}
