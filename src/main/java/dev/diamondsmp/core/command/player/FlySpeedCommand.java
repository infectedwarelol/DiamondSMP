package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class FlySpeedCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.flyspeed.others";
    private static final float DEFAULT_FLY = 0.1F;
    private static final List<String> LEVELS = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

    public FlySpeedCommand(DiamondSMP plugin) {
        super(plugin, "flyspeed", "diamondsmp.flyspeed", false);
    }

    @Override
    protected void execute(CommandContext context) {
        int level = context.integer(0, 1, 10);
        Player target = context.targetOrSelf(1, OTHERS);
        float speed = level <= 1 ? DEFAULT_FLY : Math.min(1.0F, DEFAULT_FLY + (level - 1) / 9.0F * (1.0F - DEFAULT_FLY));
        applyTo(context, target, player -> {
            player.setFlySpeed(speed);
            return plugin.messages().get("speed.type-fly");
        }, "speed.self", "speed.other", "speed.notify", "speed", level);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            return matching(context, 0, LEVELS);
        }
        return context.sender().hasPermission(OTHERS) ? players(context, 1) : Collections.emptyList();
    }
}
