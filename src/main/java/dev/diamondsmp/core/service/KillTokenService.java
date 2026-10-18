package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.message.Messages;
import dev.diamondsmp.core.message.Text;
import dev.diamondsmp.core.util.Items;
import dev.diamondsmp.core.util.Lookup;
import dev.diamondsmp.core.util.SoundSpec;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KillTokenService implements Service {

    public enum Kind {
        TOKEN,
        COMPRESSED;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Kind parse(String input) {
            if (input == null) {
                return null;
            }
            for (Kind kind : values()) {
                if (kind.id().equalsIgnoreCase(input)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private final DiamondSMP plugin;
    private final NamespacedKey key;
    private volatile boolean enabled = true;
    private volatile int perKill = 1;
    private volatile Map<Kind, ItemStack> templates = Map.of();
    private volatile SoundSpec rewardSound = SoundSpec.NONE;

    public KillTokenService(DiamondSMP plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "kill_token");
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        enabled = config.getBoolean("kill-tokens.enabled", true);
        perKill = Math.max(0, config.getInt("kill-tokens.per-kill", 1));
        templates = Map.of(
                Kind.TOKEN, build(Kind.TOKEN, "kill-tokens.token.", Material.GOLD_NUGGET),
                Kind.COMPRESSED, build(Kind.COMPRESSED, "kill-tokens.compressed.", Material.NETHER_STAR));
        rewardSound = SoundSpec.from(config.getConfigurationSection("kill-tokens.sound"));
    }

    private ItemStack build(Kind kind, String path, Material fallback) {
        FileConfiguration config = plugin.settings();
        Messages messages = plugin.messages();
        Material material = Lookup.material(config.getString(path + "material"), fallback);
        Component name = Text.display(messages.theme(config.getString(path + "name", "")));
        List<Component> lore = new ArrayList<>();
        for (String line : config.getStringList(path + "lore")) {
            lore.add(Text.display(messages.theme(line)));
        }
        ItemStack item = Items.create(material.isItem() ? material : fallback, name, lore,
                config.getBoolean(path + "glow", true));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, kind.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack create(Kind kind, int amount) {
        ItemStack item = templates.get(kind).clone();
        item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), amount)));
        return item;
    }

    public boolean isToken(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public void reward(Player killer) {
        int amount = perKill;
        if (!enabled || amount <= 0) {
            return;
        }
        plugin.scheduler().entity(killer, () -> {
            if (!killer.isOnline()) {
                return;
            }
            boolean dropped = give(killer, Kind.TOKEN, amount);
            plugin.messages().send(killer, dropped ? "kill-tokens.reward-dropped" : "kill-tokens.reward",
                    "amount", amount);
            rewardSound.play(killer);
        });
    }

    public boolean give(Player player, Kind kind, int amount) {
        boolean dropped = false;
        int remaining = amount;
        Location location = player.getLocation();
        while (remaining > 0) {
            ItemStack stack = create(kind, remaining);
            remaining -= stack.getAmount();
            for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
                player.getWorld().dropItemNaturally(location, leftover);
                dropped = true;
            }
        }
        return dropped;
    }
}
