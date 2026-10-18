package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class FlyCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.fly.others";

    public FlyCommand(DiamondSMP plugin) {
        super(plugin, "fly", "diamondsmp.fly", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Player target = context.targetOrSelf(0, OTHERS);
        applyTo(context, target, player -> {
            boolean enable = !player.getAllowFlight();
            player.setAllowFlight(enable);
            if (!enable) {
                player.setFlying(false);
            }
            return state(enable);
        }, "fly.self", "fly.other", "fly.notify");
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
