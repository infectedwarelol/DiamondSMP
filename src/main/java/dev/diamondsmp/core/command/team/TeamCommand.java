package dev.diamondsmp.core.command.team;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.TeamService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TeamCommand extends BaseCommand {

    private static final List<String> ACTIONS = List.of(
            "create", "disband", "invite", "accept", "deny", "leave", "kick", "rename", "color", "pvp", "info");

    public TeamCommand(DiamondSMP plugin) {
        super(plugin, "team", "diamondsmp.team", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        String action = context.arg(0, "info").toLowerCase(Locale.ROOT);
        switch (action) {
            case "create" -> create(context, player);
            case "disband" -> disband(context, player);
            case "invite" -> invite(context, player);
            case "accept" -> accept(context, player);
            case "deny" -> deny(context, player);
            case "leave" -> leave(context, player);
            case "kick" -> kick(context, player);
            case "rename" -> rename(context, player);
            case "color" -> color(context, player);
            case "pvp" -> pvp(context, player);
            case "info" -> info(context, player);
            default -> throw context.usage();
        }
    }

    private TeamService teams() {
        return plugin.teams();
    }

    private TeamService.Team ownTeam(CommandContext context, Player player) {
        TeamService.Team team = teams().teamOf(player.getUniqueId());
        if (team == null) {
            throw context.fail("teams.not-in-team");
        }
        return team;
    }

    private TeamService.Team ownedTeam(CommandContext context, Player player) {
        TeamService.Team team = ownTeam(context, player);
        if (!team.isOwner(player.getUniqueId())) {
            throw context.fail("teams.not-owner");
        }
        return team;
    }

    private Component tag(TeamService.Team team) {
        return Component.text(team.name(), team.color());
    }

    private void create(CommandContext context, Player player) {
        String name = context.arg(1);
        switch (teams().create(player, name)) {
            case IN_TEAM -> throw context.fail("teams.already-in-team");
            case INVALID_NAME -> throw context.fail("teams.invalid-name", "name", name,
                    "max", plugin.settings().getInt("teams.max-name-length", 10));
            case TAKEN -> throw context.fail("teams.taken", "name", name);
            case CREATED -> context.reply("teams.created", "team", tag(teams().teamOf(player.getUniqueId())));
        }
    }

    private void disband(CommandContext context, Player player) {
        TeamService.Team team = ownedTeam(context, player);
        teams().sendToTeam(team, "teams.disbanded", "team", tag(team));
        teams().disband(team);
    }

    private void invite(CommandContext context, Player player) {
        TeamService.Team team = ownedTeam(context, player);
        Player target = context.online(1);
        if (target.equals(player)) {
            throw context.fail("teams.invite-self");
        }
        if (teams().teamOf(target.getUniqueId()) != null) {
            throw context.fail("teams.target-in-team", "player", target.getName());
        }
        if (team.members().size() >= teams().maxMembers()) {
            throw context.fail("teams.full", "max", teams().maxMembers());
        }
        teams().invite(team, target);
        context.reply("teams.invited", "player", target.getName(), "team", tag(team));
        plugin.messages().send(target, "teams.invite-received", "player", player.getName(), "team", tag(team));
    }

    private void accept(CommandContext context, Player player) {
        TeamService.Team team = invitedTeam(context, player);
        if (teams().teamOf(player.getUniqueId()) != null) {
            throw context.fail("teams.already-in-team");
        }
        if (!teams().join(player, team)) {
            throw context.fail("teams.full", "max", teams().maxMembers());
        }
        teams().sendToTeam(team, "teams.joined", "player", player.getName(), "team", tag(team));
    }

    private void deny(CommandContext context, Player player) {
        TeamService.Team team = invitedTeam(context, player);
        teams().clearInvite(player.getUniqueId(), team);
        context.reply("teams.denied", "team", tag(team));
        Player owner = Bukkit.getPlayer(team.owner());
        if (owner != null) {
            plugin.messages().send(owner, "teams.denied-owner", "player", player.getName());
        }
    }

    private TeamService.Team invitedTeam(CommandContext context, Player player) {
        List<String> pending = teams().invitesOf(player.getUniqueId());
        if (pending.isEmpty()) {
            throw context.fail("teams.no-invite");
        }
        String name = context.has(1) ? context.arg(1) : pending.get(pending.size() - 1);
        TeamService.Team team = teams().find(name);
        if (team == null || !teams().hasInvite(player.getUniqueId(), team)) {
            throw context.fail("teams.no-invite");
        }
        return team;
    }

    private void leave(CommandContext context, Player player) {
        TeamService.Team team = ownTeam(context, player);
        if (team.isOwner(player.getUniqueId())) {
            throw context.fail("teams.owner-leave");
        }
        teams().remove(team, player.getUniqueId());
        context.reply("teams.left", "team", tag(team));
        teams().sendToTeam(team, "teams.member-left", "player", player.getName());
    }

    private void kick(CommandContext context, Player player) {
        TeamService.Team team = ownedTeam(context, player);
        String name = context.arg(1);
        UUID member = teams().memberByName(team, name);
        if (member == null) {
            throw context.fail("teams.not-member", "player", name);
        }
        if (team.isOwner(member)) {
            throw context.fail("teams.kick-self");
        }
        teams().remove(team, member);
        teams().sendToTeam(team, "teams.kicked", "player", name);
        Player kicked = Bukkit.getPlayer(member);
        if (kicked != null) {
            plugin.messages().send(kicked, "teams.kicked-target", "team", tag(team));
        }
    }

    private void rename(CommandContext context, Player player) {
        TeamService.Team team = ownedTeam(context, player);
        String name = context.arg(1);
        if (!teams().isValidName(name)) {
            throw context.fail("teams.invalid-name", "name", name,
                    "max", plugin.settings().getInt("teams.max-name-length", 10));
        }
        if (!name.equalsIgnoreCase(team.name()) && teams().isTaken(name)) {
            throw context.fail("teams.taken", "name", name);
        }
        teams().rename(team, name);
        teams().sendToTeam(team, "teams.renamed", "team", tag(team));
    }

    private void color(CommandContext context, Player player) {
        TeamService.Team team = ownedTeam(context, player);
        String raw = context.arg(1);
        NamedTextColor color = TeamService.color(raw);
        if (color == null) {
            throw context.fail("teams.invalid-color", "color", raw);
        }
        teams().color(team, color);
        teams().sendToTeam(team, "teams.recolored", "team", tag(team));
    }

    private void pvp(CommandContext context, Player player) {
        TeamService.Team team = ownedTeam(context, player);
        String raw = context.arg(1, team.pvp() ? "off" : "on").toLowerCase(Locale.ROOT);
        if (!raw.equals("on") && !raw.equals("off")) {
            throw context.usage();
        }
        teams().pvp(team, raw.equals("on"));
        teams().sendToTeam(team, team.pvp() ? "teams.pvp-on" : "teams.pvp-off");
    }

    private void info(CommandContext context, Player player) {
        TeamService.Team team = context.has(1) ? teams().find(context.arg(1)) : teams().teamOf(player.getUniqueId());
        if (team == null) {
            throw context.fail(context.has(1) ? "teams.unknown" : "teams.not-in-team", "name", context.arg(1, ""));
        }
        String owner = teams().nameOf(team.owner());
        context.reply("teams.info", "team", tag(team),
                "owner", owner == null ? "?" : owner,
                "size", team.members().size(), "max", teams().maxMembers(),
                "members", String.join(", ", teams().memberNames(team)),
                "pvp", plugin.messages().get(team.pvp() ? "general.enabled" : "general.disabled"));
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            return matching(context, 0, ACTIONS);
        }
        if (context.size() != 2 || !context.isPlayer()) {
            return List.of();
        }
        Player player = context.player();
        TeamService.Team team = teams().teamOf(player.getUniqueId());
        return switch (context.arg(0).toLowerCase(Locale.ROOT)) {
            case "invite" -> players(context, 1);
            case "kick" -> team == null ? List.of() : matching(context, 1, teams().memberNames(team));
            case "accept", "deny" -> matching(context, 1, teams().invitesOf(player.getUniqueId()));
            case "color" -> matching(context, 1, TeamService.colorNames());
            case "pvp" -> matching(context, 1, List.of("on", "off"));
            case "info" -> matching(context, 1, teams().teamNames());
            default -> List.of();
        };
    }
}
