package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class FeedCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.feed.others";

    public FeedCommand(DiamondSMP plugin) {
        super(plugin, "feed", "diamondsmp.feed", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.targetOrSelf(0, OTHERS);
        applyTo(context, target, player -> {
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setExhaustion(0.0F);
            return null;
        }, "feed.self", "feed.other", "feed.notify");
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
