package dev.diamondsmp.core.command.warp;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;

import java.util.List;

public final class DelWarpCommand extends BaseCommand {

    public DelWarpCommand(DiamondSMP plugin) {
        super(plugin, "delwarp", "diamondsmp.delwarp", false);
    }

    @Override
    protected void execute(CommandContext context) {
        String name = context.arg(0);
        context.reply(plugin.warps().delete(name) ? "warp.deleted" : "warp.not-found", "warp", name);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return matching(context, 0, plugin.warps().names(null));
    }
}
