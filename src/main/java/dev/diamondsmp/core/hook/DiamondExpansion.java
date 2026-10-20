package dev.diamondsmp.core.hook;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.service.KillStreakService;
import dev.diamondsmp.core.service.TeamService;
import dev.diamondsmp.core.storage.PlayerProfile;
import dev.diamondsmp.core.util.Format;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

final class DiamondExpansion extends PlaceholderExpansion {

    private final DiamondSMP plugin;
    private final String identifier;

    DiamondExpansion(DiamondSMP plugin, String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Jules";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.version();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String request = params.toLowerCase(Locale.ROOT);
        String board = board(request, "topcurrent_", "top_current_");
        if (board != null) {
            return leaderboard(false, board);
        }
        board = board(request, "tophighest_", "top_highest_");
        if (board != null) {
            return leaderboard(true, board);
        }
        if (player == null) {
            return "";
        }
        if (request.startsWith("team")) {
            return team(plugin.teams().teamOf(player.getUniqueId()), request);
        }
        KillStreakService.Known streak = plugin.killStreaks().lookup(player.getUniqueId());
        PlayerProfile profile = plugin.profiles().profile(player.getUniqueId());
        return switch (request) {
            case "current" -> String.valueOf(streak == null ? 0 : streak.current());
            case "highest" -> String.valueOf(streak == null ? 0 : streak.highest());
            case "kills" -> profile == null ? "0" : String.valueOf(profile.kills());
            case "deaths" -> profile == null ? "0" : String.valueOf(profile.deaths());
            case "kdr" -> profile == null ? "0.00" : Format.decimal(profile.ratio());
            default -> null;
        };
    }

    private String team(TeamService.Team team, String request) {
        if (team == null) {
            return request.equals("team_size") || request.equals("team_members") ? "0" : "";
        }
        return switch (request) {
            case "team", "team_name" -> team.name();
            case "team_upper" -> team.name().toUpperCase(Locale.ROOT);
            case "team_color" -> team.legacyColor();
            case "team_tag" -> plugin.settings().getString("teams.placeholder-tag", " &8| %color%⚑ %name%")
                    .replace("%color%", team.legacyColor())
                    .replace("%name_upper%", team.name().toUpperCase(Locale.ROOT))
                    .replace("%name%", team.name());
            case "team_owner" -> {
                String owner = Bukkit.getOfflinePlayer(team.owner()).getName();
                yield owner == null ? "" : owner;
            }
            case "team_size", "team_members" -> String.valueOf(team.members().size());
            case "team_pvp" -> team.pvp() ? "on" : "off";
            default -> null;
        };
    }

    private static String board(String request, String first, String second) {
        if (request.startsWith(first)) {
            return request.substring(first.length());
        }
        return request.startsWith(second) ? request.substring(second.length()) : null;
    }

    private String leaderboard(boolean highest, String tail) {
        int split = tail.indexOf('_');
        if (split < 1 || split == tail.length() - 1) {
            return "";
        }
        int rank;
        try {
            rank = Integer.parseInt(tail.substring(0, split));
        } catch (NumberFormatException ex) {
            return "";
        }
        String field = tail.substring(split + 1);
        KillStreakService.Entry entry = plugin.killStreaks().top(highest, rank);
        if (entry == null) {
            return switch (field) {
                case "value" -> plugin.killStreaks().emptyValue();
                case "name" -> plugin.killStreaks().emptyName();
                case "uuid" -> "";
                default -> null;
            };
        }
        return switch (field) {
            case "value" -> String.valueOf(entry.value());
            case "name" -> entry.name();
            case "uuid" -> entry.uuid().toString();
            default -> null;
        };
    }
}
