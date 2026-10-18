package dev.diamondsmp.core.command.warp;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.TeleportKind;
import dev.diamondsmp.core.storage.StoredLocation;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class WarpCommand extends BaseCommand {

    public WarpCommand(DiamondSMP plugin) {
        super(plugin, "warp", "diamondsmp.warp", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        if (!context.has(0)) {
            List<String> warps = plugin.warps().names(player);
            context.reply(warps.isEmpty() ? "warp.none" : "warp.list", "warps", String.join(", ", warps),
                    "count", warps.size());
            return;
        }
        String name = context.arg(0).toLowerCase(Locale.ROOT);
        StoredLocation warp = plugin.warps().find(name);
        if (warp == null || !plugin.warps().canUse(player, name)) {
            throw context.fail("warp.not-found", "warp", name);
        }
        plugin.teleports().request(player, TeleportKind.WARP, warp::toLocation, "warp.teleported", "warp", name);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.isPlayer() ? matching(context, 0, plugin.warps().names(context.player())) : List.of();
    }
}
