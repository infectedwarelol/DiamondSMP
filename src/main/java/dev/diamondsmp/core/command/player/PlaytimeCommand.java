package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.ProfileService;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class PlaytimeCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.playtime.others";

    public PlaytimeCommand(DiamondSMP plugin) {
        super(plugin, "playtime", "diamondsmp.playtime", false);
    }

    @Override
    protected void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (!context.has(0)) {
            Player player = context.player();
            context.reply("playtime.self", "time", Format.duration(ProfileService.playtime(player) / 1000L));
            return;
        }
        if (!sender.hasPermission(OTHERS)) {
            throw context.fail("general.no-permission");
        }
        String name = context.arg(0);
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && plugin.visibility().canSee(sender, online)) {
            plugin.scheduler().entity(online, () -> plugin.messages().send(sender, "playtime.other",
                    "player", online.getName(), "time", Format.duration(ProfileService.playtime(online) / 1000L)));
            return;
        }
        plugin.profiles().lookup(name).thenAccept(found -> {
            if (found.isEmpty()) {
                plugin.messages().send(sender, "playtime.not-found", "player", name);
                return;
            }
            plugin.messages().send(sender, "playtime.other", "player", found.get().name(),
                    "time", Format.duration(found.get().playtime() / 1000L));
        });
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
