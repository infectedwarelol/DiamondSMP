package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.message.Text;
import dev.diamondsmp.core.storage.PlayerProfile;
import dev.diamondsmp.core.util.Format;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class StatsService implements Service {

    private final DiamondSMP plugin;
    private volatile boolean customMessages = true;

    public StatsService(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        customMessages = plugin.settings().getBoolean("ffa.custom-death-messages", true);
    }

    public void handleDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        PlayerProfile victimProfile = plugin.profiles().profile(victim);
        int endedStreak = victimProfile == null ? 0 : victimProfile.recordDeath();
        plugin.teleports().rememberBack(victim, victim.getLocation());

        if (killer == null || killer.equals(victim)) {
            if (customMessages) {
                Component vanilla = event.deathMessage();
                event.deathMessage(plugin.messages().get("death.message",
                        "victim", victim.getName(),
                        "vanilla", vanilla == null ? Component.empty() : vanilla));
            }
            plugin.killStreaks().ended(victim, null, endedStreak);
            return;
        }

        PlayerProfile killerProfile = plugin.profiles().profile(killer);
        int streak = killerProfile == null ? 0 : killerProfile.recordKill();
        if (customMessages) {
            event.deathMessage(plugin.messages().get("kill.message",
                    "killer", killer.getName(),
                    "victim", victim.getName(),
                    "streak", streak,
                    "weapon", weapon(killer),
                    "health", Format.decimal(killer.getHealth() / 2.0D)));
        }
        plugin.killStreaks().killed(killer, streak);
        plugin.killStreaks().ended(victim, killer, endedStreak);
        plugin.killEffects().play(killer, victim.getLocation());
    }

    private String weapon(Player killer) {
        if (!Bukkit.isOwnedByCurrentRegion(killer)) {
            return plain("kill.ranged-weapon");
        }
        ItemStack item = killer.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            return plain("kill.no-weapon");
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return Text.plain(meta.displayName());
        }
        return Format.pretty(item.getType().name());
    }

    private String plain(String key) {
        Component component = plugin.messages().get(key);
        return component == null ? "" : Text.plain(component);
    }
}
