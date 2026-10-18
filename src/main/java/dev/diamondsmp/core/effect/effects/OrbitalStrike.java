package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.joml.Vector3f;

import java.util.List;

public final class OrbitalStrike implements KillEffect {

    private static final Color TARGET = Color.fromRGB(0xFF2D2D);
    private static final Color BEAM = Color.fromRGB(0xE8F7FF);
    private static final double SKY = 28.0D;
    private static final int LOCK = 24;

    @Override
    public String id() {
        return "orbital_strike";
    }

    @Override
    public String name() {
        return "&f&lOrbital Strike";
    }

    @Override
    public Material icon() {
        return Material.END_CRYSTAL;
    }

    @Override
    public List<String> description() {
        return List.of("&7A satellite locks on and fires a", "&7laser that vaporises the body.");
    }

    @Override
    public void play(EffectSession s) {
        ArmorStand[] body = new ArmorStand[1];
        s.at(0, () -> {
            body[0] = s.corpse(CorpsePose.SLUMPED, 0, 0, (float) s.random(0, 360));
            if (body[0] != null) {
                body[0].setGlowing(true);
            }
        });
        s.every(0, 1, LOCK, step -> {
            double progress = step / (double) LOCK;
            double radius = EffectMath.lerp(3.0D, 0.8D, EffectMath.easeOut(progress));
            s.ring(Particles.DUST, 0.15D, radius, 26, step * 0.15D, TARGET, 1.2F);
            s.ring(Particles.DUST, 0.15D, radius * 0.5D, 14, -step * 0.2D, TARGET, 1.0F);
            for (int arm = 0; arm < 4; arm++) {
                double angle = arm * Math.PI / 2.0D + step * 0.15D;
                s.line(Particles.DUST, Math.cos(angle) * radius * 0.6D, 0.15D, Math.sin(angle) * radius * 0.6D,
                        Math.cos(angle) * radius * 1.3D, 0.15D, Math.sin(angle) * radius * 1.3D, 0.25D, TARGET, 1.0F);
            }
        });
        int[] beeps = {0, 6, 11, 15, 18, 20, 22, 23};
        for (int index = 0; index < beeps.length; index++) {
            float pitch = 1.0F + index * 0.12F;
            s.at(beeps[index], () -> s.sound("block.note_block.pling", 0.8F, pitch));
        }
        BlockDisplay[] beam = new BlockDisplay[2];
        s.at(LOCK, () -> {
            beam[0] = s.block(Material.WHITE_CONCRETE, EffectMath.box(0, SKY / 2.0D, 0, 0.05D, SKY, 0.05D, EffectMath.spin(0, 0, 0)));
            beam[1] = s.block(Material.LIGHT_BLUE_STAINED_GLASS, EffectMath.box(0, SKY / 2.0D, 0, 0.1D, SKY, 0.1D,
                    EffectMath.spin(0, 0, 0)));
            s.sound("block.beacon.activate", 1.0F, 2.0F);
        });
        s.at(LOCK + 1, () -> {
            s.move(beam[0], EffectMath.box(0, SKY / 2.0D, 0, 0.9D, SKY, 0.9D, EffectMath.spin(0, 0.6D, 0)), 4);
            s.move(beam[1], EffectMath.box(0, SKY / 2.0D, 0, 1.6D, SKY, 1.6D, EffectMath.spin(0, -0.4D, 0)), 4);
        });
        s.at(LOCK + 4, () -> {
            s.particle(Particles.FLASH, 0, 1, 0, 1, 0, 0, 0, 0, BEAM, 1.0F);
            s.particle(Particles.EXPLOSION_EMITTER, 0, 0.5D, 0);
            s.sound("entity.warden.sonic_boom", 1.0F, 1.2F);
            s.sound("entity.generic.explode", 1.0F, 0.7F);
            s.sound("block.beacon.power_select", 1.0F, 0.5F);
            s.shake();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    s.floor(dx, dz, dx == 0 && dz == 0 ? Material.SHROOMLIGHT : Material.MAGMA_BLOCK);
                }
            }
        });
        s.every(LOCK + 4, 1, 16, step -> {
            s.line(Particles.END_ROD, 0, SKY, 0, 0, 0.2D, 0, 1.6D, null, 1.0F);
            s.particle(Particles.FLAME, 0, 0.3D, 0, 6, 0.6D, 0.08D);
            int sparks = s.count(10);
            for (int spark = 0; spark < sparks; spark++) {
                Vector3f direction = EffectMath.sphere(spark + step * sparks, sparks * 16);
                s.motion(Particles.ELECTRIC_SPARK, 0, 0.3D, 0, direction.x, Math.abs(direction.y), direction.z, 0.5D);
            }
        });
        s.at(LOCK + 12, () -> {
            s.remove(body[0]);
            s.particle(Particles.ASH, 0, 0.8D, 0, 40, 0.4D, 0.0D);
            s.particle(Particles.LARGE_SMOKE, 0, 0.8D, 0, 20, 0.3D, 0.05D);
            s.sound("block.fire.extinguish", 1.0F, 0.6F);
        });
        s.at(LOCK + 20, () -> {
            s.move(beam[0], EffectMath.box(0, SKY / 2.0D, 0, 0.01D, SKY, 0.01D, EffectMath.spin(0, 0, 0)), 4);
            s.move(beam[1], EffectMath.box(0, SKY / 2.0D, 0, 0.01D, SKY, 0.01D, EffectMath.spin(0, 0, 0)), 4);
            s.sound("block.beacon.deactivate", 1.0F, 1.4F);
        });
        s.every(LOCK + 21, 1, 14, step -> {
            double radius = 0.8D + EffectMath.easeOut(step / 14.0D) * 5.0D;
            s.ring(Particles.LARGE_SMOKE, 0.2D, radius, 18, 0, null, 1.0F);
            s.particle(Particles.SIGNAL_SMOKE, 0, 0.3D, 0, 1, 0.2D, 0.01D);
        });
    }
}
