package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

public final class EnderChestCommand extends BaseCommand {

    public EnderChestCommand(DiamondSMP plugin) {
        super(plugin, "enderchest", "diamondsmp.enderchest", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        player.openInventory(player.getEnderChest());
    }
}
