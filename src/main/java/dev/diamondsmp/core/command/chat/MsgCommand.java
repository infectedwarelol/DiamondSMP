package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.ChatService;
import org.bukkit.entity.Player;

import java.util.List;

public final class MsgCommand extends BaseCommand {

    public MsgCommand(DiamondSMP plugin) {
        super(plugin, "msg", "diamondsmp.msg", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.online(0);
        String message = context.joined(1);
        deliver(plugin, context, target, message);
    }

    static void deliver(DiamondSMP plugin, CommandContext context, Player target, String message) {
        if (message.isEmpty()) {
            throw context.usage();
        }
        ChatService.Delivery delivery = plugin.chat().sendPrivate(context.sender(), target, message);
        switch (delivery) {
            case SELF -> context.reply("msg.self");
            case IGNORED -> context.reply("msg.ignored", "player", target.getName());
            case BLOCKED -> context.reply("msg.blocked", "player", target.getName());
            case BLOCKED_BY_YOU -> context.reply("msg.blocked-by-you", "player", target.getName());
            case SENT -> {
            }
        }
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return players(context, 0);
    }
}
