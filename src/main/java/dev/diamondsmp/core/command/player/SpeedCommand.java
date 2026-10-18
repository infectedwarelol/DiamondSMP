package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class SpeedCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.speed.others";
    private static final float DEFAULT_WALK = 0.2F;
    private static final float DEFAULT_FLY = 0.1F;
    private static final List<String> LEVELS = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

    public SpeedCommand(DiamondSMP plugin) {
        super(plugin, "speed", "diamondsmp.speed", false);
    }

    @Override
    protected void execute(CommandContext context) {
        int level = context.integer(0, 1, 10);
        Player target = context.targetOrSelf(1, OTHERS);
        applyTo(context, target, player -> {
            boolean flying = player.isFlying();
            float speed = scale(level, flying ? DEFAULT_FLY : DEFAULT_WALK);
            if (flying) {
                player.setFlySpeed(speed);
            } else {
                player.setWalkSpeed(speed);
            }
            return plugin.messages().get(flying ? "speed.type-fly" : "speed.type-walk");
        }, "speed.self", "speed.other", "speed.notify", "speed", level);
    }

    private static float scale(int level, float base) {
        if (level <= 1) {
            return base;
        }
        return Math.min(1.0F, base + (level - 1) / 9.0F * (1.0F - base));
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            return matching(context, 0, LEVELS);
        }
        return context.sender().hasPermission(OTHERS) ? players(context, 1) : Collections.emptyList();
    }
}
