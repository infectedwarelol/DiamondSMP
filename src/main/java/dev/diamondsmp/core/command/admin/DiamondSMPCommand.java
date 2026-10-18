package dev.diamondsmp.core.command.admin;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;

import java.util.List;
import java.util.Locale;

public final class DiamondSMPCommand extends BaseCommand {

    public DiamondSMPCommand(DiamondSMP plugin) {
        super(plugin, "diamondsmp", "diamondsmp.admin", false);
    }

    @Override
    protected void execute(CommandContext context) {
        String action = context.arg(0, "version").toLowerCase(Locale.ROOT);
        switch (action) {
            case "reload" -> {
                long started = System.nanoTime();
                plugin.reloadAll();
                context.reply("admin.reloaded", "time", (System.nanoTime() - started) / 1_000_000L);
            }
            case "version" -> context.reply("admin.version",
                    "version", plugin.version(),
                    "server", plugin.serverVersion().describe());
            default -> throw context.usage();
        }
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return matching(context, 0, List.of("reload", "version"));
    }
}
