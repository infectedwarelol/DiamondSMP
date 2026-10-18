package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;

public final class MuteChatCommand extends BaseCommand {

    public MuteChatCommand(DiamondSMP plugin) {
        super(plugin, "mutechat", "diamondsmp.mutechat", false);
    }

    @Override
    protected void execute(CommandContext context) {
        boolean muted = plugin.chat().toggleMute();
        plugin.messages().broadcast(muted ? "chat.muted-broadcast" : "chat.unmuted-broadcast",
                "player", context.sender().getName());
    }
}
