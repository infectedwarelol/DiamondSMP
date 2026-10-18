package dev.diamondsmp.core.command.stats;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.storage.LeaderboardType;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import dev.diamondsmp.core.util.Format;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public final class LeaderboardCommand extends BaseCommand {

    private static final int SIZE = 10;

    public LeaderboardCommand(DiamondSMP plugin) {
        super(plugin, "leaderboard", "diamondsmp.leaderboard", false);
    }

    @Override
    protected void execute(CommandContext context) {
        LeaderboardType type = context.has(0) ? LeaderboardType.parse(context.arg(0)) : LeaderboardType.KILLS;
        if (type == null) {
            throw context.usage();
        }
        CommandSender sender = context.sender();
        plugin.profiles().top(type, SIZE).thenAccept(entries -> {
            plugin.messages().send(sender, "leaderboard.header",
                    "type", plugin.messages().get("leaderboard.types." + type.id()));
            if (entries.isEmpty()) {
                plugin.messages().send(sender, "leaderboard.empty");
                return;
            }
            int rank = 1;
            for (ProfileSnapshot entry : entries) {
                plugin.messages().send(sender, "leaderboard.entry", "rank", rank++, "player", entry.name(),
                        "value", value(type, entry));
            }
        });
    }

    private static String value(LeaderboardType type, ProfileSnapshot entry) {
        return switch (type) {
            case KILLS -> String.valueOf(entry.kills());
            case CURRENT -> String.valueOf(entry.streak());
            case STREAK -> String.valueOf(entry.bestStreak());
            case KDR -> Format.decimal(entry.ratio());
        };
    }

    @Override
    protected List<String> complete(CommandContext context) {
        List<String> types = new ArrayList<>();
        for (LeaderboardType type : LeaderboardType.values()) {
            types.add(type.id());
        }
        return matching(context, 0, types);
    }
}
