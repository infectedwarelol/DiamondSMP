package dev.diamondsmp.core.command;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;

public abstract class BaseCommand implements TabExecutor {

    protected final DiamondSMP plugin;
    private final String name;
    private final String permission;
    private final boolean playerOnly;

    protected BaseCommand(DiamondSMP plugin, String name, String permission, boolean playerOnly) {
        this.plugin = plugin;
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }

    public final String name() {
        return name;
    }

    public final void register() {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("The command /" + name + " is missing from plugin.yml");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
        if (permission != null) {
            command.setPermission(permission);
        }
        refresh();
    }

    public final void refresh() {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            command.permissionMessage(plugin.messages().get("general.no-permission"));
        }
    }

    public final void unregister() {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            command.setExecutor(null);
            command.setTabCompleter(null);
        }
    }

    protected abstract void execute(CommandContext context);

    protected List<String> complete(CommandContext context) {
        return Collections.emptyList();
    }

    @Override
    public final boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                   @NotNull String label, @NotNull String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            plugin.messages().error(sender, "general.no-permission");
            return true;
        }
        if (playerOnly && !(sender instanceof Player)) {
            plugin.messages().error(sender, "general.player-only");
            return true;
        }
        try {
            execute(new CommandContext(plugin, sender, label, args, command.getUsage()));
        } catch (CommandFailure failure) {
            plugin.messages().error(sender, failure.key(), failure.placeholders());
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "The command /" + label + " failed for " + sender.getName(), ex);
            plugin.messages().error(sender, "general.error");
        }
        return true;
    }

    @Override
    public final List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                            @NotNull String label, @NotNull String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        try {
            List<String> result = complete(new CommandContext(plugin, sender, label, args, command.getUsage()));
            return result == null ? Collections.emptyList() : result;
        } catch (RuntimeException ex) {
            return Collections.emptyList();
        }
    }

    protected final void applyTo(CommandContext context, Player target, Function<Player, Object> action,
                                 String selfKey, String otherKey, String notifyKey, Object... placeholders) {
        CommandSender sender = context.sender();
        plugin.scheduler().entity(target, () -> {
            if (!target.isOnline()) {
                return;
            }
            Object state = action.apply(target);
            Object[] base = state == null ? placeholders : append(placeholders, "state", state);
            if (target.equals(sender)) {
                plugin.messages().send(sender, selfKey, base);
                return;
            }
            plugin.messages().send(sender, otherKey, append(base, "player", target.getName()));
            if (notifyKey != null) {
                plugin.messages().send(target, notifyKey, append(base, "player", sender.getName()));
            }
        }, () -> plugin.messages().error(sender, "general.player-not-found", "player", target.getName()));
    }

    protected final List<String> players(CommandContext context, int index) {
        if (context.size() != index + 1) {
            return Collections.emptyList();
        }
        String prefix = context.arg(index).toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.visibility().canSee(context.sender(), online)
                    && online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(online.getName());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    protected static List<String> matching(CommandContext context, int index, Collection<String> options) {
        if (context.size() != index + 1) {
            return Collections.emptyList();
        }
        String prefix = context.arg(index).toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(option);
            }
        }
        return result;
    }

    protected final Object state(boolean enabled) {
        Object component = plugin.messages().get(enabled ? "general.enabled" : "general.disabled");
        return component == null ? String.valueOf(enabled) : component;
    }

    private static Object[] append(Object[] placeholders, String key, Object value) {
        Object[] result = new Object[placeholders.length + 2];
        System.arraycopy(placeholders, 0, result, 0, placeholders.length);
        result[placeholders.length] = key;
        result[placeholders.length + 1] = value;
        return result;
    }
}
