package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.util.Format;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class GamemodeCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.gamemode.others";
    private static final List<String> MODES = List.of("survival", "creative", "adventure", "spectator");

    private final GameMode fixed;

    public GamemodeCommand(DiamondSMP plugin, String name, GameMode fixed) {
        super(plugin, name, fixed == null ? "diamondsmp.gamemode" : permission(fixed), false);
        this.fixed = fixed;
    }

    private static String permission(GameMode mode) {
        return "diamondsmp.gamemode." + mode.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void execute(CommandContext context) {
        GameMode mode = fixed;
        int targetIndex = 0;
        if (mode == null) {
            String raw = context.arg(0);
            mode = parse(raw);
            if (mode == null) {
                throw context.fail("gamemode.unknown", "mode", raw);
            }
            if (!context.sender().hasPermission(permission(mode))) {
                throw context.fail("general.no-permission");
            }
            targetIndex = 1;
        }
        Player target = context.targetOrSelf(targetIndex, OTHERS);
        GameMode selected = mode;
        applyTo(context, target, player -> {
            player.setGameMode(selected);
            return null;
        }, "gamemode.self", "gamemode.other", "gamemode.notify", "mode", Format.pretty(selected.name()));
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (fixed != null) {
            return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
        }
        if (context.size() == 1) {
            return matching(context, 0, MODES);
        }
        return context.sender().hasPermission(OTHERS) ? players(context, 1) : Collections.emptyList();
    }

    private static GameMode parse(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
