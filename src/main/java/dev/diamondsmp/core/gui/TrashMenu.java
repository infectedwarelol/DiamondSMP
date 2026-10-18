package dev.diamondsmp.core.gui;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class TrashMenu extends Menu {

    private final DiamondSMP plugin;

    public TrashMenu(DiamondSMP plugin) {
        super(plugin.settings().getInt("trash.rows", 4), plugin.messages().display("menus.trash.title"));
        this.plugin = plugin;
    }

    @Override
    public void click(InventoryClickEvent event) {
    }

    @Override
    public void drag(InventoryDragEvent event) {
    }

    @Override
    public void close(InventoryCloseEvent event) {
        int disposed = 0;
        for (ItemStack item : getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                disposed += item.getAmount();
            }
        }
        getInventory().clear();
        if (disposed > 0 && event.getPlayer() instanceof Player player) {
            plugin.messages().send(player, "trash.disposed", "amount", disposed);
        }
    }
}
