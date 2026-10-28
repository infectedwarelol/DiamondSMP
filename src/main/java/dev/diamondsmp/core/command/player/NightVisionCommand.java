package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;

public final class NightVisionCommand extends BaseCommand {

    public NightVisionCommand(DiamondSMP plugin) {
        super(plugin, "nightvision", "diamondsmp.nightvision", true);
    }

    @Override
    protected void execute(CommandContext context) {
        applyTo(context, context.player(), player -> state(plugin.environment().toggleNightVision(player)),
                "nightvision.self", "nightvision.self", null);
    }
}
