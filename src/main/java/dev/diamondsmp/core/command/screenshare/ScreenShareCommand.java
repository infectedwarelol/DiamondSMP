package dev.diamondsmp.core.command.screenshare;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.ScreenShareService;
import dev.diamondsmp.core.service.ScreenShareSession;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ScreenShareCommand extends BaseCommand {

    private static final Map<String, String> SUBCOMMANDS = new LinkedHashMap<>();

    static {
        SUBCOMMANDS.put("info", "diamondsmp.screenshare.info");
        SUBCOMMANDS.put("list", "diamondsmp.screenshare.list");
        SUBCOMMANDS.put("reload", "diamondsmp.screenshare.reload");
    }

    public ScreenShareCommand(DiamondSMP plugin) {
        super(plugin, "screenshare", null, false);
    }

    @Override
    protected void execute(CommandContext context) {
        CommandSender sender = context.sender();
        String subcommand = context.arg(0, "").toLowerCase(Locale.ROOT);
        String permission = SUBCOMMANDS.get(subcommand);
        if (permission == null) {
            if (!available(sender).isEmpty()) {
                context.reply("screenshare.help", "version", plugin.version());
                return;
            }
            throw context.fail("general.no-permission");
        }
        if (!sender.hasPermission(permission)) {
            throw context.fail("general.no-permission");
        }
        switch (subcommand) {
            case "info" -> info(context);
            case "list" -> list(context);
            default -> {
                plugin.reloadAll();
                context.reply("screenshare.reloaded", "points", plugin.freezePoints().size());
            }
        }
    }

    private void info(CommandContext context) {
        if (context.size() != 2) {
            throw context.usage();
        }
        ScreenShareSession session = plugin.screenShare().session(context.arg(1));
        if (session == null) {
            throw context.fail("screenshare.unfreeze.not-frozen");
        }
        context.reply("screenshare.info", plugin.screenShare().placeholders(session));
    }

    private void list(CommandContext context) {
        ScreenShareService sessions = plugin.screenShare();
        if (sessions.sessions().isEmpty()) {
            context.reply("screenshare.list-empty");
            return;
        }
        context.reply("screenshare.list-header", "amount", sessions.sessions().size());
        for (ScreenShareSession session : sessions.sessions()) {
            context.reply("screenshare.list-entry", sessions.placeholders(session));
        }
    }

    private static List<String> available(CommandSender sender) {
        List<String> available = new ArrayList<>();
        for (Map.Entry<String, String> entry : SUBCOMMANDS.entrySet()) {
            if (sender.hasPermission(entry.getValue())) {
                available.add(entry.getKey());
            }
        }
        return available;
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            return matching(context, 0, available(context.sender()));
        }
        if (context.size() == 2 && context.arg(0).equalsIgnoreCase("info")
                && context.sender().hasPermission(SUBCOMMANDS.get("info"))) {
            return matching(context, 1, plugin.screenShare().targetNames());
        }
        return List.of();
    }
}
