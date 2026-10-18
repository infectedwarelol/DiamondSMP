package dev.diamondsmp.core.command.teleport;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class TpAllCommand extends BaseCommand {

    public TpAllCommand(DiamondSMP plugin) {
        super(plugin, "tpall", "diamondsmp.tpall", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        Location destination = player.getLocation();
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) {
                continue;
            }
            count++;
            plugin.teleports().teleport(online, destination).thenAccept(success -> {
                if (success) {
                    plugin.messages().send(online, "teleport.to-player", "player", player.getName());
                }
            });
        }
        context.reply("teleport.all", "count", count);
    }
}
