package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import dev.diamondsmp.core.util.Lookup;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class ChainBind implements KillEffect {

    private static final Material CHAIN = Lookup.material("IRON_CHAIN", Lookup.material("CHAIN", Material.IRON_BARS));
    private static final Color RUNE = Color.fromRGB(0x7A1FA2);
    private static final int ANCHORS = 4;
    private static final int RING_SEGMENTS = 8;
    private static final double[] RING_HEIGHTS = {0.35D, 1.0D, 1.65D};
    private static final double ANCHOR_RADIUS = 5.0D;
    private static final double ANCHOR_HEIGHT = 7.0D;
    private static final float CHEST = 1.45F;
    private static final double LIFT = 0.3D;

    @Override
    public String id() {
        return "chains";
    }

    @Override
    public String name() {
        return "&8&lChains of Judgement";
    }

    @Override
    public Material icon() {
        return CHAIN;
    }

    @Override
    public List<String> description() {
        return List.of("&7Chains burst from the sky, bind", "&7your victim tight and snap apart.");
    }

    @Override
    public void play(EffectSession s) {
        BlockDisplay[] anchors = new BlockDisplay[ANCHORS];
        BlockDisplay[][] rings = new BlockDisplay[RING_HEIGHTS.length][RING_SEGMENTS];
        ArmorStand[] body = new ArmorStand[1];
        s.at(0, () -> {
            body[0] = s.corpse(CorpsePose.STANDING, 0, 0, 0, 0.0F);
            s.sound("entity.evoker.prepare_summon", 1.0F, 0.6F);
            s.sound("block.chain.place", 1.0F, 0.5F);
        });
        s.every(0, 1, 8, step -> {
            for (int index = 0; index < ANCHORS; index++) {
                Vector3f anchor = anchor(index);
                double angle = step * 0.8D;
                s.particle(Particles.ENCHANT, anchor.x + Math.cos(angle) * 0.6D, anchor.y, anchor.z + Math.sin(angle) * 0.6D,
                        3, 0.1D, 0.6D);
                s.dust(anchor.x, anchor.y, anchor.z, RUNE, 2.0F);
            }
        });
        for (int index = 0; index < ANCHORS; index++) {
            int chain = index;
            int launch = 6 + chain * 2;
            s.at(launch, () -> {
                Vector3f anchor = anchor(chain);
                anchors[chain] = s.block(CHAIN, EffectMath.segment(anchor, new Vector3f(anchor).add(0.0F, -0.02F, 0.0F), 1.0F));
                s.sound("block.chain.place", 1.0F, 0.7F + chain * 0.15F);
            });
            s.at(launch + 1, () -> s.move(anchors[chain], EffectMath.segment(anchor(chain),
                    new Vector3f(0.0F, CHEST, 0.0F), 1.0F), 6));
            s.every(launch + 1, 1, 6, step -> {
                Vector3f anchor = anchor(chain);
                double t = (step + 1) / 6.0D;
                s.particle(Particles.ENCHANTED_HIT, EffectMath.lerp(anchor.x, 0, t), EffectMath.lerp(anchor.y, CHEST, t),
                        EffectMath.lerp(anchor.z, 0, t), 3, 0.1D, 0.05D);
            });
            s.at(launch + 7, () -> {
                s.sound("block.chain.hit", 1.0F, 0.5F + chain * 0.1F);
                s.particle(Particles.CRIT, 0, CHEST, 0, 10, 0.3D, 0.2D);
            });
        }
        s.at(19, () -> {
            s.pose(body[0], CorpsePose.HANGING);
            s.place(body[0], 0, LIFT, 0, 0.0F);
        });
        s.at(22, () -> {
            for (int ring = 0; ring < RING_HEIGHTS.length; ring++) {
                for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                    rings[ring][segment] = s.block(CHAIN, ringSegment(ring, segment, 2.2D, 0.0D));
                }
            }
            s.sound("entity.iron_golem.attack", 1.0F, 0.6F);
        });
        s.every(23, 2, 10, step -> {
            double radius = EffectMath.lerp(2.2D, 0.55D, EffectMath.easeInOut((step + 1) / 10.0D));
            double phase = (step + 1) * 0.35D;
            for (int ring = 0; ring < RING_HEIGHTS.length; ring++) {
                double direction = ring % 2 == 0 ? 1.0D : -1.0D;
                for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                    s.move(rings[ring][segment], ringSegment(ring, segment, radius, phase * direction), 2);
                }
            }
            s.sound("block.chain.hit", 0.9F, 0.6F + step * 0.12F);
            s.ring(Particles.DUST, CHEST, radius + 0.3D, 16, phase, RUNE, 1.2F);
        });
        s.at(44, () -> {
            for (int ring = 0; ring < RING_HEIGHTS.length; ring++) {
                for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                    double angle = EffectMath.TAU * segment / RING_SEGMENTS;
                    double distance = s.random(3.0D, 5.0D);
                    Vector3f from = new Vector3f((float) (Math.cos(angle) * distance), (float) (RING_HEIGHTS[ring] + s.random(1, 3)),
                            (float) (Math.sin(angle) * distance));
                    Vector3f to = new Vector3f(from).add((float) s.random(-0.6, 0.6), (float) s.random(-0.6, 0.6),
                            (float) s.random(-0.6, 0.6));
                    Transformation scattered = EffectMath.segment(from, to, 0.6F);
                    s.move(rings[ring][segment], new Transformation(scattered.getTranslation(),
                            new Quaternionf(scattered.getLeftRotation()).rotateX((float) s.random(1, 4)),
                            scattered.getScale(), scattered.getRightRotation()), 10);
                }
            }
            for (int index = 0; index < ANCHORS; index++) {
                Vector3f anchor = anchor(index);
                s.move(anchors[index], EffectMath.segment(anchor, new Vector3f(anchor).add(0.0F, -0.02F, 0.0F), 1.0F), 6);
            }
            s.particle(Particles.EXPLOSION, 0, CHEST, 0, 2, 0.4D, 0.0D);
            s.particle(Particles.CRIT, 0, CHEST, 0, 40, 0.8D, 0.6D);
            s.particle(Particles.LARGE_SMOKE, 0, CHEST, 0, 20, 0.6D, 0.05D);
            s.pose(body[0], CorpsePose.SLUMPED);
            s.place(body[0], 0, CorpsePose.SLUMPED.floorOffset(), 0, 0.0F);
            s.sound("block.chain.break", 1.0F, 0.6F);
            s.sound("block.chain.break", 1.0F, 0.9F);
            s.sound("block.chain.break", 1.0F, 1.3F);
            s.sound("block.anvil.land", 1.0F, 0.5F);
            s.sound("entity.generic.explode", 0.6F, 1.2F);
            s.shake();
        });
        s.at(55, () -> {
            for (BlockDisplay[] ring : rings) {
                for (BlockDisplay segment : ring) {
                    if (segment != null) {
                        Transformation current = segment.getTransformation();
                        s.move(segment, new Transformation(new Vector3f(current.getTranslation()).add(0.0F, -2.0F, 0.0F),
                                current.getLeftRotation(), new Vector3f(0.01F), current.getRightRotation()), 8);
                    }
                }
            }
        });
        s.every(45, 2, 10, step -> {
            s.particle(Particles.SMOKE, s.random(-1.5, 1.5), 0.2D + step * 0.2D, s.random(-1.5, 1.5), 4, 0.2D, 0.02D);
            for (int index = 0; index < ANCHORS; index++) {
                Vector3f anchor = anchor(index);
                s.particle(Particles.REVERSE_PORTAL, anchor.x, anchor.y, anchor.z, 3, 0.2D, 0.0D);
            }
        });
    }

    private static Vector3f anchor(int index) {
        double angle = EffectMath.TAU * index / ANCHORS + Math.PI / 4.0D;
        return new Vector3f((float) (Math.cos(angle) * ANCHOR_RADIUS), (float) ANCHOR_HEIGHT,
                (float) (Math.sin(angle) * ANCHOR_RADIUS));
    }

    private static Transformation ringSegment(int ring, int segment, double radius, double phase) {
        double first = phase + EffectMath.TAU * segment / RING_SEGMENTS;
        double second = phase + EffectMath.TAU * (segment + 1) / RING_SEGMENTS;
        float y = (float) RING_HEIGHTS[ring];
        Vector3f from = new Vector3f((float) (Math.cos(first) * radius), y, (float) (Math.sin(first) * radius));
        Vector3f to = new Vector3f((float) (Math.cos(second) * radius), y, (float) (Math.sin(second) * radius));
        return EffectMath.segment(from, to, 0.8F);
    }
}
