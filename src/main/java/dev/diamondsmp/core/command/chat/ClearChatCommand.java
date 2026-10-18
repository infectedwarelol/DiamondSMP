package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;

public final class ClearChatCommand extends BaseCommand {

    public ClearChatCommand(DiamondSMP plugin) {
        super(plugin, "clearchat", "diamondsmp.clearchat", false);
    }

    @Override
    protected void execute(CommandContext context) {
        plugin.chat().clearChat();
        plugin.messages().broadcast("chat.cleared", "player", context.sender().getName());
    }
}
