package dev.diamondsmp.core.command.stats;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.gui.LeaderboardBoardMenu;
import dev.diamondsmp.core.gui.LeaderboardMenu;
import dev.diamondsmp.core.gui.LeaderboardSection;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class LeaderboardCommand extends BaseCommand {

    private static final int CHAT_SIZE = 10;

    public LeaderboardCommand(DiamondSMP plugin) {
        super(plugin, "leaderboard", "diamondsmp.leaderboard", false);
    }

    @Override
    protected void execute(CommandContext context) {
        LeaderboardSection section = context.has(0) ? LeaderboardSection.parse(context.arg(0)) : null;
        if (context.has(0) && section == null) {
            throw context.usage();
        }
        if (context.isPlayer()) {
            Player player = context.player();
            if (section == null) {
                plugin.scheduler().entity(player, () -> new LeaderboardMenu(plugin).open(player));
            } else {
                LeaderboardBoardMenu.open(plugin, player, section, 0);
            }
            return;
        }
        LeaderboardSection shown = section == null ? LeaderboardSection.KILLS : section;
        CommandSender sender = context.sender();
        plugin.profiles().top(shown.type(), CHAT_SIZE).thenAccept(entries -> {
            plugin.messages().send(sender, "leaderboard.header",
                    "type", plugin.messages().get("menus.leaderboard.sections." + shown.id() + ".title"));
            if (entries.isEmpty()) {
                plugin.messages().send(sender, "leaderboard.empty");
                return;
            }
            int rank = 1;
            for (ProfileSnapshot entry : entries) {
                plugin.messages().send(sender, "leaderboard.entry", "rank", rank++, "player", entry.name(),
                        "value", shown.value(entry));
            }
        });
    }

    @Override
    protected List<String> complete(CommandContext context) {
        List<String> sections = new ArrayList<>();
        for (LeaderboardSection section : LeaderboardSection.values()) {
            sections.add(section.id());
        }
        return matching(context, 0, sections);
    }
}
