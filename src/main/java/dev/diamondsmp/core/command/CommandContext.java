package dev.diamondsmp.core.command;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public final class CommandContext {

    private final DiamondSMP plugin;
    private final CommandSender sender;
    private final String label;
    private final String[] args;
    private final String usage;

    CommandContext(DiamondSMP plugin, CommandSender sender, String label, String[] args, String usage) {
        this.plugin = plugin;
        this.sender = sender;
        this.label = label;
        this.args = args;
        this.usage = usage;
    }

    public CommandSender sender() {
        return sender;
    }

    public int size() {
        return args.length;
    }

    public boolean has(int index) {
        return index < args.length;
    }

    public String arg(int index) {
        if (!has(index)) {
            throw usage();
        }
        return args[index];
    }

    public String arg(int index, String fallback) {
        return has(index) ? args[index] : fallback;
    }

    public String joined(int from) {
        if (!has(from)) {
            throw usage();
        }
        return String.join(" ", Arrays.copyOfRange(args, from, args.length)).trim();
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public Player player() {
        if (sender instanceof Player player) {
            return player;
        }
        throw new CommandFailure("general.player-only");
    }

    public Player online(int index) {
        String name = arg(index);
        Player found = Bukkit.getPlayerExact(name);
        if (found == null) {
            found = Bukkit.getPlayer(name);
        }
        if (found == null || !plugin.visibility().canSee(sender, found)) {
            throw new CommandFailure("general.player-not-found", "player", name);
        }
        return found;
    }

    public Player targetOrSelf(int index, String othersPermission) {
        if (has(index)) {
            Player target = online(index);
            if (!target.equals(sender) && !sender.hasPermission(othersPermission)) {
                throw new CommandFailure("general.no-permission");
            }
            return target;
        }
        if (sender instanceof Player player) {
            return player;
        }
        throw usage();
    }

    public int integer(int index, int min, int max) {
        String raw = arg(index);
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) {
                throw new CommandFailure("general.number-range", "min", min, "max", max);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new CommandFailure("general.invalid-number", "input", raw);
        }
    }

    public double coordinate(int index, double base) {
        String raw = arg(index);
        boolean relative = raw.startsWith("~");
        String number = relative ? raw.substring(1) : raw;
        try {
            double value = number.isEmpty() ? 0.0D : Double.parseDouble(number);
            if (!Double.isFinite(value) || Math.abs(value) > 30_000_000.0D) {
                throw new CommandFailure("general.invalid-number", "input", raw);
            }
            return relative ? base + value : value;
        } catch (NumberFormatException ex) {
            throw new CommandFailure("general.invalid-number", "input", raw);
        }
    }

    public World world(int index, World fallback) {
        if (!has(index)) {
            if (fallback == null) {
                throw usage();
            }
            return fallback;
        }
        World world = Bukkit.getWorld(args[index]);
        if (world == null) {
            throw new CommandFailure("general.world-not-found", "world", args[index]);
        }
        return world;
    }

    public CommandFailure usage() {
        return new CommandFailure("general.usage", "usage", usage.replace("<command>", label));
    }

    public CommandFailure fail(String key, Object... placeholders) {
        return new CommandFailure(key, placeholders);
    }

    public void error(String key, Object... placeholders) {
        plugin.messages().error(sender, key, placeholders);
    }

    public void reply(String key, Object... placeholders) {
        plugin.messages().send(sender, key, placeholders);
    }
}
