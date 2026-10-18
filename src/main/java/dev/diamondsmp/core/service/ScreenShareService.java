package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.message.Messages;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.util.Format;
import dev.diamondsmp.core.util.SoundSpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class ScreenShareService implements Service {

    public static final String NOTIFY_PERMISSION = "diamondsmp.screenshare.notify";
    public static final String UNFREEZE_OTHERS_PERMISSION = "diamondsmp.screenshare.unfreeze.others";
    public static final String BYPASS_PERMISSION = "diamondsmp.screenshare.bypass";
    private static final double STAFF_RETURN_RANGE_SQUARED = 16.0D * 16.0D;
    private static final Pattern COMMAND_UNSAFE = Pattern.compile("[^A-Za-z0-9_.:#*-]");

    private final DiamondSMP plugin;
    private final Map<UUID, ScreenShareSession> sessions = new ConcurrentHashMap<>();
    private final FreezeVisuals visuals;
    private Task ticker = Task.NONE;

    private volatile long voiceMillis = 120_000L;
    private volatile boolean actionBar = true;
    private volatile double staffDistance = 1.5D;
    private volatile boolean returnOnUnfreeze;
    private volatile boolean unfreezeOnStaffLeave;
    private volatile Title.Times titleTimes = Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5),
            Duration.ofSeconds(1));
    private volatile Set<PlayerQuitEvent.QuitReason> leaveReasons = Set.of();
    private volatile Set<PlayerKickEvent.Cause> excusedKicks = Set.of();
    private volatile List<String> leaveCommands = List.of();
    private volatile boolean invulnerable = true;
    private volatile Set<String> allowedCommands = Set.of();
    private volatile Map<String, SoundSpec> sounds = Map.of();

    public ScreenShareService(DiamondSMP plugin) {
        this.plugin = plugin;
        this.visuals = new FreezeVisuals(plugin);
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        voiceMillis = Math.max(0L, Format.parseSeconds(config.getString("screenshare.voice-channel-time"), 120L)) * 1000L;
        actionBar = config.getBoolean("screenshare.action-bar", true);
        staffDistance = Math.max(0.0D, Math.min(8.0D, config.getDouble("screenshare.staff-distance", 1.5D)));
        returnOnUnfreeze = config.getBoolean("screenshare.return-on-unfreeze", false);
        unfreezeOnStaffLeave = config.getBoolean("screenshare.unfreeze-on-staff-leave", false);
        titleTimes = Title.Times.times(ticks(config.getInt("screenshare.titles.fade-in-ticks", 10)),
                ticks(config.getInt("screenshare.titles.stay-ticks", 100)),
                ticks(config.getInt("screenshare.titles.fade-out-ticks", 20)));
        leaveReasons = enums(config.getStringList("screenshare.punishments.leave-reasons"),
                PlayerQuitEvent.QuitReason.class);
        excusedKicks = enums(config.getStringList("screenshare.punishments.excused-kick-causes"),
                PlayerKickEvent.Cause.class);
        List<String> commands = new ArrayList<>();
        for (String command : config.getStringList("screenshare.punishments.leave-commands")) {
            String trimmed = command.startsWith("/") ? command.substring(1) : command;
            if (!trimmed.isBlank()) {
                commands.add(trimmed);
            }
        }
        leaveCommands = List.copyOf(commands);
        invulnerable = config.getBoolean("screenshare.restrictions.invulnerable", true);
        Set<String> allowed = new HashSet<>();
        for (String command : config.getStringList("screenshare.restrictions.allowed-commands")) {
            String label = command.trim().toLowerCase(Locale.ROOT);
            allowed.add(label.startsWith("/") ? label.substring(1) : label);
        }
        allowedCommands = Set.copyOf(allowed);
        Map<String, SoundSpec> parsed = new HashMap<>();
        ConfigurationSection soundSection = config.getConfigurationSection("screenshare.sounds");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                parsed.put(key, SoundSpec.from(soundSection.getConfigurationSection(key)));
            }
        }
        sounds = Map.copyOf(parsed);
        visuals.reload();
        ticker.cancel();
        ticker = Task.NONE;
        for (ScreenShareSession session : sessions.values()) {
            visuals.detach(session);
        }
        updateTicker();
    }

    public ScreenShareSession session(UUID targetId) {
        return sessions.isEmpty() ? null : sessions.get(targetId);
    }

    public ScreenShareSession session(String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        return target == null ? null : session(target.getUniqueId());
    }

    public boolean isFrozen(Player player) {
        return !sessions.isEmpty() && sessions.containsKey(player.getUniqueId());
    }

    public Collection<ScreenShareSession> sessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public List<String> targetNames() {
        List<String> names = new ArrayList<>(sessions.size());
        for (ScreenShareSession session : sessions.values()) {
            names.add(session.targetName());
        }
        return names;
    }

    public boolean allowsCommand(String label) {
        return allowedCommands.contains(label);
    }

    public boolean invulnerable() {
        return invulnerable;
    }

    public void play(Object target, String sound) {
        SoundSpec spec = sounds.get(sound);
        if (spec != null) {
            spec.play(target);
        }
    }

    public FreezePointService.FreezePoint selectPoint() {
        FreezePointService.FreezePoint fallback = null;
        FreezePointService.FreezePoint best = null;
        long bestUsage = Long.MAX_VALUE;
        for (FreezePointService.FreezePoint point : plugin.freezePoints().all()) {
            if (fallback == null) {
                fallback = point;
            }
            if (point.toLocation() == null) {
                continue;
            }
            long usage = 0;
            for (ScreenShareSession session : sessions.values()) {
                if (session.pointId() == point.id()) {
                    usage++;
                }
            }
            if (usage < bestUsage) {
                best = point;
                bestUsage = usage;
                if (usage == 0) {
                    break;
                }
            }
        }
        return best != null ? best : fallback;
    }

    public void freeze(CommandSender staff, Player target, FreezePointService.FreezePoint point, Location anchor) {
        Player staffPlayer = staff instanceof Player player ? player : null;
        UUID staffId = staffPlayer == null ? null : staffPlayer.getUniqueId();
        Location staffOrigin = staffPlayer == null ? null : staffPlayer.getLocation();
        plugin.scheduler().entity(target, () -> {
            long now = System.currentTimeMillis();
            ScreenShareSession session = new ScreenShareSession(target.getUniqueId(), target.getName(), staffId,
                    staff.getName(), point.id(), anchor, target.getLocation(), staffOrigin, now, now + voiceMillis);
            if (sessions.putIfAbsent(target.getUniqueId(), session) != null) {
                plugin.messages().error(staff, "screenshare.freeze.already-frozen");
                return;
            }
            updateTicker();
            Object[] placeholders = placeholders(session);
            Messages messages = plugin.messages();
            messages.send(staff, "screenshare.freeze.success", placeholders);
            messages.send(target, "screenshare.freeze.target", placeholders);
            messages.title(target, "screenshare.title", titleTimes, placeholders);
            notifyStaff("screenshare.freeze.notify", staffId, placeholders);
            play(target, "freeze");
            play(staff, "freeze");
            target.closeInventory();
            if (target.isSleeping()) {
                target.wakeup(false);
            }
            target.leaveVehicle();
            target.eject();
            target.setGliding(false);
            plugin.teleports().teleport(target, anchor, false).thenAccept(success -> arrive(session, success));
        }, () -> plugin.messages().error(staff, "general.player-not-found", "player", target.getName()));
    }

    public boolean canControl(CommandSender sender, ScreenShareSession session) {
        if (!(sender instanceof Player player) || player.hasPermission(UNFREEZE_OTHERS_PERMISSION)) {
            return true;
        }
        UUID staffId = session.staffId();
        return staffId == null || staffId.equals(player.getUniqueId()) || Bukkit.getPlayer(staffId) == null;
    }

    public void unfreeze(CommandSender sender, ScreenShareSession session) {
        Object[] placeholders = withStaff(placeholders(session), sender.getName());
        Player target = Bukkit.getPlayer(session.targetId());
        end(session, true);
        returnStaff(session);
        plugin.messages().send(sender, "screenshare.unfreeze.success", placeholders);
        plugin.messages().send(target, "screenshare.unfreeze.target", placeholders);
        notifyStaff("screenshare.unfreeze.notify", sender instanceof Player player ? player.getUniqueId() : null,
                placeholders);
        play(sender, "unfreeze");
        play(target, "unfreeze");
    }

    public void kicked(Player player, PlayerKickEvent.Cause cause) {
        ScreenShareSession session = session(player.getUniqueId());
        if (session != null) {
            session.kickCause(cause);
        }
    }

    public Location respawnLocation(Player player) {
        ScreenShareSession session = session(player.getUniqueId());
        return session == null ? null : session.anchor();
    }

    public void handleQuit(Player player, PlayerQuitEvent.QuitReason reason) {
        ScreenShareSession session = session(player.getUniqueId());
        if (session != null) {
            handleTargetLeave(session, reason);
        }
        if (!sessions.isEmpty()) {
            handleStaffLeave(player);
        }
    }

    public Object[] placeholders(ScreenShareSession session) {
        String time = clock(session.elapsed());
        UUID staffId = session.staffId();
        return new Object[]{
                "player", session.targetName(),
                "player_uuid", session.targetId(),
                "staff", session.staffName(),
                "staff_uuid", staffId == null ? "console" : staffId,
                "freeze_point", session.pointId(),
                "time", time,
                "duration", time,
                "remaining", clock(session.remaining())
        };
    }

    private void arrive(ScreenShareSession session, boolean success) {
        Player target = Bukkit.getPlayer(session.targetId());
        if (sessions.get(session.targetId()) != session) {
            return;
        }
        if (!success || target == null) {
            end(session, false);
            plugin.messages().error(staff(session), "screenshare.freeze.teleport-failed", placeholders(session));
            return;
        }
        session.markArrived();
        plugin.scheduler().entity(target, () -> {
            if (sessions.get(session.targetId()) == session) {
                visuals.attach(session, target);
            }
        });
        Player staff = session.staffId() == null ? null : Bukkit.getPlayer(session.staffId());
        if (staff != null) {
            Location anchor = session.anchor();
            plugin.scheduler().region(anchor, () -> plugin.teleports().teleport(staff, staffLocation(anchor), false));
        }
    }

    private void handleTargetLeave(ScreenShareSession session, PlayerQuitEvent.QuitReason reason) {
        Object[] placeholders = placeholders(session);
        CommandSender staff = staff(session);
        end(session, false);
        boolean excusedKick = reason == PlayerQuitEvent.QuitReason.KICKED && session.kickCause() != null
                && excusedKicks.contains(session.kickCause());
        if (excusedKick || !leaveReasons.contains(reason)) {
            plugin.messages().send(staff, "screenshare.leave.kicked", placeholders);
            return;
        }
        List<String> commands = new ArrayList<>(leaveCommands.size());
        for (String command : leaveCommands) {
            commands.add(command
                    .replace("%player%", safe(session.targetName()))
                    .replace("%player_uuid%", session.targetId().toString())
                    .replace("%staff%", safe(session.staffName()))
                    .replace("%freeze_point%", String.valueOf(session.pointId())));
        }
        plugin.scheduler().global(() -> {
            for (String command : commands) {
                dispatch(command);
            }
        });
        plugin.messages().send(staff, "screenshare.leave.punished", placeholders);
        notifyStaff("screenshare.leave.notify", session.staffId(), placeholders);
        play(staff, "leave");
    }

    private void handleStaffLeave(Player staff) {
        for (ScreenShareSession session : List.copyOf(sessions.values())) {
            if (!session.isStaff(staff.getUniqueId())) {
                continue;
            }
            Player target = Bukkit.getPlayer(session.targetId());
            if (unfreezeOnStaffLeave) {
                end(session, true);
                plugin.messages().send(target, "screenshare.staff-leave.unfrozen", placeholders(session));
                play(target, "unfreeze");
            } else {
                plugin.messages().send(target, "screenshare.staff-leave.waiting", placeholders(session));
            }
        }
    }

    private void end(ScreenShareSession session, boolean returnTarget) {
        if (!sessions.remove(session.targetId(), session)) {
            return;
        }
        visuals.detach(session);
        Player target = Bukkit.getPlayer(session.targetId());
        if (target != null) {
            boolean sendBack = returnTarget && returnOnUnfreeze;
            plugin.scheduler().entity(target, () -> {
                target.clearTitle();
                target.sendActionBar(Component.empty());
                if (sendBack && session.targetOrigin().isWorldLoaded()) {
                    plugin.teleports().teleport(target, session.targetOrigin(), false);
                }
            });
        }
        updateTicker();
    }

    private void returnStaff(ScreenShareSession session) {
        UUID staffId = session.staffId();
        Location origin = session.staffOrigin();
        if (staffId == null || origin == null || !returnOnUnfreeze || !origin.isWorldLoaded()) {
            return;
        }
        for (ScreenShareSession other : sessions.values()) {
            if (other.isStaff(staffId)) {
                return;
            }
        }
        Player staff = Bukkit.getPlayer(staffId);
        Location anchor = session.anchor();
        if (staff != null && staff.getWorld().equals(anchor.getWorld())
                && staff.getLocation().distanceSquared(anchor) <= STAFF_RETURN_RANGE_SQUARED) {
            plugin.teleports().teleport(staff, origin, false);
        }
    }

    private Location staffLocation(Location anchor) {
        if (staffDistance <= 0.0D) {
            return anchor;
        }
        double yaw = Math.toRadians(anchor.getYaw());
        Location location = anchor.clone().add(-Math.sin(yaw) * staffDistance, 0.0D, Math.cos(yaw) * staffDistance);
        location.setYaw(anchor.getYaw() + 180.0F);
        location.setPitch(0.0F);
        if (!Bukkit.isOwnedByCurrentRegion(location)) {
            return anchor;
        }
        Block feet = location.getBlock();
        return feet.isPassable() && feet.getRelative(BlockFace.UP).isPassable() ? location : anchor;
    }

    private CommandSender staff(ScreenShareSession session) {
        UUID staffId = session.staffId();
        return staffId == null ? Bukkit.getConsoleSender() : Bukkit.getPlayer(staffId);
    }

    private void notifyStaff(String key, UUID except, Object[] placeholders) {
        List<Component> lines = plugin.messages().lines(key, placeholders);
        if (lines.isEmpty()) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(except) && online.hasPermission(NOTIFY_PERMISSION)) {
                for (Component line : lines) {
                    online.sendMessage(line);
                }
            }
        }
        if (except != null) {
            for (Component line : lines) {
                Bukkit.getConsoleSender().sendMessage(line);
            }
        }
    }

    private void dispatch(String command) {
        try {
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                plugin.getLogger().warning("The screenshare punishment command did not run: " + command);
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "The screenshare punishment command failed: " + command, ex);
        }
    }

    private void tick() {
        for (ScreenShareSession session : sessions.values()) {
            Player target = Bukkit.getPlayer(session.targetId());
            if (target != null) {
                plugin.scheduler().entity(target, () -> tick(session, target));
            }
        }
    }

    private void tick(ScreenShareSession session, Player target) {
        if (sessions.get(session.targetId()) != session || !target.isOnline()) {
            return;
        }
        if (session.hasArrived()) {
            visuals.attach(session, target);
        }
        long remaining = session.remaining();
        Object[] placeholders = placeholders(session);
        if (remaining == 0L && voiceMillis > 0L && session.expireVoice()) {
            CommandSender staff = staff(session);
            plugin.messages().send(staff, "screenshare.voice.expired-staff", placeholders);
            plugin.messages().send(target, "screenshare.voice.expired-target", placeholders);
            play(staff, "voice-expired");
        }
        if (actionBar) {
            plugin.messages().actionBar(target,
                    remaining > 0L ? "screenshare.action-bar.waiting" : "screenshare.action-bar.active", placeholders);
        }
    }

    private synchronized void updateTicker() {
        if (sessions.isEmpty()) {
            ticker.cancel();
            ticker = Task.NONE;
            visuals.stop();
            return;
        }
        if (ticker == Task.NONE) {
            ticker = plugin.scheduler().globalTimer(this::tick, 20L, 20L);
        }
        visuals.start();
    }

    private static Object[] withStaff(Object[] placeholders, String staff) {
        Object[] result = new Object[placeholders.length + 2];
        result[0] = "staff";
        result[1] = staff;
        System.arraycopy(placeholders, 0, result, 2, placeholders.length);
        return result;
    }

    private static String safe(String value) {
        return COMMAND_UNSAFE.matcher(value).replaceAll("");
    }

    private static String clock(long millis) {
        long seconds = Math.max(0L, millis) / 1000L;
        long hours = seconds / 3600L;
        long minutes = seconds / 60L % 60L;
        return hours > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds % 60L)
                : String.format(Locale.ROOT, "%02d:%02d", minutes, seconds % 60L);
    }

    private <E extends Enum<E>> Set<E> enums(List<String> names, Class<E> type) {
        Set<E> values = EnumSet.noneOf(type);
        for (String name : names) {
            try {
                values.add(Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("screenshare.punishments: unknown value '" + name + "' for "
                        + type.getSimpleName() + " on this server version.");
            }
        }
        return Collections.unmodifiableSet(values);
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    @Override
    public void shutdown() {
        for (ScreenShareSession session : List.copyOf(sessions.values())) {
            Player target = Bukkit.getPlayer(session.targetId());
            end(session, true);
            plugin.messages().send(target, "screenshare.unfreeze.target", placeholders(session));
        }
        ticker.cancel();
        ticker = Task.NONE;
        visuals.stop();
    }
}
