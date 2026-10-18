package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ListCommand extends BaseCommand {

    public ListCommand(DiamondSMP plugin) {
        super(plugin, "list", "diamondsmp.list", false);
    }

    @Override
    protected void execute(CommandContext context) {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.visibility().canSee(context.sender(), online)) {
                names.add(plugin.visibility().isVanished(online) ? online.getName() + "*" : online.getName());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        context.reply("list.header", "count", names.size(), "max", Bukkit.getMaxPlayers());
        if (!names.isEmpty()) {
            context.reply("list.players", "players", String.join(", ", names));
        }
    }
}
