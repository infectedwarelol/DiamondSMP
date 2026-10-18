package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

public final class ReplyCommand extends BaseCommand {

    public ReplyCommand(DiamondSMP plugin) {
        super(plugin, "reply", "diamondsmp.reply", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        Player target = plugin.chat().replyTarget(player);
        if (target == null || !plugin.visibility().canSee(player, target)) {
            throw context.fail("msg.no-reply");
        }
        MsgCommand.deliver(plugin, context, target, context.joined(0));
    }
}
