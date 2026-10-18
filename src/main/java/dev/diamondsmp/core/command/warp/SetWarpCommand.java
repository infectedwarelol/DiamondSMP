package dev.diamondsmp.core.command.warp;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class SetWarpCommand extends BaseCommand {

    public SetWarpCommand(DiamondSMP plugin) {
        super(plugin, "setwarp", "diamondsmp.setwarp", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        String name = context.arg(0);
        if (!plugin.warps().isValidName(name)) {
            throw context.fail("warp.invalid-name", "warp", name);
        }
        plugin.warps().set(name, player.getLocation());
        context.reply("warp.set", "warp", name.toLowerCase(Locale.ROOT));
    }
}
