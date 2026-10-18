package dev.diamondsmp.core.command.player;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RepairCommand extends BaseCommand {

    private static final String OTHERS = "diamondsmp.repair.others";
    private static final String ALL = "diamondsmp.repair.all";
    private static final List<String> MODES = List.of("hand", "all");

    public RepairCommand(DiamondSMP plugin) {
        super(plugin, "repair", "diamondsmp.repair", false);
    }

    @Override
    protected void execute(CommandContext context) {
        String mode = context.arg(0, "hand").toLowerCase(Locale.ROOT);
        if (!MODES.contains(mode)) {
            throw context.usage();
        }
        boolean all = mode.equals("all");
        if (all && !context.sender().hasPermission(ALL)) {
            throw context.fail("general.no-permission");
        }
        Player target = context.targetOrSelf(1, OTHERS);
        applyTo(context, target, player -> all ? repairAll(player) : repairHand(player),
                "repair.self", "repair.other", "repair.notify");
    }

    private static int repairHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!repair(item)) {
            return 0;
        }
        player.getInventory().setItemInMainHand(item);
        return 1;
    }

    private static int repairAll(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        int repaired = 0;
        for (ItemStack item : contents) {
            if (repair(item)) {
                repaired++;
            }
        }
        if (repaired > 0) {
            player.getInventory().setContents(contents);
        }
        return repaired;
    }

    private static boolean repair(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getType().getMaxDurability() <= 0) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable) || !damageable.hasDamage()) {
            return false;
        }
        damageable.setDamage(0);
        item.setItemMeta(meta);
        return true;
    }

    @Override
    protected List<String> complete(CommandContext context) {
        if (context.size() == 1) {
            return matching(context, 0, MODES);
        }
        return context.sender().hasPermission(OTHERS) ? players(context, 1) : Collections.emptyList();
    }
}
