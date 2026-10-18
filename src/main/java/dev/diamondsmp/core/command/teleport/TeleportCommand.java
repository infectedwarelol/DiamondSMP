package dev.diamondsmp.core.command.teleport;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TeleportCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.teleport.others";
    private static final String COORDINATES = "diamondsmp.teleport.coordinates";

    public TeleportCommand(DiamondSMP plugin) {
        super(plugin, "teleport", "diamondsmp.teleport", false);
    }

    @Override
    protected void execute(CommandContext context) {
        switch (context.size()) {
            case 1 -> toPlayer(context);
            case 2 -> playerToPlayer(context);
            case 3, 4 -> toCoordinates(context);
            default -> throw context.usage();
        }
    }

    private void toPlayer(CommandContext context) {
        Player player = context.player();
        Player target = context.online(0);
        if (target.equals(player)) {
            throw context.fail("teleport.self");
        }
        CommandSender sender = context.sender();
        plugin.teleports().teleport(player, target.getLocation()).thenAccept(success -> plugin.messages()
                .send(sender, success ? "teleport.to-player" : "teleport.failed", "player", target.getName()));
    }

    private void playerToPlayer(CommandContext context) {
        if (!context.sender().hasPermission(OTHERS)) {
            throw context.fail("general.no-permission");
        }
        Player moving = context.online(0);
        Player target = context.online(1);
        if (moving.equals(target)) {
            throw context.fail("teleport.self");
        }
        CommandSender sender = context.sender();
        plugin.teleports().teleport(moving, target.getLocation()).thenAccept(success -> {
            if (!success) {
                plugin.messages().send(sender, "teleport.failed");
                return;
            }
            plugin.messages().send(sender, "teleport.player-to-player", "player", moving.getName(),
                    "target", target.getName());
            plugin.messages().send(moving, "teleport.to-player", "player", target.getName());
        });
    }

    private void toCoordinates(CommandContext context) {
        if (!context.sender().hasPermission(COORDINATES)) {
            throw context.fail("general.no-permission");
        }
        Player player = context.player();
        Location current = player.getLocation();
        double x = context.coordinate(0, current.getX());
        double y = context.coordinate(1, current.getY());
        double z = context.coordinate(2, current.getZ());
        World world = context.world(3, player.getWorld());
        Location destination = new Location(world, x, y, z, current.getYaw(), current.getPitch());
        String position = Format.coordinates(x, y, z);
        plugin.teleports().teleport(player, destination).thenAccept(success -> plugin.messages()
                .send(player, success ? "teleport.to-coordinates" : "teleport.failed",
                        "position", position, "world", world.getName()));
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            List<String> options = new ArrayList<>(players(context, 0));
            if (context.sender().hasPermission(COORDINATES) && context.arg(0).isEmpty()) {
                options.add("~");
            }
            return options;
        }
        if (context.size() == 2 && context.sender().hasPermission(OTHERS)) {
            return players(context, 1);
        }
        if (context.size() == 4 && context.sender().hasPermission(COORDINATES)) {
            List<String> worlds = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                worlds.add(world.getName());
            }
            return matching(context, 3, worlds);
        }
        return Collections.emptyList();
    }
}
