package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.gui.TrashMenu;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class TrashCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.trash.others";

    public TrashCommand(DiamondSMP plugin) {
        super(plugin, "trash", "diamondsmp.trash", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.targetOrSelf(0, OTHERS);
        applyTo(context, target, player -> {
            new TrashMenu(plugin).open(player);
            return null;
        }, "trash.opened", "trash.opened-other", null);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
