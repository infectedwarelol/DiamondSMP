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
import java.util.Map;

public final class TimeCommand extends BaseCommand {

    private static final Map<String, Long> PRESETS = Map.of(
            "day", 1000L,
            "noon", 6000L,
            "night", 13000L,
            "midnight", 18000L);
    private static final List<String> FIRST = List.of("set", "add", "day", "noon", "night", "midnight");

    private final String preset;

    public TimeCommand(DiamondSMP plugin, String name, String preset) {
        super(plugin, name, "diamondsmp.time", false);
        this.preset = preset;
    }

    @Override
    protected void execute(CommandContext context) {
        if (preset != null) {
            World world = context.world(0, defaultWorld(context.sender()));
            apply(context, world, PRESETS.get(preset), false);
            return;
        }
        String action = context.arg(0).toLowerCase(Locale.ROOT);
        if (PRESETS.containsKey(action)) {
            apply(context, context.world(1, defaultWorld(context.sender())), PRESETS.get(action), false);
            return;
        }
        if (!action.equals("set") && !action.equals("add")) {
            throw context.usage();
        }
        String raw = context.arg(1).toLowerCase(Locale.ROOT);
        Long ticks = PRESETS.get(raw);
        if (ticks == null) {
            ticks = (long) context.integer(1, 0, 24000 * 365);
        }
        apply(context, context.world(2, defaultWorld(context.sender())), ticks, action.equals("add"));
    }

    private void apply(CommandContext context, World world, long ticks, boolean add) {
        CommandSender sender = context.sender();
        plugin.scheduler().global(() -> {
            if (add) {
                world.setTime(world.getTime() + ticks);
            } else {
                world.setTime(ticks);
            }
            plugin.messages().send(sender, "time.set", "time", world.getTime(), "world", world.getName());
        });
    }

    private static World defaultWorld(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    @Override
    protected List<String> complete(CommandContext context) {
        List<String> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            worlds.add(world.getName());
        }
        if (preset != null) {
            return matching(context, 0, worlds);
        }
        if (context.size() == 1) {
            return matching(context, 0, FIRST);
        }
        String action = context.arg(0).toLowerCase(Locale.ROOT);
        if (PRESETS.containsKey(action)) {
            return matching(context, 1, worlds);
        }
        if (context.size() == 2) {
            return matching(context, 1, List.copyOf(PRESETS.keySet()));
        }
        return matching(context, 2, worlds);
    }
}
