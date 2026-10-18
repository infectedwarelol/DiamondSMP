package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatService implements Service {

    public static final String MUTE_BYPASS = "diamondsmp.mutechat.bypass";
    public static final String CLEAR_BYPASS = "diamondsmp.clearchat.bypass";
    public static final String IGNORE_BYPASS = "diamondsmp.ignore.bypass";
    public static final String SOCIALSPY_PERMISSION = "diamondsmp.socialspy";

    public enum Delivery {
        SENT,
        SELF,
        IGNORED,
        BLOCKED,
        BLOCKED_BY_YOU
    }

    private final DiamondSMP plugin;
    private final AtomicBoolean muted = new AtomicBoolean();
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();
    private volatile int clearLines = 100;

    public ChatService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        clearLines = Math.max(1, Math.min(500, plugin.settings().getInt("chat.clear-lines", 100)));
    }

    public Delivery sendPrivate(CommandSender sender, Player target, String message) {
        if (sender instanceof Player player) {
            if (player.equals(target)) {
                return Delivery.SELF;
            }
            Delivery refused = refusal(player, target);
            if (refused != null) {
                return refused;
            }
            lastPartner.put(player.getUniqueId(), target.getUniqueId());
            lastPartner.put(target.getUniqueId(), player.getUniqueId());
        }
        String senderName = sender instanceof Player player ? player.getName() : "Console";
        plugin.messages().send(sender, "msg.to", "player", target.getName(), "message", message);
        plugin.messages().send(target, "msg.from", "player", senderName, "message", message);
        Component spy = plugin.messages().get("msg.spy", "sender", senderName, "target", target.getName(),
                "message", message);
        if (spy == null) {
            return Delivery.SENT;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender) || online.equals(target) || !online.hasPermission(SOCIALSPY_PERMISSION)) {
                continue;
            }
            PlayerProfile profile = plugin.profiles().profile(online);
            if (profile != null && profile.socialSpy()) {
                online.sendMessage(spy);
            }
        }
        return Delivery.SENT;
    }

    private Delivery refusal(Player sender, Player target) {
        PlayerProfile senderProfile = plugin.profiles().profile(sender);
        if (senderProfile != null && senderProfile.isBlocking(target.getUniqueId())) {
            return Delivery.BLOCKED_BY_YOU;
        }
        if (sender.hasPermission(IGNORE_BYPASS)) {
            return null;
        }
        PlayerProfile targetProfile = plugin.profiles().profile(target);
        if (targetProfile == null) {
            return null;
        }
        if (targetProfile.isBlocking(sender.getUniqueId())) {
            return Delivery.BLOCKED;
        }
        return targetProfile.isIgnoring(sender.getUniqueId()) ? Delivery.IGNORED : null;
    }

    public void dropConversation(Player first, Player second) {
        lastPartner.remove(first.getUniqueId(), second.getUniqueId());
        lastPartner.remove(second.getUniqueId(), first.getUniqueId());
    }

    public Player replyTarget(Player player) {
        UUID partner = lastPartner.get(player.getUniqueId());
        return partner == null ? null : Bukkit.getPlayer(partner);
    }

    public boolean toggleMute() {
        boolean now = !muted.get();
        muted.set(now);
        return now;
    }

    public boolean isMuted() {
        return muted.get();
    }

    public void clearChat() {
        Component blank = Component.text("\n".repeat(clearLines));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(CLEAR_BYPASS)) {
                online.sendMessage(blank);
            }
        }
    }

    public void quit(Player player) {
        lastPartner.remove(player.getUniqueId());
    }

    @Override
    public void shutdown() {
        lastPartner.clear();
        muted.set(false);
    }
}
