package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class ClearCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.clear.others";

    public ClearCommand(DiamondSMP plugin) {
        super(plugin, "clear", "diamondsmp.clear", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.targetOrSelf(0, OTHERS);
        applyTo(context, target, player -> {
            player.getInventory().clear();
            player.setItemOnCursor(null);
            return null;
        }, "clear.self", "clear.other", "clear.notify");
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
