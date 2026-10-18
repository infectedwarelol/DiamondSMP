package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;

public final class LinkCommand extends BaseCommand {

    public LinkCommand(DiamondSMP plugin, String name) {
        super(plugin, name, "diamondsmp." + name, false);
    }

    @Override
    protected void execute(CommandContext context) {
        context.reply("links." + name());
    }
}
