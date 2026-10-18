package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;

public final class HealCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.heal.others";

    public HealCommand(DiamondSMP plugin) {
        super(plugin, "heal", "diamondsmp.heal", false);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void execute(CommandContext context) {
        Player target = context.targetOrSelf(0, OTHERS);
        boolean clearEffects = plugin.settings().getBoolean("heal.clear-effects", true);
        boolean extinguish = plugin.settings().getBoolean("heal.extinguish", true);
        applyTo(context, target, player -> {
            if (player.isDead()) {
                return null;
            }
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setExhaustion(0.0F);
            if (extinguish) {
                player.setFireTicks(0);
            }
            if (clearEffects) {
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
            }
            return null;
        }, "heal.self", "heal.other", "heal.notify");
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return context.sender().hasPermission(OTHERS) ? players(context, 0) : Collections.emptyList();
    }
}
