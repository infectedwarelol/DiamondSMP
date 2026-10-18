package dev.diamondsmp.core.command.stats;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.KillTokenService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class KillTokenCommand extends BaseCommand {

    public KillTokenCommand(DiamondSMP plugin) {
        super(plugin, "killtoken", "diamondsmp.killtoken.give", false);
    }

    @Override
    protected void execute(CommandContext context) {
        KillTokenService.Kind kind = KillTokenService.Kind.parse(context.arg(0, "token"));
        if (kind == null) {
            throw context.usage();
        }
        int amount = context.has(1) ? context.integer(1, 1, 2304) : 1;
        Player target = context.targetOrSelf(2, "diamondsmp.killtoken.give");
        applyTo(context, target, player -> {
            plugin.killTokens().give(player, kind, amount);
            return null;
        }, "kill-tokens.given-self", "kill-tokens.given-other", "kill-tokens.received",
                "amount", amount, "type", plugin.messages().get("kill-tokens.types." + kind.id()));
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            List<String> kinds = new ArrayList<>();
            for (KillTokenService.Kind kind : KillTokenService.Kind.values()) {
                kinds.add(kind.id());
            }
            return matching(context, 0, kinds);
        }
        if (context.size() == 2) {
            return matching(context, 1, List.of("1", "16", "64"));
        }
        return players(context, 2);
    }
}
