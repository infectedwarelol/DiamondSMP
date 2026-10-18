package dev.diamondsmp.core.effect;

import dev.diamondsmp.core.scheduler.Scheduler;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class KillEffectRegistry {

    private static final Color[] FIREWORK_COLORS = {
            Color.AQUA, Color.BLUE, Color.FUCHSIA, Color.LIME, Color.ORANGE, Color.RED, Color.WHITE, Color.YELLOW
    };

    private final NamespacedKey fireworkKey;
    private final Map<String, KillEffect> effects = new LinkedHashMap<>();

    public KillEffectRegistry(NamespacedKey fireworkKey) {
        this.fireworkKey = fireworkKey;
        register("lightning", "&eLightning", Material.LIGHTNING_ROD,
                List.of("&7A harmless bolt strikes your victim."),
                (scheduler, at) -> at.getWorld().strikeLightningEffect(at));
        register("firework", "&dFirework Burst", Material.FIREWORK_ROCKET,
                List.of("&7A colorful firework bursts where", "&7your victim fell."),
                (scheduler, at) -> firework(at));
        register("flame_spiral", "&6Flame Spiral", Material.BLAZE_POWDER,
                List.of("&7A spiral of flames rises up."),
                KillEffectRegistry::flameSpiral);
        register("soul_rise", "&3Soul Rise", Material.SOUL_LANTERN,
                List.of("&7The victim's soul drifts away."),
                KillEffectRegistry::soulRise);
        register("blood_burst", "&4Blood Burst", Material.REDSTONE,
                List.of("&7A burst of crimson dust."),
                (scheduler, at) -> burst(at, Particles.DUST, 70, 0.45D, 0.0D, Color.fromRGB(150, 0, 0),
                        "entity.player.hurt", 1.0F));
        register("totem_pop", "&aTotem Pop", Material.TOTEM_OF_UNDYING,
                List.of("&7A totem pops, but too late."),
                (scheduler, at) -> burst(at, Particles.TOTEM, 90, 0.4D, 0.55D, Color.LIME, "item.totem.use", 1.0F));
        register("heart_burst", "&cHeart Burst", Material.POPPY,
                List.of("&7Hearts fly out of your victim."),
                (scheduler, at) -> burst(at, Particles.HEART, 14, 0.6D, 0.0D, Color.RED,
                        "entity.experience_orb.pickup", 1.2F));
        register("explosion_poof", "&7Explosion Poof", Material.TNT,
                List.of("&7A harmless puff of an explosion."),
                (scheduler, at) -> explosion(at));
        register("smoke_trail", "&8Smoke Trail", Material.CAMPFIRE,
                List.of("&7A column of smoke rises up."),
                KillEffectRegistry::smokeColumn);
        register("enchant_swirl", "&5Enchant Swirl", Material.ENCHANTING_TABLE,
                List.of("&7Arcane runes swirl around."),
                KillEffectRegistry::enchantSwirl);
        register("ender_burst", "&5Ender Burst", Material.ENDER_PEARL,
                List.of("&7Your victim is pulled into the void."),
                (scheduler, at) -> ender(at));
        register("water_splash", "&9Water Splash", Material.WATER_BUCKET,
                List.of("&7A big splash of water."),
                (scheduler, at) -> splash(at));
        register("note_burst", "&bNote Burst", Material.NOTE_BLOCK,
                List.of("&7A cheerful burst of music notes."),
                (scheduler, at) -> notes(at));
        register("cloud_puff", "&fCloud Puff", Material.WHITE_WOOL,
                List.of("&7Your victim vanishes into a cloud."),
                (scheduler, at) -> burst(at, Particles.CLOUD, 45, 0.4D, 0.04D, Color.WHITE, "block.wool.break", 0.8F));
        register("villager_joy", "&2Villager Joy", Material.EMERALD,
                List.of("&7The villagers celebrate your kill."),
                (scheduler, at) -> burst(at, Particles.HAPPY_VILLAGER, 50, 0.6D, 0.0D, Color.LIME,
                        "entity.villager.yes", 1.0F));
        register("witch_magic", "&5Witch Magic", Material.BREWING_STAND,
                List.of("&7A witch's curse lingers."),
                (scheduler, at) -> burst(at, Particles.WITCH, 70, 0.5D, 0.0D, Color.PURPLE,
                        "entity.witch.celebrate", 1.0F));
        register("crit_storm", "&eCrit Storm", Material.IRON_SWORD,
                List.of("&7A storm of critical hits."),
                (scheduler, at) -> crits(at));
        register("end_rod_burst", "&fStar Burst", Material.END_ROD,
                List.of("&7Glowing stars scatter outward."),
                (scheduler, at) -> burst(at, Particles.END_ROD, 60, 0.2D, 0.18D, Color.WHITE,
                        "entity.shulker.shoot", 1.3F));
        register("anvil_drop", "&7Anvil Drop", Material.ANVIL,
                List.of("&7A giant anvil crushes your victim."),
                (scheduler, at) -> burst(at, Particles.CLOUD, 40, 0.6D, 0.02D, Color.GRAY, "block.anvil.land", 0.8F));
        register("skull_rise", "&8Skull Rise", Material.WITHER_SKELETON_SKULL,
                List.of("&7A cursed skull rises from the body."),
                (scheduler, at) -> burst(at, Particles.LARGE_SMOKE, 45, 0.4D, 0.03D, Color.BLACK,
                        "entity.wither.ambient", 1.2F));
        register("diamond_rain", "&bDiamond Rain", Material.DIAMOND,
                List.of("&7A huge diamond shines above the kill."),
                (scheduler, at) -> burst(at, Particles.DUST, 80, 0.8D, 0.0D, Color.fromRGB(0x5CE1E6),
                        "block.amethyst_block.chime", 1.0F));
        register("dragon_roar", "&5Dragon Roar", Material.DRAGON_HEAD,
                List.of("&7The ender dragon claims the kill."),
                (scheduler, at) -> burst(at, Particles.PORTAL, 160, 0.6D, 1.0D, Color.PURPLE,
                        "entity.ender_dragon.growl", 1.0F));
        register("trident_storm", "&3Trident Storm", Material.TRIDENT,
                List.of("&7A giant trident calls down the storm."),
                (scheduler, at) -> tridentStorm(at));
        register("gold_rush", "&6Gold Rush", Material.GOLD_BLOCK,
                List.of("&7Your victim was worth their weight in gold."),
                (scheduler, at) -> burst(at, Particles.DUST, 70, 0.7D, 0.0D, Color.fromRGB(0xFFD24D),
                        "entity.player.levelup", 1.2F));
        register("pumpkin_pop", "&6Pumpkin Pop", Material.JACK_O_LANTERN,
                List.of("&7A spooky pumpkin pops out."),
                (scheduler, at) -> burst(at, Particles.FLAME, 50, 0.5D, 0.05D, Color.ORANGE,
                        "entity.witch.celebrate", 0.9F));
        register("royal_crown", "&eRoyal Crown", Material.GOLDEN_HELMET,
                List.of("&7Crown yourself after the kill."),
                (scheduler, at) -> burst(at, Particles.END_ROD, 40, 0.5D, 0.05D, Color.YELLOW,
                        "ui.toast.challenge_complete", 1.0F));
    }

    private static void tridentStorm(Location at) {
        World world = at.getWorld();
        if (world != null) {
            world.strikeLightningEffect(at);
        }
        burst(at, Particles.SPLASH, 90, 0.6D, 0.3D, Color.AQUA, "item.trident.thunder", 1.0F);
    }

    private void register(String id, String name, Material icon, List<String> description,
                          KillEffect.Animation animation) {
        effects.put(id, new KillEffect(id, name, icon, description, animation));
    }

    public Collection<KillEffect> all() {
        return Collections.unmodifiableCollection(effects.values());
    }

    public NamespacedKey fireworkKey() {
        return fireworkKey;
    }

    private static Location center(Location at) {
        return at.clone().add(0.0D, 1.0D, 0.0D);
    }

    private static void sound(Location at, String key, float pitch) {
        World world = at.getWorld();
        if (world != null) {
            world.playSound(at, key, SoundCategory.PLAYERS, 1.0F, pitch);
        }
    }

    private static void burst(Location at, Particle particle, int count, double spread, double speed,
                              Color color, String sound, float pitch) {
        Location center = center(at);
        Particles.spawn(at.getWorld(), particle, center, count, spread, spread, spread, speed, color);
        sound(at, sound, pitch);
    }

    private void firework(Location at) {
        World world = at.getWorld();
        if (world == null) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Firework firework = world.spawn(center(at), Firework.class);
        firework.getPersistentDataContainer().set(fireworkKey, PersistentDataType.BYTE, (byte) 1);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(FIREWORK_COLORS[random.nextInt(FIREWORK_COLORS.length)],
                        FIREWORK_COLORS[random.nextInt(FIREWORK_COLORS.length)])
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(0);
        firework.setFireworkMeta(meta);
        firework.detonate();
    }

    private static void flameSpiral(Scheduler scheduler, Location at) {
        sound(at, "entity.blaze.shoot", 1.0F);
        scheduler.regionRepeat(at, 1L, 24, step -> {
            double angle = step * 0.55D;
            double height = step * 0.09D;
            for (int strand = 0; strand < 2; strand++) {
                double offset = angle + Math.PI * strand;
                Location point = at.clone().add(Math.cos(offset) * 0.8D, height, Math.sin(offset) * 0.8D);
                Particles.spawn(at.getWorld(), Particles.FLAME, point, 2, 0.02D, 0.0D);
            }
        });
    }

    private static void soulRise(Scheduler scheduler, Location at) {
        sound(at, "particle.soul_escape", 0.8F);
        scheduler.regionRepeat(at, 2L, 14, step -> {
            Location point = at.clone().add(0.0D, 0.2D + step * 0.18D, 0.0D);
            Particles.spawn(at.getWorld(), Particles.SOUL, point, 4, 0.25D, 0.02D);
            Particles.spawn(at.getWorld(), Particles.SOUL_FLAME, point, 2, 0.15D, 0.01D);
        });
    }

    private static void explosion(Location at) {
        Location center = center(at);
        Particles.spawn(at.getWorld(), Particles.EXPLOSION, center, 1, 0.0D, 0.0D);
        Particles.spawn(at.getWorld(), Particles.POOF, center, 35, 0.5D, 0.05D);
        World world = at.getWorld();
        if (world != null) {
            world.playSound(at, "entity.generic.explode", SoundCategory.PLAYERS, 0.6F, 1.2F);
        }
    }

    private static void smokeColumn(Scheduler scheduler, Location at) {
        sound(at, "block.fire.extinguish", 0.9F);
        scheduler.regionRepeat(at, 1L, 20, step -> {
            Location point = at.clone().add(0.0D, step * 0.15D, 0.0D);
            Particles.spawn(at.getWorld(), Particles.LARGE_SMOKE, point, 3, 0.12D, 0.01D);
        });
    }

    private static void enchantSwirl(Scheduler scheduler, Location at) {
        sound(at, "block.enchantment_table.use", 1.0F);
        scheduler.regionRepeat(at, 1L, 20, step -> {
            double base = step * 0.35D;
            for (int point = 0; point < 6; point++) {
                double angle = base + point * (Math.PI / 3.0D);
                Location spot = at.clone().add(Math.cos(angle) * 1.2D, 0.6D + step * 0.05D, Math.sin(angle) * 1.2D);
                Particles.spawn(at.getWorld(), Particles.ENCHANT, spot, 3, 0.05D, 0.4D);
            }
        });
    }

    private static void ender(Location at) {
        Location center = center(at);
        Particles.spawn(at.getWorld(), Particles.PORTAL, center, 140, 0.5D, 1.0D);
        Particles.spawn(at.getWorld(), Particles.REVERSE_PORTAL, center, 45, 0.3D, 0.05D);
        sound(at, "entity.enderman.teleport", 1.0F);
    }

    private static void splash(Location at) {
        Location center = center(at);
        Particles.spawn(at.getWorld(), Particles.SPLASH, center, 90, 0.6D, 0.3D);
        Particles.spawn(at.getWorld(), Particles.FALLING_WATER, center.clone().add(0.0D, 0.8D, 0.0D),
                40, 0.6D, 0.0D);
        sound(at, "entity.generic.splash", 1.0F);
    }

    private static void notes(Location at) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location center = center(at);
        for (int index = 0; index < 16; index++) {
            Location spot = center.clone().add(random.nextDouble(-0.9D, 0.9D), random.nextDouble(0.0D, 1.2D),
                    random.nextDouble(-0.9D, 0.9D));
            Particles.spawn(at.getWorld(), Particles.NOTE, spot, 1, 0.0D, 0.0D, 0.0D, random.nextDouble(),
                    Color.WHITE);
        }
        sound(at, "block.note_block.pling", 1.4F);
    }

    private static void crits(Location at) {
        Location center = center(at);
        Particles.spawn(at.getWorld(), Particles.CRIT, center, 60, 0.5D, 0.4D);
        Particles.spawn(at.getWorld(), Particles.ENCHANTED_HIT, center, 40, 0.5D, 0.4D);
        sound(at, "entity.player.attack.crit", 1.0F);
    }
}
