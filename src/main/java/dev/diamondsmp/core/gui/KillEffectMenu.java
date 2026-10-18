package dev.diamondsmp.core.gui;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.message.Messages;
import dev.diamondsmp.core.message.Text;
import dev.diamondsmp.core.service.KillEffectService;
import dev.diamondsmp.core.storage.PlayerProfile;
import dev.diamondsmp.core.util.Items;
import dev.diamondsmp.core.util.Lookup;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class KillEffectMenu extends Menu {

    private static final int ROWS = 6;
    private static final int PAGE_SIZE = (ROWS - 1) * 9;
    private static final int PREVIOUS_SLOT = PAGE_SIZE + 3;
    private static final int CLOSE_SLOT = PAGE_SIZE + 4;
    private static final int NEXT_SLOT = PAGE_SIZE + 5;
    private static final int VIEW_SLOT = PAGE_SIZE + 8;

    private final DiamondSMP plugin;
    private final Player viewer;
    private final List<String> entries;
    private int page;

    public KillEffectMenu(DiamondSMP plugin, Player viewer, int page) {
        super(ROWS, plugin.messages().display("menus.killeffect.title"));
        this.plugin = plugin;
        this.viewer = viewer;
        this.entries = plugin.killEffects().ids();
        this.page = Math.max(0, Math.min(page, pages() - 1));
        render();
    }

    private int pages() {
        return Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void render() {
        Inventory inventory = getInventory();
        inventory.clear();
        KillEffectService effects = plugin.killEffects();
        String selected = effects.selected(viewer);
        int start = page * PAGE_SIZE;
        int shown = Math.min(PAGE_SIZE, entries.size() - start);
        for (int offset = 0; offset < shown; offset++) {
            inventory.setItem(offset, entryItem(effects, entries.get(start + offset), selected));
        }
        Messages messages = plugin.messages();
        int pages = pages();
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, Items.create(Material.ARROW,
                    messages.display("menus.killeffect.previous", "page", page, "pages", pages), null, false));
        }
        inventory.setItem(CLOSE_SLOT, Items.create(Material.BARRIER,
                messages.display("menus.killeffect.close"),
                messages.lore("menus.killeffect.close-lore", "page", page + 1, "pages", pages), false));
        if (page + 1 < pages) {
            inventory.setItem(NEXT_SLOT, Items.create(Material.ARROW,
                    messages.display("menus.killeffect.next", "page", page + 2, "pages", pages), null, false));
        }
        boolean hidden = effects.hidesEffects(viewer);
        inventory.setItem(VIEW_SLOT, Items.create(hidden ? Material.ENDER_PEARL : Material.ENDER_EYE,
                messages.display(hidden ? "menus.killeffect.view-off" : "menus.killeffect.view-on"),
                messages.lore("menus.killeffect.view-lore"), !hidden));
        ItemStack filler = Items.create(Lookup.material(plugin.settings().getString("kill-effects.filler"),
                Material.GRAY_STAINED_GLASS_PANE), Component.text(" "), null, false);
        for (int slot = PAGE_SIZE; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler.clone());
            }
        }
    }

    private ItemStack entryItem(KillEffectService effects, String id, String selected) {
        Messages messages = plugin.messages();
        boolean isSelected = id.equals(selected);
        if (KillEffectService.NONE.equals(id)) {
            List<Component> lore = new ArrayList<>(messages.lore("menus.killeffect.none-lore"));
            lore.addAll(messages.lore(isSelected ? "menus.killeffect.selected-lore" : "menus.killeffect.select-lore"));
            return Items.create(effects.noneIcon(), messages.display("menus.killeffect.none-name"), lore, isSelected);
        }
        if (KillEffectService.RANDOM.equals(id)) {
            List<Component> lore = new ArrayList<>(messages.lore("menus.killeffect.random-lore"));
            if (effects.isLocked(viewer, id)) {
                lore.addAll(messages.lore("menus.killeffect.locked-lore"));
                return Items.create(effects.lockedIcon(), messages.display("menus.killeffect.locked-name",
                        "effect", effects.displayName(id)), lore, false);
            }
            lore.addAll(messages.lore(isSelected ? "menus.killeffect.selected-lore" : "menus.killeffect.select-lore"));
            return Items.create(effects.randomIcon(), effects.displayName(id), lore, isSelected);
        }
        KillEffectService.Configured effect = effects.find(id);
        Component name = Text.display(messages.theme(effect.name()));
        List<Component> lore = new ArrayList<>(Text.lore(effect.description()));
        if (effects.isLocked(viewer, id)) {
            lore.addAll(messages.lore("menus.killeffect.locked-lore"));
            return Items.create(effects.lockedIcon(), messages.display("menus.killeffect.locked-name",
                    "effect", name), lore, false);
        }
        lore.addAll(messages.lore(isSelected ? "menus.killeffect.selected-lore" : "menus.killeffect.select-lore"));
        return Items.create(effect.icon(), name, lore, isSelected);
    }

    @Override
    public void click(InventoryClickEvent event) {
        event.setCancelled(true);
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked.getHolder() != this || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getSlot();
        if (slot == PREVIOUS_SLOT && page > 0) {
            page--;
            render();
            return;
        }
        if (slot == NEXT_SLOT && page + 1 < pages()) {
            page++;
            render();
            return;
        }
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot == VIEW_SLOT) {
            PlayerProfile profile = plugin.profiles().profile(player);
            if (profile == null) {
                plugin.messages().error(player, "general.data-loading");
                return;
            }
            profile.hidesEffects(!profile.hidesEffects());
            plugin.messages().send(player, profile.hidesEffects() ? "killeffect.view-off" : "killeffect.view-on");
            Lookup.sound(player, plugin.settings().getString("kill-effects.sounds.select"), 1.0F, 1.0F);
            render();
            return;
        }
        int index = page * PAGE_SIZE + slot;
        if (slot >= PAGE_SIZE || index >= entries.size()) {
            return;
        }
        String id = entries.get(index);
        KillEffectService effects = plugin.killEffects();
        if (effects.isLocked(player, id)) {
            plugin.messages().error(player, "killeffect.locked");
            Lookup.sound(player, plugin.settings().getString("kill-effects.sounds.locked"), 1.0F, 0.8F);
            return;
        }
        if (id.equals(effects.selected(player))) {
            return;
        }
        if (effects.select(player, id)) {
            plugin.messages().send(player, "killeffect.selected", "effect", effects.displayName(id));
            Lookup.sound(player, plugin.settings().getString("kill-effects.sounds.select"), 1.0F, 1.2F);
            render();
        } else {
            plugin.messages().error(player, "general.data-loading");
        }
    }
}
