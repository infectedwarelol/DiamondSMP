package dev.diamondsmp.core.command.teleport;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public final class TpHereCommand extends BaseCommand {

    public TpHereCommand(DiamondSMP plugin) {
        super(plugin, "tphere", "diamondsmp.tphere", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        Player target = context.online(0);
        if (target.equals(player)) {
            throw context.fail("teleport.self");
        }
        Location destination = player.getLocation();
        plugin.teleports().teleport(target, destination).thenAccept(success -> {
            if (!success) {
                plugin.messages().send(player, "teleport.failed");
                return;
            }
            plugin.messages().send(player, "teleport.here", "player", target.getName());
            plugin.messages().send(target, "teleport.to-player", "player", player.getName());
        });
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return players(context, 0);
    }
}
