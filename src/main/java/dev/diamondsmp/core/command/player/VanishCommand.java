package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class VanishCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.vanish.others";

    public VanishCommand(DiamondSMP plugin) {
        super(plugin, "vanish", "diamondsmp.vanish", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.targetOrSelf(0, OTHERS);
        boolean hide = !plugin.visibility().isVanished(target);
        plugin.visibility().setVanished(target, hide);
        applyTo(context, target, player -> state(hide), "vanish.self", "vanish.other", "vanish.notify");
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
