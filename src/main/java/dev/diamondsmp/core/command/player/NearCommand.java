package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NearCommand extends BaseCommand {

    private record Nearby(String name, double distance) {
    }

    public NearCommand(DiamondSMP plugin) {
        super(plugin, "near", "diamondsmp.near", true);
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        int radius = Math.max(1, plugin.settings().getInt("near.radius", 200));
        double limit = (double) radius * radius;
        Location origin = player.getLocation();
        List<Nearby> nearby = new ArrayList<>();
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || !plugin.visibility().canSee(player, other)) {
                continue;
            }
            double distance = origin.distanceSquared(other.getLocation());
            if (distance <= limit) {
                nearby.add(new Nearby(other.getName(), Math.sqrt(distance)));
            }
        }
        if (nearby.isEmpty()) {
            context.reply("near.none", "radius", radius);
            return;
        }
        nearby.sort(Comparator.comparingDouble(Nearby::distance));
        StringBuilder list = new StringBuilder();
        for (Nearby entry : nearby) {
            if (!list.isEmpty()) {
                list.append(", ");
            }
            list.append(entry.name()).append(" (").append(Math.round(entry.distance())).append("m)");
        }
        context.reply("near.list", "radius", radius, "count", nearby.size(), "players", list.toString());
    }
}
