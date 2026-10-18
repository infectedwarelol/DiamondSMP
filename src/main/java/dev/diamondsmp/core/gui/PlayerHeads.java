package dev.diamondsmp.core.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.service.Service;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import dev.diamondsmp.core.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerHeads implements Service {

    private final DiamondSMP plugin;
    private final Map<UUID, PlayerProfile> textured = new ConcurrentHashMap<>();
    private final Set<UUID> resolving = ConcurrentHashMap.newKeySet();

    public PlayerHeads(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    public ItemStack head(UUID uuid, String name, Component display, List<Component> lore) {
        ItemStack item = Items.create(Material.PLAYER_HEAD, display, lore, false);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            meta.setPlayerProfile(profile(uuid, name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private PlayerProfile profile(UUID uuid, String name) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getPlayerProfile();
        }
        PlayerProfile cached = textured.get(uuid);
        return cached != null ? cached : Bukkit.createProfile(uuid, name);
    }

    public void resolve(Collection<ProfileSnapshot> entries, Runnable done) {
        List<ProfileSnapshot> missing = new ArrayList<>();
        for (ProfileSnapshot entry : entries) {
            UUID uuid = entry.uuid();
            if (!textured.containsKey(uuid) && Bukkit.getPlayer(uuid) == null && resolving.add(uuid)) {
                missing.add(entry);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        plugin.scheduler().async(() -> {
            boolean changed = false;
            for (ProfileSnapshot entry : missing) {
                try {
                    PlayerProfile profile = Bukkit.createProfile(entry.uuid(), entry.name());
                    if (profile.complete(true) && profile.hasTextures()) {
                        textured.put(entry.uuid(), profile);
                        changed = true;
                    }
                } catch (RuntimeException ex) {
                    plugin.getLogger().fine("Could not fetch the skin of " + entry.name() + ": " + ex.getMessage());
                } finally {
                    resolving.remove(entry.uuid());
                }
            }
            if (changed) {
                done.run();
            }
        });
    }

    @Override
    public void shutdown() {
        textured.clear();
        resolving.clear();
    }
}
