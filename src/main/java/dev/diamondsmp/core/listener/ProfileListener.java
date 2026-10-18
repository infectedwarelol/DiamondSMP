package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.storage.StorageException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

public final class ProfileListener implements Listener {

    private final DiamondSMP plugin;

    public ProfileListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        try {
            plugin.profiles().loadBlocking(event.getUniqueId(), event.getName());
        } catch (StorageException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not load the profile of " + event.getName(), ex);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, plugin.messages().get("general.load-failed"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLoginResult(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.profiles().forget(event.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.profiles().loadLater(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.teleports().quit(player);
        plugin.chat().quit(player);
        plugin.god().quit(player);
        plugin.visibility().quit(player);
        plugin.profiles().unload(player);
    }
}
