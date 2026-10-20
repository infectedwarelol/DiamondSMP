package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.storage.TeamRecord;
import dev.diamondsmp.core.util.Format;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class TeamService implements Service {

    private static final Map<NamedTextColor, Character> LEGACY_CODES = Map.ofEntries(
            Map.entry(NamedTextColor.BLACK, '0'), Map.entry(NamedTextColor.DARK_BLUE, '1'),
            Map.entry(NamedTextColor.DARK_GREEN, '2'), Map.entry(NamedTextColor.DARK_AQUA, '3'),
            Map.entry(NamedTextColor.DARK_RED, '4'), Map.entry(NamedTextColor.DARK_PURPLE, '5'),
            Map.entry(NamedTextColor.GOLD, '6'), Map.entry(NamedTextColor.GRAY, '7'),
            Map.entry(NamedTextColor.DARK_GRAY, '8'), Map.entry(NamedTextColor.BLUE, '9'),
            Map.entry(NamedTextColor.GREEN, 'a'), Map.entry(NamedTextColor.AQUA, 'b'),
            Map.entry(NamedTextColor.RED, 'c'), Map.entry(NamedTextColor.LIGHT_PURPLE, 'd'),
            Map.entry(NamedTextColor.YELLOW, 'e'), Map.entry(NamedTextColor.WHITE, 'f'));

    public static final class Team {

        private final UUID owner;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private volatile String name;
        private volatile NamedTextColor color;
        private volatile boolean pvp;

        private Team(String name, UUID owner, NamedTextColor color, boolean pvp, Collection<UUID> members) {
            this.name = name;
            this.owner = owner;
            this.color = color;
            this.pvp = pvp;
            this.members.add(owner);
            this.members.addAll(members);
        }

        public String name() {
            return name;
        }

        public UUID owner() {
            return owner;
        }

        public NamedTextColor color() {
            return color;
        }

        public boolean pvp() {
            return pvp;
        }

        public Set<UUID> members() {
            return Collections.unmodifiableSet(members);
        }

        public boolean isOwner(UUID uuid) {
            return owner.equals(uuid);
        }

        public String legacyColor() {
            return "&" + LEGACY_CODES.getOrDefault(color, 'f');
        }

        private TeamRecord record() {
            return new TeamRecord(name, owner, color.toString(), pvp, members);
        }
    }

    public enum CreateResult {
        CREATED,
        INVALID_NAME,
        TAKEN,
        IN_TEAM
    }

    private final DiamondSMP plugin;
    private final Map<String, Team> teams = new ConcurrentHashMap<>();
    private final Map<UUID, Team> memberships = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> invites = new ConcurrentHashMap<>();

    private volatile Pattern namePattern = Pattern.compile("[A-Za-z0-9_]{1,10}");
    private volatile int maxMembers = 8;
    private volatile long inviteMillis = 60_000L;
    private volatile NamedTextColor defaultColor = NamedTextColor.AQUA;

    public TeamService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> load() {
        return plugin.database().supply("load the teams", storage -> storage.loadTeams()).thenAccept(records -> {
            for (TeamRecord record : records) {
                NamedTextColor color = color(record.color());
                Team team = new Team(record.name(), record.owner(), color == null ? defaultColor : color, record.pvp(),
                        record.members());
                if (teams.putIfAbsent(key(team.name()), team) == null) {
                    for (UUID member : team.members()) {
                        memberships.putIfAbsent(member, team);
                    }
                }
            }
        });
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        int length = Math.max(1, Math.min(32, config.getInt("teams.max-name-length", 10)));
        namePattern = Pattern.compile("[A-Za-z0-9_]{1," + length + "}");
        maxMembers = Math.max(2, config.getInt("teams.max-members", 8));
        inviteMillis = Math.max(10L, Format.parseSeconds(config.getString("teams.invite-expire"), 60L)) * 1000L;
        NamedTextColor configured = color(config.getString("teams.default-color", "aqua"));
        defaultColor = configured == null ? NamedTextColor.AQUA : configured;
    }

    public int maxMembers() {
        return maxMembers;
    }

    public Team teamOf(UUID uuid) {
        return memberships.get(uuid);
    }

    public Team find(String name) {
        return name == null ? null : teams.get(key(name));
    }

    public List<String> teamNames() {
        List<String> names = new ArrayList<>(teams.size());
        for (Team team : teams.values()) {
            names.add(team.name());
        }
        return names;
    }

    public boolean protects(Player first, Player second) {
        Team team = memberships.get(first.getUniqueId());
        return team != null && !team.pvp() && team.members.contains(second.getUniqueId());
    }

    public synchronized CreateResult create(Player owner, String name) {
        if (memberships.containsKey(owner.getUniqueId())) {
            return CreateResult.IN_TEAM;
        }
        if (!namePattern.matcher(name).matches()) {
            return CreateResult.INVALID_NAME;
        }
        if (teams.containsKey(key(name))) {
            return CreateResult.TAKEN;
        }
        Team team = new Team(name, owner.getUniqueId(), defaultColor, false, List.of());
        teams.put(key(name), team);
        memberships.put(owner.getUniqueId(), team);
        save(team);
        return CreateResult.CREATED;
    }

    public synchronized void disband(Team team) {
        teams.remove(key(team.name()), team);
        for (UUID member : team.members) {
            memberships.remove(member, team);
        }
        for (Map<String, Long> pending : invites.values()) {
            pending.remove(key(team.name()));
        }
        String name = team.name();
        plugin.database().run("delete the team " + name, storage -> storage.deleteTeam(name));
    }

    public void invite(Team team, Player target) {
        invites.computeIfAbsent(target.getUniqueId(), uuid -> new ConcurrentHashMap<>())
                .put(key(team.name()), System.currentTimeMillis() + inviteMillis);
    }

    public boolean hasInvite(UUID uuid, Team team) {
        Map<String, Long> pending = invites.get(uuid);
        Long until = pending == null ? null : pending.get(key(team.name()));
        return until != null && until > System.currentTimeMillis();
    }

    public List<String> invitesOf(UUID uuid) {
        Map<String, Long> pending = invites.get(uuid);
        List<String> names = new ArrayList<>();
        if (pending == null) {
            return names;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : pending.entrySet()) {
            Team team = teams.get(entry.getKey());
            if (team != null && entry.getValue() > now) {
                names.add(team.name());
            }
        }
        return names;
    }

    public void clearInvite(UUID uuid, Team team) {
        Map<String, Long> pending = invites.get(uuid);
        if (pending != null) {
            pending.remove(key(team.name()));
        }
    }

    public synchronized boolean join(Player player, Team team) {
        if (memberships.containsKey(player.getUniqueId()) || team.members.size() >= maxMembers
                || teams.get(key(team.name())) != team) {
            return false;
        }
        team.members.add(player.getUniqueId());
        memberships.put(player.getUniqueId(), team);
        invites.remove(player.getUniqueId());
        save(team);
        return true;
    }

    public synchronized boolean remove(Team team, UUID member) {
        if (team.isOwner(member) || !team.members.remove(member)) {
            return false;
        }
        memberships.remove(member, team);
        save(team);
        return true;
    }

    public synchronized boolean rename(Team team, String name) {
        if (!namePattern.matcher(name).matches()) {
            return false;
        }
        Team existing = teams.get(key(name));
        if (existing != null && existing != team) {
            return false;
        }
        String previous = team.name();
        teams.remove(key(previous), team);
        team.name = name;
        teams.put(key(name), team);
        plugin.database().run("rename the team " + previous, storage -> {
            storage.deleteTeam(previous);
            storage.saveTeam(team.record());
        });
        return true;
    }

    public boolean isValidName(String name) {
        return namePattern.matcher(name).matches();
    }

    public boolean isTaken(String name) {
        return teams.containsKey(key(name));
    }

    public void color(Team team, NamedTextColor color) {
        team.color = color;
        save(team);
    }

    public void pvp(Team team, boolean enabled) {
        team.pvp = enabled;
        save(team);
    }

    public UUID memberByName(Team team, String name) {
        for (UUID member : team.members) {
            Player online = Bukkit.getPlayer(member);
            String known = online != null ? online.getName() : Bukkit.getOfflinePlayer(member).getName();
            if (name.equalsIgnoreCase(known)) {
                return member;
            }
        }
        return null;
    }

    public List<String> memberNames(Team team) {
        List<String> names = new ArrayList<>();
        for (UUID member : team.members) {
            Player online = Bukkit.getPlayer(member);
            OfflinePlayer offline = online != null ? online : Bukkit.getOfflinePlayer(member);
            names.add(offline.getName() == null ? member.toString().substring(0, 8) : offline.getName());
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void sendToTeam(Team team, String key, Object... placeholders) {
        for (UUID member : team.members) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                plugin.messages().send(online, key, placeholders);
            }
        }
    }

    public static NamedTextColor color(String name) {
        return name == null ? null : NamedTextColor.NAMES.value(name.trim().toLowerCase(Locale.ROOT));
    }

    public static List<String> colorNames() {
        return new ArrayList<>(NamedTextColor.NAMES.keys());
    }

    private void save(Team team) {
        TeamRecord record = team.record();
        plugin.database().run("save the team " + record.name(), storage -> storage.saveTeam(record));
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public void shutdown() {
        invites.clear();
    }
}
