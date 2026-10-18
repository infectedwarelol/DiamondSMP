package dev.diamondsmp.core.gui;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.util.Items;
import dev.diamondsmp.core.util.Lookup;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public final class LeaderboardMenu extends Menu {

    private final DiamondSMP plugin;
    private final Map<LeaderboardSection, Integer> slots = new EnumMap<>(LeaderboardSection.class);

    public LeaderboardMenu(DiamondSMP plugin) {
        super(3, plugin.messages().display("menus.leaderboard.title"));
        this.plugin = plugin;
        Inventory inventory = getInventory();
        int fallbackSlot = 10;
        for (LeaderboardSection section : LeaderboardSection.values()) {
            String path = "leaderboard.sections." + section.id() + ".";
            int slot = Math.max(0, Math.min(inventory.getSize() - 1,
                    plugin.settings().getInt(path + "slot", fallbackSlot)));
            fallbackSlot += 2;
            slots.put(section, slot);
            Material icon = Lookup.material(plugin.settings().getString(path + "icon"), Material.PAPER);
            String messages = "menus.leaderboard.sections." + section.id() + ".";
            inventory.setItem(slot, Items.create(icon, plugin.messages().display(messages + "name"),
                    plugin.messages().lore(messages + "lore"), false));
        }
        ItemStack filler = Items.create(Lookup.material(plugin.settings().getString("leaderboard.filler"),
                Material.GRAY_STAINED_GLASS_PANE), Component.text(" "), null, false);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler.clone());
            }
        }
    }

    @Override
    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked.getHolder() != this || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        for (Map.Entry<LeaderboardSection, Integer> entry : slots.entrySet()) {
            if (entry.getValue() == event.getSlot()) {
                LeaderboardBoardMenu.open(plugin, player, entry.getKey(), 0);
                return;
            }
        }
    }
}
