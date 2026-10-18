package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.joml.Vector3f;

import java.util.List;

public final class SoulHarvest implements KillEffect {

    private static final Color SOUL = Color.fromRGB(0x6FF2FF);
    private static final double PEAK = 6.5D;

    @Override
    public String id() {
        return "soul_harvest";
    }

    @Override
    public String name() {
        return "&b&lSoul Harvest";
    }

    @Override
    public Material icon() {
        return Material.SOUL_LANTERN;
    }

    @Override
    public List<String> description() {
        return List.of("&7The soul is torn out, spirals", "&7into the sky and vanishes in a flash.");
    }

    @Override
    public void play(EffectSession s) {
        s.at(0, () -> {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz <= 5) {
                        s.floor(dx, dz, Material.SOUL_SOIL);
                    }
                }
            }
            s.corpse(CorpsePose.KNEELING, 0, 0, (float) s.random(0, 360));
            s.sound("block.soul_sand.break", 1.0F, 0.6F);
            s.sound("particle.soul_escape", 1.0F, 0.5F);
        });
        s.every(0, 1, 10, step -> {
            s.particle(Particles.SOUL_FLAME, s.random(-1.5, 1.5), 0.1D, s.random(-1.5, 1.5), 2, 0.05D, 0.03D);
            s.particle(Particles.SCULK_SOUL, 0, 0.6D + step * 0.05D, 0, 1, 0.2D, 0.0D);
        });
        ItemDisplay[] soul = new ItemDisplay[1];
        s.at(10, () -> {
            soul[0] = s.item(Material.GHAST_TEAR, EffectMath.item(0, 1.0D, 0, 1.2D, EffectMath.spin(0, 0, 0)));
            if (soul[0] != null) {
                soul[0].setGlowing(true);
            }
            s.sound("entity.vex.charge", 1.0F, 0.7F);
        });
        s.every(11, 3, 10, step -> s.move(soul[0], EffectMath.item(0, 1.0D + (step + 1) * (PEAK - 1.0D) / 10.0D, 0,
                1.2D, EffectMath.spin(step * 0.8D, 0, 0)), 3));
        s.every(10, 1, 30, step -> {
            double height = 1.0D + (PEAK - 1.0D) * step / 30.0D;
            for (int strand = 0; strand < 2; strand++) {
                double angle = step * 0.5D + strand * Math.PI;
                double radius = 0.9D - step * 0.02D;
                s.particle(Particles.SOUL, Math.cos(angle) * radius, height, Math.sin(angle) * radius, 1, 0.0D, 0.0D);
                s.dust(Math.cos(angle + 0.3D) * radius, height - 0.3D, Math.sin(angle + 0.3D) * radius, SOUL, 1.0F);
            }
            for (int wisp = 0; wisp < 3; wisp++) {
                double orbit = -step * 0.35D + wisp * EffectMath.TAU / 3.0D;
                s.particle(Particles.SOUL_FLAME, Math.cos(orbit) * 1.6D, height * 0.7D, Math.sin(orbit) * 1.6D);
            }
        });
        s.every(12, 5, 6, step -> s.sound("entity.allay.ambient_without_item", 0.8F, 0.6F + step * 0.22F));
        s.at(40, () -> {
            s.move(soul[0], EffectMath.item(0, PEAK, 0, 0.01D, EffectMath.spin(0, 0, 0)), 2);
            s.particle(Particles.FLASH, 0, PEAK, 0, 1, 0, 0, 0, 0, SOUL, 1.0F);
            int total = s.count(40);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(Particles.END_ROD, 0, PEAK, 0, direction.x, direction.y, direction.z, 0.3D);
            }
            s.sound("entity.vex.death", 1.0F, 0.5F);
            s.sound("block.respawn_anchor.deplete", 0.8F, 1.4F);
            s.sound("block.amethyst_block.chime", 1.0F, 1.8F);
            s.shake();
        });
        s.at(42, () -> s.remove(soul[0]));
        s.every(42, 1, 20, step -> {
            s.particle(Particles.SOUL_FLAME, s.random(-2, 2), PEAK - step * 0.3D, s.random(-2, 2), 1, 0.0D, 0.0D);
            s.ring(Particles.SOUL, 0.2D, 1.2D + step * 0.05D, 8, step * 0.2D, null, 1.0F);
        });
        s.at(50, () -> s.sound("ambient.soul_sand_valley.mood", 0.6F, 1.2F));
    }
}
