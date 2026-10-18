package dev.diamondsmp.core.effect;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public record Corpse(String name, ItemStack head, ItemStack chest, ItemStack legs, ItemStack boots, ItemStack weapon) {

    private static final Color SHIRT = Color.fromRGB(0x3B3B3B);
    private static final Color TROUSERS = Color.fromRGB(0x2E3B5E);
    private static final Color SHOES = Color.fromRGB(0x2A2A2A);

    public static Corpse of(Player victim) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            PlayerProfile profile = victim.getPlayerProfile();
            meta.setPlayerProfile(profile);
            head.setItemMeta(meta);
        }
        ItemStack[] armor = victim.getInventory().getArmorContents();
        ItemStack weapon = victim.getInventory().getItemInMainHand();
        return new Corpse(victim.getName(), head,
                worn(armor, 2, Material.LEATHER_CHESTPLATE, SHIRT),
                worn(armor, 1, Material.LEATHER_LEGGINGS, TROUSERS),
                worn(armor, 0, Material.LEATHER_BOOTS, SHOES),
                weapon.getType().isAir() ? null : weapon.clone());
    }

    private static ItemStack worn(ItemStack[] armor, int slot, Material fallback, Color color) {
        ItemStack piece = slot < armor.length ? armor[slot] : null;
        if (piece != null && !piece.getType().isAir()) {
            return piece.clone();
        }
        ItemStack plain = new ItemStack(fallback);
        if (plain.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            plain.setItemMeta(meta);
        }
        return plain;
    }
}
