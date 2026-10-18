package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class Shatter implements KillEffect {

    private static final double SHARD = 0.28D;
    private static final Material[] GLASS = {
            Material.GLASS, Material.LIGHT_BLUE_STAINED_GLASS, Material.WHITE_STAINED_GLASS, Material.CYAN_STAINED_GLASS
    };
    private static final List<Vector3f> BODY = body();

    @Override
    public String id() {
        return "shatter";
    }

    @Override
    public String name() {
        return "&f&lShatter";
    }

    @Override
    public Material icon() {
        return Material.GLASS;
    }

    @Override
    public List<String> description() {
        return List.of("&7Your victim turns to glass, cracks,", "&7and explodes into a thousand shards.");
    }

    @Override
    public void play(EffectSession s) {
        BlockDisplay[] shards = new BlockDisplay[BODY.size()];
        s.at(0, () -> {
            for (int index = 0; index < BODY.size(); index++) {
                Vector3f point = BODY.get(index);
                shards[index] = s.block(GLASS[index % GLASS.length], EffectMath.cube(point.x, point.y, point.z, SHARD,
                        EffectMath.spin(0, 0, 0)));
            }
            s.sound("block.glass.place", 1.0F, 0.8F);
            s.sound("block.amethyst_block.resonate", 1.0F, 1.2F);
        });
        s.every(2, 2, 5, step -> {
            for (int index = 0; index < shards.length; index++) {
                Vector3f point = BODY.get(index);
                double jitter = 0.02D * (step + 1);
                s.move(shards[index], EffectMath.cube(point.x + s.random(-jitter, jitter), point.y, point.z + s.random(-jitter, jitter),
                        SHARD, EffectMath.spin(s.random(-0.1, 0.1) * step, 0, 0)), 2);
            }
            s.particle(Particles.CRIT, 0, 1, 0, 6, 0.3D, 0.1D);
            s.sound("block.glass.hit", 0.8F, 1.0F + step * 0.15F);
        });
        s.at(13, () -> {
            for (int index = 0; index < shards.length; index++) {
                Vector3f point = BODY.get(index);
                Vector3f out = new Vector3f(point.x, point.y - 0.9F, point.z);
                if (out.lengthSquared() < 0.01F) {
                    out.set((float) s.random(-1, 1), 0.3F, (float) s.random(-1, 1));
                }
                out.normalize().mul((float) s.random(2.5, 4.5));
                s.move(shards[index], EffectMath.cube(point.x + out.x, point.y + out.y + s.random(0.5, 2.0), point.z + out.z,
                        SHARD * 0.8D, EffectMath.spin(s.random(-6, 6), s.random(-6, 6), s.random(-6, 6))), 12);
            }
            s.particle(Particles.FLASH, 0, 1, 0, 1, 0, 0, 0, 0, null, 1.0F);
            int total = s.count(50);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(Particles.END_ROD, 0, 1, 0, direction.x, direction.y * 0.6D, direction.z, 0.4D);
            }
            s.sound("block.glass.break", 1.0F, 0.7F);
            s.sound("block.glass.break", 1.0F, 1.0F);
            s.sound("block.glass.break", 1.0F, 1.4F);
            s.sound("entity.generic.explode", 0.4F, 1.8F);
            s.shake();
        });
        s.at(26, () -> {
            for (BlockDisplay shard : shards) {
                if (shard != null && shard.isValid()) {
                    Transformation current = shard.getTransformation();
                    Vector3f fall = new Vector3f(current.getTranslation()).add(0.0F, -2.5F, 0.0F);
                    s.move(shard, new Transformation(fall, current.getLeftRotation(), new Vector3f(0.02F),
                            current.getRightRotation()), 12);
                }
            }
            s.sound("block.amethyst_cluster.break", 1.0F, 1.2F);
        });
        s.every(14, 2, 14, step -> s.particle(Particles.END_ROD, s.random(-3, 3), s.random(0.2, 3), s.random(-3, 3), 2,
                0.1D, 0.0D));
        s.every(18, 6, 4, step -> s.sound("block.amethyst_block.chime", 0.6F, 1.0F + step * 0.25F));
    }

    private static List<Vector3f> body() {
        List<Vector3f> points = new ArrayList<>();
        for (double y = 0.15D; y < 0.9D; y += SHARD) {
            points.add(new Vector3f(-0.15F, (float) y, 0.0F));
            points.add(new Vector3f(0.15F, (float) y, 0.0F));
        }
        for (double y = 0.95D; y < 1.5D; y += SHARD) {
            for (double x = -0.3D; x <= 0.31D; x += SHARD) {
                points.add(new Vector3f((float) x, (float) y, 0.0F));
            }
            points.add(new Vector3f(-0.58F, (float) y, 0.0F));
            points.add(new Vector3f(0.58F, (float) y, 0.0F));
        }
        for (double y = 1.6D; y < 2.0D; y += SHARD) {
            points.add(new Vector3f(-0.14F, (float) y, 0.0F));
            points.add(new Vector3f(0.14F, (float) y, 0.0F));
        }
        return List.copyOf(points);
    }
}
