package dev.diamondsmp.core.command.spawn;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.util.Format;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class SetSpawnCommand extends BaseCommand {

    public SetSpawnCommand(DiamondSMP plugin) {
        super(plugin, "setspawn", "diamondsmp.setspawn", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        Location location = player.getLocation();
        plugin.spawn().set(location);
        context.reply("spawn.set", "position", Format.coordinates(location.getX(), location.getY(), location.getZ()),
                "world", player.getWorld().getName());
    }
}
