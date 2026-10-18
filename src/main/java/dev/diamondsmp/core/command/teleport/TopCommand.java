package dev.diamondsmp.core.command.teleport;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.TeleportKind;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class TopCommand extends BaseCommand {

    public TopCommand(DiamondSMP plugin) {
        super(plugin, "top", "diamondsmp.top", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        plugin.scheduler().entity(player, () -> {
            Location current = player.getLocation();
            Block highest = player.getWorld().getHighestBlockAt(current.getBlockX(), current.getBlockZ());
            if (highest.getY() + 1 <= current.getBlockY()) {
                plugin.messages().error(player, "top.already");
                return;
            }
            Location destination = new Location(player.getWorld(), current.getBlockX() + 0.5D,
                    highest.getY() + 1.0D, current.getBlockZ() + 0.5D, current.getYaw(), current.getPitch());
            plugin.teleports().request(player, TeleportKind.TOP, () -> destination, "top.teleported");
        });
    }
}
