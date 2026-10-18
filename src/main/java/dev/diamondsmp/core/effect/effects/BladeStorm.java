package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;

import java.util.List;

public final class BladeStorm implements KillEffect {

    private static final int SWORDS = 8;
    private static final int ORBIT_STEPS = 14;
    private static final Material[] BLADES = {Material.NETHERITE_SWORD, Material.DIAMOND_SWORD};

    @Override
    public String id() {
        return "blade_storm";
    }

    @Override
    public String name() {
        return "&b&lBlade Storm";
    }

    @Override
    public Material icon() {
        return Material.NETHERITE_SWORD;
    }

    @Override
    public List<String> description() {
        return List.of("&7A storm of swords circles faster and", "&7faster, then rains down on the body.");
    }

    @Override
    public void play(EffectSession s) {
        ItemDisplay[] swords = new ItemDisplay[SWORDS];
        s.at(0, () -> {
            s.corpse(CorpsePose.SLUMPED, 0, 0, (float) s.random(0, 360));
            for (int index = 0; index < SWORDS; index++) {
                swords[index] = s.item(BLADES[index % BLADES.length], orbit(index, 0, 0.01D));
            }
            s.sound("item.armor.equip_netherite", 1.0F, 0.6F);
        });
        s.every(1, 2, ORBIT_STEPS, step -> {
            int phase = step + 1;
            for (int index = 0; index < SWORDS; index++) {
                s.move(swords[index], orbit(index, phase, 1.3D), 2);
            }
            double radius = 3.0D - phase * 0.06D;
            double height = 1.2D + phase * 0.12D;
            double angle = phase * (0.25D + phase * 0.02D);
            s.particle(Particles.SWEEP, Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            s.particle(Particles.CRIT, Math.cos(angle + Math.PI) * radius, height, Math.sin(angle + Math.PI) * radius,
                    4, 0.1D, 0.1D);
            s.sound("entity.player.attack.sweep", 0.7F, 0.6F + phase * 0.08F);
        });
        s.at(30, () -> {
            for (int index = 0; index < SWORDS; index++) {
                double angle = EffectMath.TAU * index / SWORDS;
                s.move(swords[index], EffectMath.item(Math.cos(angle) * 0.6D, 0.55D, Math.sin(angle) * 0.6D, 1.4D,
                        EffectMath.spin(-angle, 0, Math.PI * 0.75D)), 4);
            }
            s.sound("item.trident.throw", 1.0F, 0.8F);
        });
        s.at(34, () -> {
            s.particle(Particles.CRIT, 0, 0.5D, 0, 50, 0.6D, 0.4D);
            s.particle(Particles.ENCHANTED_HIT, 0, 0.5D, 0, 30, 0.6D, 0.4D);
            s.particle(Particles.EXPLOSION, 0, 0.5D, 0, 1, 0.0D, 0.0D);
            s.sound("item.trident.hit_ground", 1.0F, 0.7F);
            s.sound("item.trident.hit_ground", 1.0F, 1.0F);
            s.sound("entity.player.attack.crit", 1.0F, 0.8F);
            s.sound("block.anvil.land", 0.6F, 1.6F);
            s.shake();
        });
        s.every(35, 1, 12, step -> {
            double radius = 0.8D + EffectMath.easeOut(step / 12.0D) * 4.5D;
            s.ring(Particles.SWEEP, 0.3D, radius, 10, step * 0.2D, null, 1.0F);
        });
        s.at(48, () -> {
            for (int index = 0; index < SWORDS; index++) {
                double angle = EffectMath.TAU * index / SWORDS;
                s.move(swords[index], EffectMath.item(Math.cos(angle) * 0.6D, 0.2D, Math.sin(angle) * 0.6D, 0.01D,
                        EffectMath.spin(-angle, 0, Math.PI * 0.75D)), 6);
            }
            s.sound("block.amethyst_block.break", 0.8F, 1.6F);
        });
    }

    private static Transformation orbit(int index, int phase, double scale) {
        double angle = EffectMath.TAU * index / SWORDS + phase * (0.25D + phase * 0.02D);
        double radius = 3.0D - phase * 0.06D;
        double height = 1.2D + phase * 0.12D;
        return EffectMath.item(Math.cos(angle) * radius, height, Math.sin(angle) * radius, scale,
                EffectMath.spin(-angle + Math.PI / 2.0D, 0, Math.PI / 4.0D));
    }
}
