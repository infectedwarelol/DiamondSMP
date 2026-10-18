package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;

import java.util.List;

public final class VoidRift implements KillEffect {

    private static final Color VOID = Color.fromRGB(0x2B0A3D);
    private static final Color EDGE = Color.fromRGB(0xE040FB);
    private static final double CENTER_Y = 1.6D;
    private static final double HEIGHT = 3.2D;

    @Override
    public String id() {
        return "void_rift";
    }

    @Override
    public String name() {
        return "&5&lVoid Rift";
    }

    @Override
    public Material icon() {
        return Material.CRYING_OBSIDIAN;
    }

    @Override
    public List<String> description() {
        return List.of("&7Reality tears open, the void drinks", "&7in your victim and slams shut.");
    }

    @Override
    public void play(EffectSession s) {
        BlockDisplay[] rift = new BlockDisplay[2];
        s.every(0, 1, 10, step -> {
            double reach = (step + 1) / 10.0D * HEIGHT / 2.0D;
            s.line(Particles.ELECTRIC_SPARK, 0, CENTER_Y - reach, 0, 0, CENTER_Y + reach, 0, 0.2D, null, 1.0F);
            s.particle(Particles.CRIT, 0, CENTER_Y, 0, 4, 0.1D, 0.3D);
        });
        s.at(0, () -> {
            s.sound("block.end_portal_frame.fill", 1.0F, 0.5F);
            s.sound("block.respawn_anchor.charge", 1.0F, 0.6F);
        });
        s.at(10, () -> {
            rift[0] = s.block(Material.BLACK_CONCRETE, EffectMath.box(0, CENTER_Y, 0, 0.02D, HEIGHT, 0.06D, EffectMath.spin(0, 0, 0)));
            rift[1] = s.block(Material.PURPLE_STAINED_GLASS, EffectMath.box(0, CENTER_Y, 0, 0.02D, HEIGHT + 0.2D, 0.1D,
                    EffectMath.spin(0, 0, 0)));
            s.sound("block.portal.trigger", 1.0F, 0.6F);
        });
        s.at(11, () -> {
            s.move(rift[0], EffectMath.box(0, CENTER_Y, 0, 1.7D, HEIGHT, 0.06D, EffectMath.spin(0, 0, 0)), 8);
            s.move(rift[1], EffectMath.box(0, CENTER_Y, 0, 2.0D, HEIGHT + 0.3D, 0.1D, EffectMath.spin(0, 0, 0)), 8);
        });
        s.every(11, 1, 30, step -> {
            int pulls = s.count(6);
            for (int pull = 0; pull < pulls; pull++) {
                double angle = s.random(0, EffectMath.TAU);
                double radius = s.random(2.5, 5.0);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = s.random(0.2, 3.5);
                s.motion(Particles.REVERSE_PORTAL, x, y, z, -x, CENTER_Y - y, -z, 0.12D);
            }
            double edge = Math.min(1.0D, (step + 1) / 8.0D) * 0.85D;
            for (double y = -HEIGHT / 2.0D; y <= HEIGHT / 2.0D; y += 0.4D) {
                double width = edge * Math.cos(y / HEIGHT * Math.PI);
                s.dust(width, CENTER_Y + y, 0.1D, EDGE, 1.0F);
                s.dust(-width, CENTER_Y + y, 0.1D, EDGE, 1.0F);
            }
            if (step % 5 == 0) {
                tendril(s, step);
            }
        });
        s.every(12, 8, 4, step -> s.sound("block.portal.ambient", 0.8F, 0.5F + step * 0.15F));
        s.at(26, () -> s.sound("entity.enderman.scream", 0.8F, 0.5F));
        s.at(42, () -> {
            s.move(rift[0], EffectMath.box(0, CENTER_Y, 0, 0.01D, HEIGHT, 0.06D, EffectMath.spin(0, 0, 0)), 3);
            s.move(rift[1], EffectMath.box(0, CENTER_Y, 0, 0.01D, HEIGHT, 0.1D, EffectMath.spin(0, 0, 0)), 3);
            s.sound("block.respawn_anchor.deplete", 1.0F, 0.5F);
        });
        s.at(45, () -> {
            s.remove(rift[0]);
            s.remove(rift[1]);
            s.particle(Particles.SONIC_BOOM, 0, CENTER_Y, 0);
            s.particle(Particles.FLASH, 0, CENTER_Y, 0, 1, 0, 0, 0, 0, EDGE, 1.0F);
            s.particle(Particles.REVERSE_PORTAL, 0, CENTER_Y, 0, 80, 0.2D, 0.8D);
            s.sound("entity.warden.sonic_boom", 1.0F, 0.8F);
            s.sound("block.end_portal.spawn", 0.5F, 1.2F);
            s.shake();
        });
        s.every(46, 2, 8, step -> {
            s.particle(Particles.SQUID_INK, s.random(-1, 1), CENTER_Y + s.random(-1, 1), s.random(-1, 1), 3, 0.2D, 0.01D);
            s.ring(Particles.DUST, 0.15D, 0.5D + step * 0.4D, 16, step * 0.3D, VOID, 1.5F);
        });
    }

    private static void tendril(EffectSession s, int seed) {
        double angle = seed * 1.7D;
        double endX = Math.cos(angle) * 4.0D;
        double endZ = Math.sin(angle) * 4.0D;
        double endY = 0.3D + (seed % 3) * 0.8D;
        double controlX = endX * 0.5D + Math.sin(angle) * 1.5D;
        double controlZ = endZ * 0.5D - Math.cos(angle) * 1.5D;
        for (int point = 0; point <= 12; point++) {
            double t = point / 12.0D;
            double inverse = 1.0D - t;
            double x = 2.0D * inverse * t * controlX + t * t * endX;
            double y = inverse * inverse * CENTER_Y + 2.0D * inverse * t * (CENTER_Y + 1.0D) + t * t * endY;
            double z = 2.0D * inverse * t * controlZ + t * t * endZ;
            s.dust(x, y, z, point % 2 == 0 ? VOID : EDGE, 1.2F);
        }
        s.particle(Particles.SCULK_SOUL, endX, endY, endZ, 1, 0.0D, 0.02D);
    }
}
