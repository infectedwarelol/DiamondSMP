package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.List;

public final class InfernoTornado implements KillEffect {

    private static final int LAYERS = 16;
    private static final int DEBRIS = 5;
    private static final int SPIN_TICKS = 56;
    private static final Color EMBER = Color.fromRGB(0xFF5A1F);

    @Override
    public String id() {
        return "inferno_tornado";
    }

    @Override
    public String name() {
        return "&c&lInferno Tornado";
    }

    @Override
    public Material icon() {
        return Material.BLAZE_POWDER;
    }

    @Override
    public List<String> description() {
        return List.of("&7A pillar of fire spins up from the", "&7ground and swallows your victim.");
    }

    @Override
    public void play(EffectSession s) {
        s.at(0, () -> {
            for (int index = 0; index < 12; index++) {
                double angle = EffectMath.TAU * index / 12.0D;
                s.air((int) Math.round(Math.cos(angle) * 2.5D), 0, (int) Math.round(Math.sin(angle) * 2.5D), Material.FIRE);
            }
            s.sound("item.firecharge.use", 1.0F, 0.6F);
            s.sound("entity.blaze.ambient", 1.0F, 0.5F);
        });
        s.every(0, 1, SPIN_TICKS, step -> {
            double speed = 0.3D + Math.sin(step / (double) SPIN_TICKS * Math.PI) * 0.6D;
            double growth = EffectMath.easeOut(Math.min(1.0D, step / 18.0D));
            for (int layer = 0; layer < LAYERS * growth; layer++) {
                double height = layer * 0.4D;
                double radius = 0.25D + height * 0.33D;
                double angle = step * speed + layer * 0.55D;
                s.particle(Particles.FLAME, Math.cos(angle) * radius, height, Math.sin(angle) * radius);
                s.particle(Particles.FLAME, Math.cos(angle + Math.PI) * radius, height, Math.sin(angle + Math.PI) * radius);
                if (layer % 4 == 0) {
                    s.dust(Math.cos(angle + 1.5D) * radius, height, Math.sin(angle + 1.5D) * radius, EMBER, 1.3F);
                }
            }
            s.particle(Particles.LARGE_SMOKE, 0, LAYERS * 0.4D * growth, 0, 3, 1.2D, 0.02D);
            if (step % 3 == 0) {
                s.particle(Particles.LAVA, s.random(-1, 1), 0.3D, s.random(-1, 1), 1, 0.0D, 0.0D);
            }
        });
        BlockDisplay[] debris = new BlockDisplay[DEBRIS];
        ArmorStand[] body = new ArmorStand[1];
        s.at(4, () -> body[0] = s.corpse(CorpsePose.FLOATING, 0, 0, 0, 0.0F));
        s.every(5, 3, 17, step -> s.place(body[0], 0, (step + 1) * 0.16D, 0, (step + 1) * 40.0F));
        s.at(6, () -> {
            for (int index = 0; index < DEBRIS; index++) {
                debris[index] = s.block(index % 2 == 0 ? Material.MAGMA_BLOCK : Material.NETHERRACK,
                        orbit(index, 0, 0.5D));
            }
        });
        s.every(7, 2, 24, step -> {
            for (int index = 0; index < DEBRIS; index++) {
                s.move(debris[index], orbit(index, step + 1, 0.5D + (step + 1) * 0.12D), 2);
            }
        });
        s.every(0, 5, 11, step -> {
            s.sound("block.fire.ambient", 1.0F, 0.6F + step * 0.06F);
            s.sound("entity.blaze.shoot", 0.5F, 0.5F + step * 0.1F);
        });
        s.at(56, () -> {
            s.remove(body[0]);
            for (int index = 0; index < DEBRIS; index++) {
                double angle = EffectMath.TAU * index / DEBRIS;
                s.move(debris[index], EffectMath.cube(Math.cos(angle) * 6.0D, 1.0D, Math.sin(angle) * 6.0D, 0.05D,
                        EffectMath.spin(4, 4, 0)), 8);
            }
            int total = s.count(60);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(Particles.FLAME, 0, 2.5D, 0, direction.x, direction.y, direction.z, 0.4D);
            }
            s.particle(Particles.EXPLOSION, 0, 2, 0, 3, 1.0D, 0.0D);
            s.sound("entity.generic.explode", 1.0F, 0.8F);
            s.sound("entity.blaze.death", 1.0F, 0.6F);
            s.shake();
        });
        s.every(58, 1, 14, step -> s.particle(Particles.ASH, s.random(-3, 3), s.random(0.5, 5), s.random(-3, 3), 6, 0.3D, 0.0D));
    }

    private static Transformation orbit(int index, int step, double height) {
        double angle = EffectMath.TAU * index / DEBRIS + step * 0.55D;
        double radius = 0.6D + height * 0.33D;
        return EffectMath.cube(Math.cos(angle) * radius, height, Math.sin(angle) * radius, 0.4D,
                EffectMath.spin(step * 0.4D, step * 0.3D, 0));
    }
}
