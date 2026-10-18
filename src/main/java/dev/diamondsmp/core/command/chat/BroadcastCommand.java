package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.message.Text;

public final class BroadcastCommand extends BaseCommand {

    public BroadcastCommand(DiamondSMP plugin) {
        super(plugin, "broadcast", "diamondsmp.broadcast", false);
    }

    @Override
    protected void execute(CommandContext context) {
        String message = context.joined(0);
        if (message.isEmpty()) {
            throw context.usage();
        }
        plugin.messages().broadcast("broadcast.format",
                "message", Text.parse(plugin.messages().theme(message)));
    }
}
