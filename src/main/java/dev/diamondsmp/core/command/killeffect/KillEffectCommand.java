package dev.diamondsmp.core.command.killeffect;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.KillEffectService;
import dev.diamondsmp.core.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
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
            toggle(context);
            return;
        }
        if (!context.sender().hasPermission(ADMIN)) {
            throw context.fail("general.no-permission");
        }
        switch (action) {
            case "set" -> set(context);
            case "reload" -> {
                plugin.reloadAll();
                context.reply("killeffect.reloaded", "count", plugin.killEffects().available().size());
            }
            default -> throw context.usage();
        }
    }

    private void toggle(CommandContext context) {
        PlayerProfile profile = plugin.profiles().profile(context.player());
        if (profile == null) {
            throw context.fail("general.data-loading");
        }
        profile.hidesEffects(!profile.hidesEffects());
        context.reply(profile.hidesEffects() ? "killeffect.view-off" : "killeffect.view-on");
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
            return matching(context, 0, admin ? List.of("toggle", "set", "reload") : List.of("toggle"));
        }
        if (!admin) {
            return List.of();
        }
        if (!context.arg(0).equalsIgnoreCase("set")) {
            return List.of();
        }
        if (context.size() == 2) {
            return players(context, 1);
        }
        return matching(context, 2, new ArrayList<>(plugin.killEffects().ids()));
    }
}
