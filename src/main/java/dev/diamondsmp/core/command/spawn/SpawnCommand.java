package dev.diamondsmp.core.command.spawn;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.TeleportKind;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class SpawnCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.spawn.others";

    public SpawnCommand(DiamondSMP plugin) {
        super(plugin, "spawn", "diamondsmp.spawn", false);
    }

    @Override
    protected void execute(CommandContext context) {
        Location spawn = plugin.spawn().spawn();
        if (spawn == null) {
            throw context.fail("spawn.not-set");
        }
        Player target = context.targetOrSelf(0, OTHERS);
        if (target.equals(context.sender())) {
            plugin.teleports().request(target, TeleportKind.SPAWN, plugin.spawn()::spawn, "spawn.teleported");
            return;
        }
        CommandSender sender = context.sender();
        plugin.teleports().teleport(target, spawn).thenAccept(success -> {
            if (!success) {
                plugin.messages().error(sender, "teleport.failed");
                return;
            }
            plugin.messages().send(sender, "spawn.sent", "player", target.getName());
            plugin.messages().send(target, "spawn.teleported");
        });
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
