package dev.diamondsmp.core.command.stats;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.storage.PlayerProfile;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import dev.diamondsmp.core.util.Format;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public final class StatsCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.stats.others";

    public StatsCommand(DiamondSMP plugin) {
        super(plugin, "stats", "diamondsmp.stats", false);
    }

    @Override
    protected void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (!context.has(0)) {
            PlayerProfile profile = plugin.profiles().profile(context.player());
            if (profile == null) {
                throw context.fail("general.data-loading");
            }
            show(sender, profile.snapshot());
            return;
        }
        if (!sender.hasPermission(OTHERS)) {
            throw context.fail("general.no-permission");
        }
        String name = context.arg(0);
        plugin.profiles().lookup(name).thenAccept(found -> {
            if (found.isEmpty()) {
                plugin.messages().send(sender, "stats.not-found", "player", name);
                return;
            }
            show(sender, found.get());
        });
    }

    private void show(CommandSender sender, ProfileSnapshot profile) {
        plugin.messages().send(sender, "stats.header", "player", profile.name());
        plugin.messages().send(sender, "stats.kills", "kills", profile.kills());
        plugin.messages().send(sender, "stats.deaths", "deaths", profile.deaths());
        plugin.messages().send(sender, "stats.ratio", "ratio", Format.decimal(profile.ratio()));
        plugin.messages().send(sender, "stats.streak", "streak", profile.streak(), "best", profile.bestStreak());
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
