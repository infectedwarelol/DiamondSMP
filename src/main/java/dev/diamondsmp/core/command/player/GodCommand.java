package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class GodCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.god.others";

    public GodCommand(DiamondSMP plugin) {
        super(plugin, "god", "diamondsmp.god", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.targetOrSelf(0, OTHERS);
        applyTo(context, target, player -> state(plugin.god().toggle(player)), "god.self", "god.other", "god.notify");
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
