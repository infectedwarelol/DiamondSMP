package dev.diamondsmp.core.command.killeffect;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.KillEffectService;
import dev.diamondsmp.core.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KillEffectCommand extends BaseCommand {

    private static final String ADMIN = "diamondsmp.killeffect.admin";

    public KillEffectCommand(DiamondSMP plugin) {
        super(plugin, "killeffect", "diamondsmp.killeffect", false);
    }

    @Override
    protected void execute(CommandContext context) {
        if (!context.has(0)) {
            plugin.killEffects().open(context.player(), 0);
            return;
        }
        String action = context.arg(0).toLowerCase(Locale.ROOT);
        if (action.equals("toggle")) {
            toggle(context, false);
            return;
        }
        if (action.equals("private")) {
            toggle(context, true);
            return;
        }
        if (!context.sender().hasPermission(ADMIN)) {
            throw context.fail("general.no-permission");
        }
        switch (action) {
            case "set" -> set(context);
            case "give" -> grant(context, true);
            case "take" -> grant(context, false);
            case "reload" -> {
                plugin.reloadAll(() -> context.reply("killeffect.reloaded",
                        "count", plugin.killEffects().available().size()));
            }
            default -> throw context.usage();
        }
    }

    private void toggle(CommandContext context, boolean ownEffects) {
        PlayerProfile profile = plugin.profiles().profile(context.player());
        if (profile == null) {
            throw context.fail("general.data-loading");
        }
        if (ownEffects) {
            profile.privateEffects(!profile.privateEffects());
            context.reply(profile.privateEffects() ? "killeffect.private-on" : "killeffect.private-off");
        } else {
            profile.hidesEffects(!profile.hidesEffects());
            context.reply(profile.hidesEffects() ? "killeffect.view-off" : "killeffect.view-on");
        }
    }

    private void grant(CommandContext context, boolean give) {
        String name = context.arg(1);
        String id = context.arg(2).toLowerCase(Locale.ROOT);
        KillEffectService effects = plugin.killEffects();
        if (!KillEffectService.ALL.equals(id) && effects.find(id) == null) {
            throw context.fail("killeffect.unknown", "effect", id);
        }
        Component effect = KillEffectService.ALL.equals(id) ? plugin.messages().get("killeffect.all")
                : effects.displayName(id);
        CommandSender sender = context.sender();
        effects.grant(name, id, give).thenAccept(result -> {
            Player online = Bukkit.getPlayerExact(name);
            String player = online == null ? name : online.getName();
            switch (result) {
                case UNKNOWN_PLAYER -> plugin.messages().error(sender, "general.player-not-found", "player", name);
                case UNCHANGED -> plugin.messages().error(sender, give ? "killeffect.already-owned" : "killeffect.not-owned",
                        "player", player, "effect", effect);
                case GRANTED -> {
                    plugin.messages().send(sender, "killeffect.given", "player", player, "effect", effect);
                    if (online != null && !online.equals(sender)) {
                        plugin.messages().send(online, "killeffect.received", "effect", effect);
                    }
                }
                case REVOKED -> plugin.messages().send(sender, "killeffect.taken", "player", player, "effect", effect);
            }
        });
    }

    private void set(CommandContext context) {
        Player target = context.online(1);
        String id = context.arg(2).toLowerCase(Locale.ROOT);
        KillEffectService effects = plugin.killEffects();
        if (effects.isUnknown(id)) {
            throw context.fail("killeffect.unknown", "effect", id);
        }
        if (!effects.select(target, id)) {
            throw context.fail("general.data-loading");
        }
        Component name = effects.displayName(id);
        context.reply("killeffect.set", "player", target.getName(), "effect", name);
        if (!target.equals(context.sender())) {
            plugin.messages().send(target, "killeffect.selected", "effect", name);
        }
    }

    @Override
    protected List<String> complete(CommandContext context) {
        boolean admin = context.sender().hasPermission(ADMIN);
        if (context.size() == 1) {
            return matching(context, 0, admin ? List.of("toggle", "private", "give", "take", "set", "reload")
                    : List.of("toggle", "private"));
        }
        String action = context.arg(0).toLowerCase(Locale.ROOT);
        if (!admin || !(action.equals("set") || action.equals("give") || action.equals("take"))) {
            return List.of();
        }
        if (context.size() == 2) {
            return players(context, 1);
        }
        if (action.equals("set")) {
            return matching(context, 2, new ArrayList<>(plugin.killEffects().ids()));
        }
        List<String> options = new ArrayList<>(plugin.killEffects().effectIds());
        options.add(KillEffectService.ALL);
        return matching(context, 2, options);
    }
}
