package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class SeenCommand extends BaseCommand {

    public SeenCommand(DiamondSMP plugin) {
        super(plugin, "seen", "diamondsmp.seen", false);
    }

    @Override
    protected void execute(CommandContext context) {
        String name = context.arg(0);
        CommandSender sender = context.sender();
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && plugin.visibility().canSee(sender, online)) {
            context.reply("seen.online", "player", online.getName());
            return;
        }
        plugin.profiles().lookup(name).thenAccept(found -> {
            if (found.isEmpty()) {
                plugin.messages().send(sender, "seen.never", "player", name);
                return;
            }
            long seconds = (System.currentTimeMillis() - found.get().lastSeen()) / 1000L;
            plugin.messages().send(sender, "seen.offline", "player", found.get().name(),
                    "time", Format.duration(seconds));
        });
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return players(context, 0);
    }
}
