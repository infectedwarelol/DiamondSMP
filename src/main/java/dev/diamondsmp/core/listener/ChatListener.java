package dev.diamondsmp.core.listener;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.service.ChatService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {

    private final DiamondSMP plugin;

    public ChatListener(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (plugin.chat().isMuted() && !event.getPlayer().hasPermission(ChatService.MUTE_BYPASS)) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "chat.muted");
        }
    }
}
