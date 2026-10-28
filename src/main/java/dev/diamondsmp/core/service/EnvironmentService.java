package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.scheduler.Task;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class EnvironmentService implements Service {

    private static final long SWEEP_PERIOD_TICKS = 100L;
    private static final PotionEffect NIGHT_VISION = new PotionEffect(PotionEffectType.NIGHT_VISION,
            PotionEffect.INFINITE_DURATION, 0, true, false, false);

    private final DiamondSMP plugin;
    private final NamespacedKey nightVisionKey;
    private Task sweep = Task.NONE;

    public EnvironmentService(DiamondSMP plugin) {
        this.plugin = plugin;
        this.nightVisionKey = new NamespacedKey(plugin, "nightvision");
    }

    @Override
    public synchronized void reload() {
        plugin.scheduler().global(this::enforceDifficulty);
        sweep.cancel();
        sweep = plugin.scheduler().globalTimer(this::sweep, 1L, SWEEP_PERIOD_TICKS);
    }

    @Override
    public synchronized void shutdown() {
        sweep.cancel();
        sweep = Task.NONE;
    }

    public boolean nightVision(Player player) {
        Byte stored = player.getPersistentDataContainer().get(nightVisionKey, PersistentDataType.BYTE);
        return stored == null || stored != 0;
    }

    public boolean toggleNightVision(Player player) {
        boolean enabled = !nightVision(player);
        player.getPersistentDataContainer().set(nightVisionKey, PersistentDataType.BYTE, enabled ? (byte) 1 : (byte) 0);
        if (enabled) {
            player.addPotionEffect(NIGHT_VISION);
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
        return enabled;
    }

    public void refreshNightVision(Player player) {
        if (player.isOnline() && !player.isDead() && nightVision(player)
                && !player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(NIGHT_VISION);
        }
    }

    public void refreshNightVisionLater(Player player) {
        plugin.scheduler().entityLater(player, () -> refreshNightVision(player), 1L);
    }

    public void enforceDifficulty(World world) {
        if (world.getDifficulty() != Difficulty.HARD) {
            world.setDifficulty(Difficulty.HARD);
        }
    }

    private void enforceDifficulty() {
        for (World world : Bukkit.getWorlds()) {
            enforceDifficulty(world);
        }
    }

    private void sweep() {
        enforceDifficulty();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Bukkit.isOwnedByCurrentRegion(player)) {
                refreshNightVision(player);
            } else {
                plugin.scheduler().entity(player, () -> refreshNightVision(player));
            }
        }
    }
}
