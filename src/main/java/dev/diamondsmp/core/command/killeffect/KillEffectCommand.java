package dev.diamondsmp.core.command.killeffect;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.message.Text;
import dev.diamondsmp.core.service.KillEffectService;
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

    private void set(CommandContext context) {
        Player target = context.online(1);
        String id = context.arg(2).toLowerCase(Locale.ROOT);
        KillEffectService effects = plugin.killEffects();
        if (!KillEffectService.NONE.equals(id) && effects.find(id) == null) {
            throw context.fail("killeffect.unknown", "effect", id);
        }
        if (!effects.select(target, id)) {
            throw context.fail("general.data-loading");
        }
        KillEffectService.Configured effect = effects.find(id);
        Component name = effect == null ? plugin.messages().display("menus.killeffect.none-name")
                : Text.display(plugin.messages().theme(effect.name()));
        context.reply("killeffect.set", "player", target.getName(), "effect", name);
        if (!target.equals(context.sender())) {
            plugin.messages().send(target, "killeffect.selected", "effect", name);
        }
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (!context.sender().hasPermission(ADMIN)) {
            return List.of();
        }
        if (context.size() == 1) {
            return matching(context, 0, List.of("set", "reload"));
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
