package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.storage.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConnectionListener implements Listener {

    private final DiamondSMP plugin;

    public ConnectionListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = plugin.profiles().profile(player);
        boolean firstJoin = profile != null && profile.isFirstJoin();
        if (plugin.settings().getBoolean("join-quit.enabled", true)) {
            event.joinMessage(plugin.messages().get(firstJoin ? "join.first" : "join.message",
                    "player", player.getName()));
        }
        boolean toSpawn = firstJoin ? plugin.spawn().teleportOnFirstJoin() : plugin.spawn().teleportOnJoin();
        if (toSpawn && plugin.spawn().isSet()) {
            plugin.scheduler().entityLater(player, () -> plugin.spawn().send(player), 1L);
        }
        plugin.visibility().joined(player);
        plugin.reactiveDistance().apply(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.settings().getBoolean("join-quit.enabled", true)) {
            event.quitMessage(plugin.messages().get("quit.message", "player", event.getPlayer().getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.reactiveDistance().apply(event.getPlayer());
    }
}
