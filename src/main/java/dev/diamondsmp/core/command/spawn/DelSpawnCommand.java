package dev.diamondsmp.core.command.spawn;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;

public final class DelSpawnCommand extends BaseCommand {

    public DelSpawnCommand(DiamondSMP plugin) {
        super(plugin, "delspawn", "diamondsmp.delspawn", false);
    }

    @Override
    protected void execute(CommandContext context) {
        context.reply(plugin.spawn().delete() ? "spawn.deleted" : "spawn.not-set");
    }
}
