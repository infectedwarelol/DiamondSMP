package dev.diamondsmp.core.command.warp;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.List;

public final class WarpsCommand extends BaseCommand {

    public WarpsCommand(DiamondSMP plugin) {
        super(plugin, "warps", "diamondsmp.warps", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player viewer = context.isPlayer() ? context.player() : null;
        List<String> warps = plugin.warps().names(viewer);
        if (warps.isEmpty()) {
            throw context.fail("warp.none");
        }
        context.reply("warp.list", "warps", String.join(", ", warps),
                "count", warps.size());
    }
}
