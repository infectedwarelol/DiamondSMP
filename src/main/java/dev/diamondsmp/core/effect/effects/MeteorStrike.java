package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.joml.Vector3f;

import java.util.List;

public final class MeteorStrike implements KillEffect {

    private static final Vector3f SKY = new Vector3f(9.0F, 22.0F, 6.0F);
    private static final Vector3f IMPACT = new Vector3f(0.0F, 0.8F, 0.0F);
    private static final int FALL = 24;
    private static final Color WARNING = Color.fromRGB(0xFF3B1F);
    private static final Material[] DEBRIS = {Material.MAGMA_BLOCK, Material.NETHERRACK, Material.BLACKSTONE};

    @Override
    public String id() {
        return "meteor_strike";
    }

    @Override
    public String name() {
        return "&6&lMeteor Strike";
    }

    @Override
    public Material icon() {
        return Material.MAGMA_BLOCK;
    }

    @Override
    public List<String> description() {
        return List.of("&7A burning meteor tears through the", "&7sky and leaves a smoking crater.");
    }

    @Override
    public void play(EffectSession s) {
        BlockDisplay[] meteor = new BlockDisplay[1];
        s.every(0, 2, 6, step -> {
            s.ring(Particles.DUST, 0.1D, 2.0D - step * 0.1D, 22, step * 0.3D, WARNING, 1.4F);
            s.sound("block.note_block.basedrum", 1.0F, 0.6F + step * 0.1F);
        });
        s.at(2, () -> {
            meteor[0] = s.block(Material.MAGMA_BLOCK, EffectMath.cube(SKY.x, SKY.y, SKY.z, 1.8D, EffectMath.spin(0, 0, 0)));
            s.sound("entity.ghast.shoot", 1.0F, 0.5F);
        });
        s.at(3, () -> s.move(meteor[0], EffectMath.cube(IMPACT.x, IMPACT.y, IMPACT.z, 1.8D, EffectMath.spin(4, 3, 2)), FALL));
        s.every(3, 1, FALL, step -> {
            double t = step / (double) FALL;
            double x = EffectMath.lerp(SKY.x, IMPACT.x, t);
            double y = EffectMath.lerp(SKY.y, IMPACT.y, t);
            double z = EffectMath.lerp(SKY.z, IMPACT.z, t);
            s.particle(Particles.FLAME, x, y, z, 6, 0.4D, 0.02D);
            s.particle(Particles.LARGE_SMOKE, x + 0.4D, y + 0.6D, z + 0.3D, 3, 0.3D, 0.01D);
            s.particle(Particles.LAVA, x, y, z, 1, 0.3D, 0.0D);
        });
        s.every(6, 6, 3, step -> s.sound("block.fire.ambient", 1.0F, 0.6F + step * 0.2F));
        s.at(FALL + 1, () -> s.sound("entity.firework_rocket.large_blast_far", 1.0F, 0.5F));
        BlockDisplay[] debris = new BlockDisplay[10];
        s.at(FALL + 3, () -> {
            s.remove(meteor[0]);
            s.corpse(CorpsePose.SLUMPED, 0.2D, 0.1D, (float) s.random(0, 360));
            s.particle(Particles.EXPLOSION_EMITTER, 0, 0.5D, 0);
            s.particle(Particles.FLAME, 0, 0.5D, 0, 60, 1.2D, 0.2D);
            s.particle(Particles.LAVA, 0, 0.5D, 0, 20, 1.0D, 0.0D);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int distance = dx * dx + dz * dz;
                    if (distance <= 10) {
                        s.floor(dx, dz, distance <= 2 ? Material.MAGMA_BLOCK : distance <= 5 ? Material.BLACKSTONE
                                : Material.COBBLED_DEEPSLATE);
                    }
                }
            }
            for (int index = 0; index < debris.length; index++) {
                debris[index] = s.block(DEBRIS[index % DEBRIS.length], EffectMath.cube(0, 0.8D, 0, 0.45D,
                        EffectMath.spin(index, index, 0)));
            }
            s.sound("entity.generic.explode", 1.0F, 0.6F);
            s.sound("block.anvil.land", 1.0F, 0.5F);
            s.sound("entity.lightning_bolt.impact", 1.0F, 0.6F);
            s.shake();
        });
        s.at(FALL + 4, () -> {
            for (int index = 0; index < debris.length; index++) {
                double angle = EffectMath.TAU * index / debris.length;
                double distance = s.random(2.5, 4.5);
                s.move(debris[index], EffectMath.cube(Math.cos(angle) * distance, s.random(1.5, 3.5), Math.sin(angle) * distance,
                        0.35D, EffectMath.spin(s.random(-5, 5), s.random(-5, 5), 0)), 8);
            }
        });
        s.at(FALL + 12, () -> {
            for (int index = 0; index < debris.length; index++) {
                double angle = EffectMath.TAU * index / debris.length;
                s.move(debris[index], EffectMath.cube(Math.cos(angle) * 5.0D, 0.15D, Math.sin(angle) * 5.0D, 0.2D,
                        EffectMath.spin(s.random(-5, 5), s.random(-5, 5), 0)), 8);
            }
        });
        s.every(FALL + 4, 1, 14, step -> {
            double radius = 1.0D + EffectMath.easeOut(step / 14.0D) * 6.0D;
            s.ring(Particles.LARGE_SMOKE, 0.2D, radius, 22, 0, null, 1.0F);
        });
        s.every(FALL + 6, 2, 16, step -> {
            s.particle(Particles.SIGNAL_SMOKE, s.random(-0.8, 0.8), 0.4D, s.random(-0.8, 0.8), 1, 0.1D, 0.02D);
            s.particle(Particles.LAVA, s.random(-1.5, 1.5), 0.3D, s.random(-1.5, 1.5), 1, 0.0D, 0.0D);
        });
    }
}
