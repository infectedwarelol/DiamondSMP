package dev.diamondsmp.core.service;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GodService implements Service {

    private final Set<UUID> gods = ConcurrentHashMap.newKeySet();

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (gods.remove(uuid)) {
            return false;
        }
        gods.add(uuid);
        return true;
    }

    public boolean isGod(Player player) {
        return gods.contains(player.getUniqueId());
    }

    public void quit(Player player) {
        gods.remove(player.getUniqueId());
    }

    @Override
    public void shutdown() {
        gods.clear();
    }
}
