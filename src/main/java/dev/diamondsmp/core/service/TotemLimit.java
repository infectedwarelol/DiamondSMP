package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TotemLimit implements Service {

    public static final String BYPASS_PERMISSION = "diamondsmp.totem.bypass";

    private final DiamondSMP plugin;
    private final Set<UUID> queued = ConcurrentHashMap.newKeySet();
    private volatile int limit = 3;

    public TotemLimit(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        limit = plugin.settings().getInt("totems.limit", 3);
    }

    public void check(Player player) {
        if (limit < 0 || player.hasPermission(BYPASS_PERMISSION) || !queued.add(player.getUniqueId())) {
            return;
        }
        plugin.scheduler().entityLater(player, () -> {
            queued.remove(player.getUniqueId());
            if (player.isOnline()) {
                enforce(player);
            }
        }, 1L);
    }

    private void enforce(Player player) {
        PlayerInventory inventory = player.getInventory();
        int max = limit;
        int total = count(inventory.getItemInOffHand());
        ItemStack[] storage = inventory.getStorageContents();
        for (ItemStack item : storage) {
            total += count(item);
        }
        int excess = total - max;
        if (excess <= 0) {
            return;
        }
        int removed = 0;
        for (int slot = storage.length - 1; slot >= 0 && removed < excess; slot--) {
            ItemStack item = storage[slot];
            if (count(item) == 0) {
                continue;
            }
            int take = Math.min(item.getAmount(), excess - removed);
            removed += take;
            if (take == item.getAmount()) {
                storage[slot] = null;
            } else {
                item.setAmount(item.getAmount() - take);
            }
        }
        if (removed < excess && count(inventory.getItemInOffHand()) > 0) {
            ItemStack offhand = inventory.getItemInOffHand();
            int take = Math.min(offhand.getAmount(), excess - removed);
            removed += take;
            offhand.setAmount(offhand.getAmount() - take);
            inventory.setItemInOffHand(offhand.getAmount() <= 0 ? null : offhand);
        }
        inventory.setStorageContents(storage);
        int dropped = 0;
        Location location = player.getLocation();
        for (ItemStack leftover : player.getEnderChest().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, removed)).values()) {
            dropped += leftover.getAmount();
            player.getWorld().dropItem(location, leftover);
        }
        plugin.messages().error(player, dropped > 0 ? "totems.dropped" : "totems.stored",
                "limit", max, "removed", removed, "stored", removed - dropped, "dropped", dropped);
    }

    private static int count(ItemStack item) {
        return item != null && item.getType() == Material.TOTEM_OF_UNDYING ? item.getAmount() : 0;
    }

    public void quit(Player player) {
        queued.remove(player.getUniqueId());
    }
}
