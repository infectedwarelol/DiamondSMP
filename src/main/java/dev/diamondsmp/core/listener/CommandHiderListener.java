package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.command.UnknownCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

public final class CommandHiderListener implements Listener {

    private final DiamondSMP plugin;

    public CommandHiderListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandList(PlayerCommandSendEvent event) {
        plugin.commandHider().filter(event.getPlayer(), event.getCommands());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUnknown(UnknownCommandEvent event) {
        Component message = plugin.commandHider().unknown(event.getSender());
        if (message != null) {
            event.message(message);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        int space = message.indexOf(' ');
        String label = message.substring(1, space < 0 ? message.length() : space);
        Player player = event.getPlayer();
        if (plugin.commandHider().blocks(player, label)) {
            event.setCancelled(true);
            plugin.commandHider().deny(player);
        }
    }
}
