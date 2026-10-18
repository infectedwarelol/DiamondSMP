package dev.diamondsmp.core.command.screenshare;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.FreezePointService;
import dev.diamondsmp.core.service.ScreenShareService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class FreezeCommand extends BaseCommand {

    public FreezeCommand(DiamondSMP plugin) {
        super(plugin, "freeze", "diamondsmp.screenshare.freeze", false);
    }

    @Override
    protected void execute(CommandContext context) {
        if (context.size() < 1 || context.size() > 2) {
            throw context.usage();
        }
        Player target = context.online(0);
        ScreenShareService sessions = plugin.screenShare();
        if (target.equals(context.sender())) {
            throw context.fail("screenshare.freeze.self");
        }
        if (target.hasPermission(ScreenShareService.BYPASS_PERMISSION)) {
            throw context.fail("screenshare.freeze.bypass");
        }
        if (sessions.isFrozen(target)) {
            throw context.fail("screenshare.freeze.already-frozen");
        }
        if (plugin.freezePoints().isEmpty()) {
            throw context.fail("screenshare.freeze-point.none");
        }
        FreezePointService.FreezePoint point = context.has(1)
                ? plugin.freezePoints().find(context.arg(1)) : sessions.selectPoint();
        if (point == null) {
            throw context.fail("screenshare.freeze-point.invalid");
        }
        Location anchor = point.toLocation();
        if (anchor == null) {
            throw context.fail("screenshare.freeze-point.unavailable", point.placeholders());
        }
        sessions.freeze(context.sender(), target, point, anchor);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            List<String> names = new ArrayList<>();
            for (String name : players(context, 0)) {
                Player player = Bukkit.getPlayerExact(name);
                if (player != null && !player.equals(context.sender()) && !plugin.screenShare().isFrozen(player)
                        && !player.hasPermission(ScreenShareService.BYPASS_PERMISSION)) {
                    names.add(name);
                }
            }
            return names;
        }
        return matching(context, 1, plugin.freezePoints().ids());
    }
}
