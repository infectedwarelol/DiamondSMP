package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class FfaRules implements Service {

    private final DiamondSMP plugin;
    private volatile boolean keepInventory;
    private volatile boolean noHunger = true;
    private volatile boolean autoRespawn = true;

    public FfaRules(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.settings();
        keepInventory = config.getBoolean("ffa.keep-inventory", false);
        noHunger = config.getBoolean("ffa.no-hunger", true);
        autoRespawn = config.getBoolean("ffa.auto-respawn", true);
        plugin.scheduler().global(() -> {
            for (World world : Bukkit.getWorlds()) {
                apply(world);
            }
        });
    }

    public void apply(World world) {
        if (autoRespawn) {
            plugin.scheduler().global(() -> world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true));
        }
    }

    public boolean noHunger() {
        return noHunger;
    }

    public void applyDeath(PlayerDeathEvent event) {
        if (!keepInventory) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
}
