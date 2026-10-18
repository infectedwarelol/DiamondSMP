package dev.diamondsmp.core.command.screenshare;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.FreezePointService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public final class FreezePointCommand extends BaseCommand {

    public enum Mode {
        SET,
        DELETE,
        LIST
    }

    private final Mode mode;

    public FreezePointCommand(DiamondSMP plugin, String name, String permission, Mode mode) {
        super(plugin, name, permission, mode == Mode.SET);
        this.mode = mode;
    }

    @Override
    protected void execute(CommandContext context) {
        switch (mode) {
            case SET -> create(context);
            case DELETE -> delete(context);
            case LIST -> list(context);
        }
    }

    private void create(CommandContext context) {
        Player player = context.player();
        Location location = player.getLocation();
        FreezePointService.FreezePoint point = plugin.freezePoints().create(location);
        context.reply("screenshare.freeze-point.created", point.placeholders());
        if (location.clone().subtract(0.0D, 0.05D, 0.0D).getBlock().isPassable()) {
            context.reply("screenshare.freeze-point.not-on-ground");
        }
        plugin.screenShare().play(player, "point-created");
    }

    private void delete(CommandContext context) {
        if (context.size() != 1) {
            throw context.usage();
        }
        FreezePointService.FreezePoint point = plugin.freezePoints().find(context.arg(0));
        if (point == null || !plugin.freezePoints().delete(point)) {
            throw context.fail("screenshare.freeze-point.invalid");
        }
        context.reply("screenshare.freeze-point.deleted", point.placeholders());
        plugin.screenShare().play(context.sender(), "point-deleted");
    }

    private void list(CommandContext context) {
        FreezePointService points = plugin.freezePoints();
        if (points.isEmpty()) {
            context.reply("screenshare.freeze-point.list-empty");
            return;
        }
        context.reply("screenshare.freeze-point.list-header", "amount", points.size());
        for (FreezePointService.FreezePoint point : points.all()) {
            context.reply("screenshare.freeze-point.list-entry", point.placeholders());
        }
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return mode == Mode.DELETE ? matching(context, 0, plugin.freezePoints().ids()) : List.of();
    }
}
