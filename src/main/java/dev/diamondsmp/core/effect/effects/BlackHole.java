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
import org.joml.Vector3f;

import java.util.List;

public final class BlackHole implements KillEffect {

    private static final Color DISK = Color.fromRGB(0x5B2A86);
    private static final Color EDGE = Color.fromRGB(0xC77DFF);
    private static final double CORE_Y = 1.2D;

    @Override
    public String id() {
        return "black_hole";
    }

    @Override
    public String name() {
        return "&5&lBlack Hole";
    }

    @Override
    public Material icon() {
        return Material.ENDER_EYE;
    }

    @Override
    public List<String> description() {
        return List.of("&7Space folds in on itself, swallows", "&7your victim and erupts outward.");
    }

    @Override
    public void play(EffectSession s) {
        BlockDisplay[] core = new BlockDisplay[1];
        ArmorStand[] body = new ArmorStand[1];
        s.at(0, () -> {
            body[0] = s.corpse(CorpsePose.FLOATING, 0, 0, 0, 0.0F);
            core[0] = s.block(Material.BLACK_CONCRETE, EffectMath.cube(0, CORE_Y, 0, 0.02D, EffectMath.spin(0, 0, 0)));
            s.sound("block.portal.trigger", 0.7F, 0.5F);
            s.sound("block.beacon.deactivate", 1.0F, 0.5F);
        });
        s.every(1, 4, 8, step -> s.move(core[0], EffectMath.cube(0, CORE_Y, 0, 0.15D * (step + 1),
                EffectMath.spin(step * 0.9D, step * 0.5D, step * 0.3D)), 4));
        s.every(0, 1, 36, step -> {
            double progress = step / 36.0D;
            double radius = EffectMath.lerp(4.5D, 0.4D, EffectMath.easeIn(progress));
            for (int arm = 0; arm < 4; arm++) {
                double angle = step * 0.45D + arm * Math.PI / 2.0D;
                s.particle(Particles.REVERSE_PORTAL, Math.cos(angle) * radius, CORE_Y + Math.sin(step * 0.3D) * 0.2D,
                        Math.sin(angle) * radius, 2, 0.05D, 0.0D);
                s.dust(Math.cos(angle + 0.4D) * radius * 0.8D, CORE_Y, Math.sin(angle + 0.4D) * radius * 0.8D,
                        arm % 2 == 0 ? DISK : EDGE, 1.6F);
            }
            s.ring(Particles.DUST, CORE_Y, 1.6D - progress * 0.8D, 18, -step * 0.25D, DISK, 1.1F);
            s.particle(Particles.SQUID_INK, s.random(-3, 3), s.random(0, 3), s.random(-3, 3), 1, 0.0D, 0.0D);
        });
        s.every(0, 6, 6, step -> s.sound("entity.warden.heartbeat", 1.0F, 0.6F + step * 0.18F));
        s.every(2, 3, 11, step -> s.place(body[0], 0, (step + 1) * 0.06D, 0, (step + 1) * 32.0F));
        s.at(22, () -> s.sound("entity.warden.sonic_charge", 1.0F, 0.8F));
        s.at(36, () -> s.remove(body[0]));
        s.at(36, () -> s.move(core[0], EffectMath.cube(0, CORE_Y, 0, 0.05D, EffectMath.spin(3, 2, 1)), 4));
        s.at(40, () -> {
            s.remove(core[0]);
            s.particle(Particles.SONIC_BOOM, 0, CORE_Y, 0);
            s.particle(Particles.FLASH, 0, CORE_Y, 0, 1, 0, 0, 0, 0, EDGE, 1.0F);
            int total = s.count(70);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(index % 3 == 0 ? Particles.END_ROD : Particles.REVERSE_PORTAL, 0, CORE_Y, 0,
                        direction.x, direction.y, direction.z, 0.55D);
            }
            s.sound("entity.warden.sonic_boom", 1.0F, 0.7F);
            s.sound("entity.generic.explode", 0.8F, 0.5F);
            s.sound("block.respawn_anchor.deplete", 1.0F, 0.6F);
            s.shake();
        });
        s.every(41, 1, 22, step -> {
            double radius = 0.6D + EffectMath.easeOut(step / 22.0D) * 5.5D;
            s.ring(Particles.LARGE_SMOKE, 0.15D, radius, 20, step * 0.1D, null, 1.0F);
            s.ring(Particles.DUST, 0.3D, radius * 0.92D, 14, -step * 0.1D, DISK, 1.3F);
        });
        s.every(44, 4, 5, step -> s.sound("block.portal.ambient", 0.4F, 1.6F - step * 0.2F));
    }
}
