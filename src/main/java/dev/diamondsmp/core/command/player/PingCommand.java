package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class PingCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.ping.others";

    public PingCommand(DiamondSMP plugin) {
        super(plugin, "ping", "diamondsmp.ping", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.targetOrSelf(0, OTHERS);
        if (target.equals(context.sender())) {
            context.reply("ping.self", "ping", target.getPing());
        } else {
            context.reply("ping.other", "player", target.getName(), "ping", target.getPing());
        }
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
