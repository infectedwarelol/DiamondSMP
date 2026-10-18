package dev.diamondsmp.core.command.screenshare;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.ScreenShareSession;

import java.util.List;

public final class UnfreezeCommand extends BaseCommand {

    public UnfreezeCommand(DiamondSMP plugin) {
        super(plugin, "unfreeze", "diamondsmp.screenshare.unfreeze", false);
    }

    @Override
    protected void execute(CommandContext context) {
        if (context.size() != 1) {
            throw context.usage();
        }
        ScreenShareSession session = plugin.screenShare().session(context.arg(0));
        if (session == null) {
            throw context.fail("screenshare.unfreeze.not-frozen");
        }
        if (!plugin.screenShare().canControl(context.sender(), session)) {
            throw context.fail("screenshare.unfreeze.not-owner", plugin.screenShare().placeholders(session));
        }
        plugin.screenShare().unfreeze(context.sender(), session);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return matching(context, 0, plugin.screenShare().targetNames());
    }
}
