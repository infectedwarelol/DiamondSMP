package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.joml.Vector3f;

import java.util.List;

public final class Supernova implements KillEffect {

    private static final double STAR_Y = 2.0D;
    private static final Material[] STAGES = {
            Material.SEA_LANTERN, Material.GLOWSTONE, Material.SHROOMLIGHT, Material.REDSTONE_BLOCK, Material.WHITE_CONCRETE
    };
    private static final Color HOT = Color.fromRGB(0xFFE9A6);
    private static final Color FLARE = Color.fromRGB(0xFF6A00);

    @Override
    public String id() {
        return "supernova";
    }

    @Override
    public String name() {
        return "&6&lSupernova";
    }

    @Override
    public Material icon() {
        return Material.NETHER_STAR;
    }

    @Override
    public List<String> description() {
        return List.of("&7A star is born, burns out, collapses", "&7and explodes in a blinding supernova.");
    }

    @Override
    public void play(EffectSession s) {
        BlockDisplay[] star = new BlockDisplay[1];
        s.at(0, () -> {
            star[0] = s.block(STAGES[0], EffectMath.cube(0, STAR_Y, 0, 0.05D, EffectMath.spin(0, 0, 0)));
            s.sound("block.beacon.ambient", 1.0F, 0.6F);
        });
        s.every(1, 3, 10, step -> s.move(star[0], EffectMath.cube(0, STAR_Y, 0, 0.15D + step * 0.13D,
                EffectMath.spin(step * 0.7D, step * 0.4D, step * 0.2D)), 3));
        s.every(6, 6, 4, step -> {
            if (star[0] != null && star[0].isValid()) {
                star[0].setBlock(STAGES[step + 1].createBlockData());
            }
        });
        s.every(0, 5, 6, step -> s.sound("block.note_block.bell", 0.8F, 0.5F + step * 0.28F));
        s.every(0, 1, 32, step -> {
            double radius = 1.6D + Math.sin(step * 0.2D) * 0.2D;
            for (int orbit = 0; orbit < 3; orbit++) {
                double tilt = orbit * Math.PI / 3.0D;
                double angle = step * (0.3D + orbit * 0.08D);
                double x = Math.cos(angle) * radius;
                double flat = Math.sin(angle) * radius;
                s.particle(orbit == 1 ? Particles.FLAME : Particles.END_ROD, x * Math.cos(tilt), STAR_Y + flat * Math.sin(tilt),
                        flat * Math.cos(tilt) + x * Math.sin(tilt) * 0.3D);
            }
            s.particle(Particles.DUST, 0, STAR_Y, 0, 2, 0.5D, 0.5D, 0.5D, 0.0D, HOT, 1.4F);
        });
        s.at(32, () -> {
            s.move(star[0], EffectMath.cube(0, STAR_Y, 0, 0.15D, EffectMath.spin(6, 4, 2)), 5);
            s.sound("block.respawn_anchor.deplete", 1.0F, 0.5F);
        });
        s.every(32, 1, 6, step -> {
            int total = s.count(16);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(Particles.FLAME, direction.x * 3.0D, STAR_Y + direction.y * 3.0D, direction.z * 3.0D,
                        -direction.x, -direction.y, -direction.z, 0.4D);
            }
        });
        s.at(39, () -> {
            s.remove(star[0]);
            s.particle(Particles.FLASH, 0, STAR_Y, 0, 1, 0, 0, 0, 0, HOT, 1.0F);
            s.particle(Particles.EXPLOSION_EMITTER, 0, STAR_Y, 0);
            int total = s.count(90);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(index % 2 == 0 ? Particles.FLAME : Particles.END_ROD, 0, STAR_Y, 0,
                        direction.x, direction.y, direction.z, index % 2 == 0 ? 0.45D : 0.8D);
            }
            s.sound("entity.generic.explode", 1.0F, 0.5F);
            s.sound("entity.firework_rocket.twinkle_far", 1.0F, 0.8F);
            s.sound("entity.lightning_bolt.thunder", 0.6F, 0.6F);
            s.sound("block.beacon.deactivate", 1.0F, 1.6F);
            s.shake();
        });
        s.every(40, 1, 12, step -> {
            double radius = 0.8D + EffectMath.easeOut(step / 12.0D) * 7.0D;
            s.ring(Particles.DUST, STAR_Y, radius, 26, step * 0.1D, step % 2 == 0 ? HOT : FLARE, 1.8F);
            s.ring(Particles.FIREWORK, 0.2D, radius * 0.8D, 14, 0, null, 1.0F);
        });
        BlockDisplay[] dwarf = new BlockDisplay[1];
        s.at(42, () -> dwarf[0] = s.block(Material.QUARTZ_BLOCK, EffectMath.cube(0, STAR_Y, 0, 0.3D, EffectMath.spin(0, 0, 0))));
        s.every(43, 4, 5, step -> s.move(dwarf[0], EffectMath.cube(0, STAR_Y, 0, (step % 2 == 0 ? 0.35D : 0.22D) * (1.0D - step * 0.15D),
                EffectMath.spin(step, step * 0.5D, 0)), 4));
        s.every(43, 3, 7, step -> s.particle(Particles.WHITE_ASH, s.random(-3, 3), s.random(0, 4), s.random(-3, 3), 6, 0.5D, 0.0D));
    }
}
