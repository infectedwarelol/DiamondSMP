package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.List;

public final class FrozenShatter implements KillEffect {

    private static final int SPIKES = 8;
    private static final Material[] ICE = {Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE};
    private static final Color FROST = Color.fromRGB(0xBFEFFF);

    @Override
    public String id() {
        return "frozen_shatter";
    }

    @Override
    public String name() {
        return "&b&lFrozen Shatter";
    }

    @Override
    public Material icon() {
        return Material.BLUE_ICE;
    }

    @Override
    public List<String> description() {
        return List.of("&7Ice spikes erupt, your victim freezes", "&7solid and bursts into frost.");
    }

    @Override
    public void play(EffectSession s) {
        BlockDisplay[] spikes = new BlockDisplay[SPIKES];
        BlockDisplay[] coffin = new BlockDisplay[1];
        s.at(0, () -> {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int distance = dx * dx + dz * dz;
                    if (distance <= 10) {
                        s.floor(dx, dz, distance <= 4 ? Material.PACKED_ICE : Material.SNOW_BLOCK);
                    }
                }
            }
            s.sound("block.powder_snow.place", 1.0F, 0.6F);
            s.sound("entity.player.hurt_freeze", 1.0F, 0.8F);
        });
        s.every(0, 1, 24, step -> {
            for (int arm = 0; arm < 3; arm++) {
                double angle = step * 0.35D + arm * EffectMath.TAU / 3.0D;
                double radius = 3.0D - step * 0.1D;
                s.particle(Particles.SNOWFLAKE, Math.cos(angle) * radius, 0.3D + step * 0.08D, Math.sin(angle) * radius,
                        2, 0.05D, 0.0D);
            }
            s.dust(s.random(-1, 1), s.random(0, 2), s.random(-1, 1), FROST, 1.0F);
        });
        for (int index = 0; index < SPIKES; index++) {
            int spike = index;
            double angle = EffectMath.TAU * spike / SPIKES;
            double height = 1.2D + (spike % 3) * 0.5D;
            s.at(2 + spike * 2, () -> {
                spikes[spike] = s.block(ICE[spike % ICE.length], spike(angle, 0.01D));
                s.sound("block.glass.place", 0.8F, 0.8F + spike * 0.1F);
            });
            s.at(3 + spike * 2, () -> s.move(spikes[spike], spike(angle, height), 6));
        }
        s.at(22, () -> {
            coffin[0] = s.block(Material.ICE, EffectMath.box(0, 1.0D, 0, 0.01D, 0.01D, 0.01D, EffectMath.spin(0, 0, 0)));
            s.sound("block.amethyst_block.resonate", 1.0F, 1.6F);
        });
        s.at(23, () -> s.move(coffin[0], EffectMath.box(0, 1.0D, 0, 0.9D, 2.1D, 0.9D, EffectMath.spin(0.2D, 0, 0)), 5));
        s.at(33, () -> {
            for (int index = 0; index < SPIKES; index++) {
                double angle = EffectMath.TAU * index / SPIKES;
                s.move(spikes[index], EffectMath.box(Math.cos(angle) * 5.0D, s.random(1.5, 3.0), Math.sin(angle) * 5.0D,
                        0.1D, 0.3D, 0.1D, EffectMath.spin(s.random(-4, 4), s.random(-4, 4), 0)), 10);
            }
            s.move(coffin[0], EffectMath.box(0, 1.0D, 0, 0.01D, 0.01D, 0.01D, EffectMath.spin(2, 2, 0)), 4);
            int total = s.count(60);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(index % 2 == 0 ? Particles.SNOWFLAKE : Particles.END_ROD, 0, 1, 0,
                        direction.x, direction.y, direction.z, 0.4D);
            }
            s.particle(Particles.FLASH, 0, 1, 0, 1, 0, 0, 0, 0, FROST, 1.0F);
            s.sound("block.glass.break", 1.0F, 0.6F);
            s.sound("block.glass.break", 1.0F, 0.9F);
            s.sound("block.glass.break", 1.0F, 1.3F);
            s.sound("entity.player.hurt_freeze", 1.0F, 0.5F);
            s.shake();
            s.flair(Component.text("❄ FROZEN SOLID ❄", TextColor.color(0x9FE7FF)).decorate(TextDecoration.BOLD),
                    Component.text(s.victimName() + " shattered into frost", TextColor.color(0x8E8E8E)));
        });
        s.every(35, 1, 25, step -> {
            int flakes = s.count(5);
            for (int flake = 0; flake < flakes; flake++) {
                s.particle(Particles.SNOWFLAKE, s.random(-3.5, 3.5), s.random(3, 5), s.random(-3.5, 3.5), 1, 0.0D, 0.0D);
            }
            s.particle(Particles.WHITE_ASH, 0, 2, 0, 4, 2.0D, 0.0D);
        });
        s.every(40, 7, 3, step -> s.sound("block.amethyst_block.chime", 0.6F, 1.4F + step * 0.2F));
    }

    private static Transformation spike(double angle, double height) {
        double lean = 0.45D;
        double x = Math.cos(angle) * (1.6D + height * 0.25D);
        double z = Math.sin(angle) * (1.6D + height * 0.25D);
        return EffectMath.box(x, height / 2.0D, z, 0.32D, Math.max(0.01D, height), 0.32D,
                EffectMath.spin(-angle, 0, 0).rotateZ((float) (lean * Math.signum(Math.cos(angle)))));
    }
}
