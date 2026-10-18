package dev.diamondsmp.core.command.teleport;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.TeleportKind;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class BackCommand extends BaseCommand {

    public BackCommand(DiamondSMP plugin) {
        super(plugin, "back", "diamondsmp.back", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        Location back = plugin.teleports().back(player);
        if (back == null) {
            throw context.fail("back.none");
        }
        plugin.teleports().request(player, TeleportKind.BACK, () -> back, "back.teleported");
    }
}
