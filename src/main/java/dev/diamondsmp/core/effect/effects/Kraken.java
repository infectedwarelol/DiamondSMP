package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.joml.Vector3f;

import java.util.List;

public final class Kraken implements KillEffect {

    private static final int TENTACLES = 4;
    private static final int SEGMENTS = 7;
    private static final double BASE_RADIUS = 1.9D;
    private static final Material[] FLESH = {Material.PRISMARINE, Material.DARK_PRISMARINE};

    @Override
    public String id() {
        return "kraken";
    }

    @Override
    public String name() {
        return "&3&lKraken";
    }

    @Override
    public Material icon() {
        return Material.HEART_OF_THE_SEA;
    }

    @Override
    public List<String> description() {
        return List.of("&7The ground turns to sea, tentacles", "&7rise, grab the body and drag it under.");
    }

    @Override
    public void play(EffectSession s) {
        ArmorStand[] body = new ArmorStand[1];
        BlockDisplay[][] tentacles = new BlockDisplay[TENTACLES][SEGMENTS];
        s.at(0, () -> {
            body[0] = s.corpse(CorpsePose.STANDING, 0, 0, (float) s.random(0, 360));
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx * dx + dz * dz <= 10) {
                        s.floor(dx, dz, Material.WATER);
                    }
                }
            }
            s.particle(Particles.SPLASH, 0, 0.2D, 0, 60, 2.0D, 0.2D);
            s.sound("entity.player.splash.high_speed", 1.0F, 0.6F);
            s.sound("ambient.underwater.enter", 1.0F, 0.8F);
        });
        s.every(0, 1, 60, step -> {
            s.particle(Particles.BUBBLE, s.random(-2.5, 2.5), 0.1D, s.random(-2.5, 2.5), 2, 0.1D, 0.05D);
            if (step % 5 == 0) {
                s.particle(Particles.SPLASH, s.random(-2, 2), 0.2D, s.random(-2, 2), 10, 0.3D, 0.1D);
            }
        });
        s.at(4, () -> {
            for (int tentacle = 0; tentacle < TENTACLES; tentacle++) {
                for (int segment = 0; segment < SEGMENTS; segment++) {
                    Vector3f[] points = shape(tentacle, segment, 0.05D, 0.0D, 0);
                    tentacles[tentacle][segment] = s.block(FLESH[segment % FLESH.length],
                            EffectMath.segment(points[0], points[1], width(segment)));
                }
            }
            s.sound("entity.elder_guardian.ambient", 1.0F, 0.5F);
        });
        s.every(5, 2, 26, step -> {
            int phase = step + 1;
            double reach = Math.min(1.0D, phase / 8.0D);
            double curl = phase <= 8 ? 0.0D : Math.min(1.0D, (phase - 8) / 8.0D);
            double sink = phase <= 18 ? 0.0D : (phase - 18) / 8.0D;
            for (int tentacle = 0; tentacle < TENTACLES; tentacle++) {
                for (int segment = 0; segment < SEGMENTS; segment++) {
                    Vector3f[] points = shape(tentacle, segment, reach * (1.0D - sink), curl, phase);
                    s.move(tentacles[tentacle][segment], EffectMath.segment(points[0], points[1], width(segment)), 2);
                }
            }
            if (phase == 9) {
                s.sound("entity.guardian.attack", 1.0F, 0.5F);
            }
        });
        s.every(6, 6, 4, step -> s.sound("entity.generic.splash", 0.8F, 0.6F + step * 0.1F));
        s.at(42, () -> {
            s.sound("entity.elder_guardian.curse", 0.8F, 0.6F);
            s.sound("entity.player.splash", 1.0F, 0.5F);
            s.shake();
        });
        s.every(42, 2, 8, step -> {
            s.place(body[0], 0, -(step + 1) * 0.28D, 0, (step + 1) * 20.0F);
            s.particle(Particles.SPLASH, 0, 0.3D, 0, 25, 0.6D, 0.2D);
            s.particle(Particles.BUBBLE, 0, 0.2D, 0, 10, 0.4D, 0.1D);
            s.particle(Particles.FALLING_WATER, s.random(-1.8, 1.8), 2.6D, s.random(-1.8, 1.8), 4, 0.3D, 0.0D);
        });
        s.at(58, () -> {
            s.remove(body[0]);
            s.particle(Particles.SPLASH, 0, 0.3D, 0, 80, 1.2D, 0.3D);
            s.sound("entity.player.splash.high_speed", 1.0F, 0.5F);
        });
    }

    private static float width(int segment) {
        return (float) (0.5D - segment * 0.055D);
    }

    private static Vector3f[] shape(int tentacle, int segment, double reach, double curl, int phase) {
        return new Vector3f[]{point(tentacle, segment, reach, curl, phase), point(tentacle, segment + 1, reach, curl, phase)};
    }

    private static Vector3f point(int tentacle, int joint, double reach, double curl, int phase) {
        double base = EffectMath.TAU * tentacle / TENTACLES + Math.PI / 4.0D;
        double t = joint / (double) SEGMENTS;
        double height = t * 3.4D * reach;
        double inward = curl * t * t * BASE_RADIUS * 1.1D;
        double sway = Math.sin(phase * 0.4D + tentacle + t * 3.0D) * 0.35D * t;
        double radius = BASE_RADIUS - inward;
        double angle = base + sway;
        return new Vector3f((float) (Math.cos(angle) * radius), (float) (height - curl * t * t * 1.4D),
                (float) (Math.sin(angle) * radius));
    }
}
