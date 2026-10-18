package dev.diamondsmp.core.command.stats;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.KillStreakService;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class KillStreakCommand extends BaseCommand {

    private static final String RELOAD = "diamondsmp.killstreak.reload";
    private static final String MODIFY = "diamondsmp.killstreak.modify";
    private static final List<String> CHANGES = List.of("set", "add", "clear");

    public KillStreakCommand(DiamondSMP plugin) {
        super(plugin, "killstreak", "diamondsmp.killstreak", false);
    }

    @Override
    protected void execute(CommandContext context) {
        if (!context.has(0)) {
            Player player = context.player();
            KillStreakService.Known streak = plugin.killStreaks().lookup(player.getUniqueId());
            if (streak == null) {
                throw context.fail("general.data-loading");
            }
            context.reply("killstreak.self", "current", streak.current(), "highest", streak.highest());
            return;
        }
        String action = context.arg(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "reload" -> reload(context);
            case "set", "add", "clear" -> change(context, KillStreakService.Change.valueOf(action.toUpperCase(Locale.ROOT)));
            default -> show(context, context.arg(0));
        }
    }

    private void reload(CommandContext context) {
        if (!context.sender().hasPermission(RELOAD)) {
            throw context.fail("general.no-permission");
        }
        plugin.reloadAll();
        context.reply("killstreak.reloaded");
    }

    private void show(CommandContext context, String name) {
        CommandSender sender = context.sender();
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && plugin.visibility().canSee(sender, online)) {
            KillStreakService.Known streak = plugin.killStreaks().lookup(online.getUniqueId());
            if (streak != null) {
                context.reply("killstreak.other", "player", online.getName(), "current", streak.current(),
                        "highest", streak.highest());
                return;
            }
        }
        CompletableFuture<Optional<ProfileSnapshot>> lookup = plugin.profiles().lookup(name);
        lookup.thenAccept(found -> {
            if (found.isEmpty()) {
                plugin.messages().error(sender, "killstreak.unknown-player", "player", name);
                return;
            }
            ProfileSnapshot snapshot = found.get();
            plugin.messages().send(sender, "killstreak.other", "player", snapshot.name(),
                    "current", snapshot.streak(), "highest", snapshot.bestStreak());
        });
    }

    private void change(CommandContext context, KillStreakService.Change change) {
        if (!context.sender().hasPermission(MODIFY)) {
            throw context.fail("general.no-permission");
        }
        String name = context.arg(1);
        int amount = change == KillStreakService.Change.CLEAR ? 0
                : context.integer(2, change == KillStreakService.Change.ADD ? -1_000_000 : 0, 1_000_000);
        CommandSender sender = context.sender();
        plugin.killStreaks().change(name, change, amount).thenAccept(result -> {
            if (result.isEmpty()) {
                plugin.messages().error(sender, "killstreak.unknown-player", "player", name);
                return;
            }
            KillStreakService.Known streak = result.get();
            String key = switch (change) {
                case SET -> "killstreak.modify-set";
                case ADD -> "killstreak.modify-add";
                case CLEAR -> "killstreak.modify-clear";
            };
            plugin.messages().send(sender, key, "player", streak.name(), "amount", amount,
                    "current", streak.current());
        });
    }

    @Override
    protected List<String> complete(CommandContext context) {
        CommandSender sender = context.sender();
        if (context.size() == 1) {
            List<String> options = new ArrayList<>(players(context, 0));
            if (sender.hasPermission(RELOAD)) {
                options.addAll(matching(context, 0, List.of("reload")));
            }
            if (sender.hasPermission(MODIFY)) {
                options.addAll(matching(context, 0, CHANGES));
            }
            return options;
        }
        if (context.size() == 2 && sender.hasPermission(MODIFY)
                && CHANGES.contains(context.arg(0).toLowerCase(Locale.ROOT))) {
            return players(context, 1);
        }
        return List.of();
    }
}
