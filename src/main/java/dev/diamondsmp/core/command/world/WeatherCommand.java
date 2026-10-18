package dev.diamondsmp.core.command.world;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WeatherCommand extends BaseCommand {

    private static final List<String> TYPES = List.of("clear", "rain", "thunder");
    private static final int DEFAULT_DURATION_TICKS = 20 * 60 * 10;

    public WeatherCommand(DiamondSMP plugin) {
        super(plugin, "weather", "diamondsmp.weather", false);
    }

    @Override
    protected void execute(CommandContext context) {
        String type = context.arg(0).toLowerCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            throw context.usage();
        }
        CommandSender sender = context.sender();
        World fallback = sender instanceof Player player ? player.getWorld()
                : Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        World world = context.world(1, fallback);
        plugin.scheduler().global(() -> {
            switch (type) {
                case "clear" -> {
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setClearWeatherDuration(DEFAULT_DURATION_TICKS);
                }
                case "rain" -> {
                    world.setStorm(true);
                    world.setThundering(false);
                    world.setWeatherDuration(DEFAULT_DURATION_TICKS);
                }
                default -> {
                    world.setStorm(true);
                    world.setThundering(true);
                    world.setWeatherDuration(DEFAULT_DURATION_TICKS);
                    world.setThunderDuration(DEFAULT_DURATION_TICKS);
                }
            }
            plugin.messages().send(sender, "weather.set", "weather", type, "world", world.getName());
        });
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            return matching(context, 0, TYPES);
        }
        List<String> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            worlds.add(world.getName());
        }
        return matching(context, 1, worlds);
    }
}
