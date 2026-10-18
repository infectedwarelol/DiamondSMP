package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.joml.Vector3f;

import java.util.List;

public final class DragonsBreath implements KillEffect {

    private static final Color PURPLE = Color.fromRGB(0xB24BF3);
    private static final Color MAGENTA = Color.fromRGB(0xFF4FD8);
    private static final Vector3f START = new Vector3f(-9.0F, 6.0F, 2.0F);
    private static final Vector3f PERCH = new Vector3f(-3.5F, 3.0F, 0.0F);
    private static final double HEAD_SCALE = 3.0D;

    @Override
    public String id() {
        return "dragons_breath";
    }

    @Override
    public String name() {
        return "&d&lDragon's Breath";
    }

    @Override
    public Material icon() {
        return Material.DRAGON_HEAD;
    }

    @Override
    public List<String> description() {
        return List.of("&7A dragon swoops in and scorches", "&7your victim with a torrent of breath.");
    }

    @Override
    public void play(EffectSession s) {
        ItemDisplay[] head = new ItemDisplay[1];
        s.at(0, () -> {
            head[0] = s.item(Material.DRAGON_HEAD, EffectMath.item(START.x, START.y, START.z, HEAD_SCALE,
                    EffectMath.spin(-Math.PI / 2.0D, 0.3D, 0)));
            s.sound("entity.ender_dragon.flap", 1.0F, 0.8F);
        });
        s.at(1, () -> s.move(head[0], EffectMath.item(PERCH.x, PERCH.y, PERCH.z, HEAD_SCALE,
                EffectMath.spin(-Math.PI / 2.0D, 0.45D, 0)), 14));
        s.every(1, 1, 14, step -> {
            double t = step / 14.0D;
            s.particle(Particles.DRAGON_BREATH, EffectMath.lerp(START.x, PERCH.x, t), EffectMath.lerp(START.y, PERCH.y, t),
                    EffectMath.lerp(START.z, PERCH.z, t), 3, 0.3D, 0.01D);
        });
        s.every(2, 5, 3, step -> s.sound("entity.ender_dragon.flap", 1.0F, 0.9F + step * 0.1F));
        s.at(15, () -> {
            s.sound("entity.ender_dragon.growl", 1.0F, 1.0F);
            s.particle(Particles.EXPLOSION, PERCH.x, PERCH.y, PERCH.z, 1, 0.0D, 0.0D);
        });
        s.every(18, 1, 22, step -> {
            int jets = s.count(8);
            for (int jet = 0; jet < jets; jet++) {
                double spread = 0.12D + step * 0.01D;
                double vx = 3.5D;
                double vy = -2.0D + s.random(-spread, spread) * 6.0D;
                double vz = s.random(-spread, spread) * 6.0D;
                s.motion(jet % 3 == 0 ? Particles.FLAME : Particles.DRAGON_BREATH, PERCH.x + 0.6D, PERCH.y - 0.4D, PERCH.z,
                        vx, vy, vz, 0.18D);
            }
            s.particle(Particles.DUST, s.random(-1.5, 1.5), s.random(0, 1.5), s.random(-1.5, 1.5), 2, 0.2D, 0.2D, 0.2D,
                    0.0D, step % 2 == 0 ? PURPLE : MAGENTA, 1.6F);
        });
        s.every(18, 6, 4, step -> {
            s.sound("entity.ender_dragon.shoot", 1.0F, 0.8F + step * 0.1F);
            s.sound("entity.blaze.shoot", 0.7F, 0.6F);
        });
        s.at(22, () -> {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz <= 5) {
                        s.floor(dx, dz, (dx + dz) % 2 == 0 ? Material.CRYING_OBSIDIAN : Material.PURPUR_BLOCK);
                    }
                }
            }
        });
        s.at(40, () -> {
            int total = s.count(50);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(Particles.DRAGON_BREATH, 0, 1, 0, direction.x, Math.abs(direction.y), direction.z, 0.25D);
            }
            s.particle(Particles.EXPLOSION, 0, 1, 0, 3, 0.6D, 0.0D);
            s.sound("entity.dragon_fireball.explode", 1.0F, 0.8F);
            s.sound("entity.ender_dragon.growl", 0.8F, 0.6F);
            s.shake();
            s.flair(Component.text("DRAGON'S BREATH", TextColor.color(0xB24BF3)).decorate(TextDecoration.BOLD),
                    Component.text(s.victimName() + " was reduced to ash", TextColor.color(0x8E8E8E)));
            s.move(head[0], EffectMath.item(-6.0D, 11.0D, -3.0D, 0.01D, EffectMath.spin(-Math.PI / 2.0D, -0.6D, 0)), 14);
        });
        s.every(41, 2, 10, step -> {
            double angle = step * 0.7D;
            double radius = 0.5D + step * 0.2D;
            s.particle(Particles.DRAGON_BREATH, Math.cos(angle) * radius, 0.3D + step * 0.1D, Math.sin(angle) * radius,
                    4, 0.3D, 0.0D);
        });
    }
}
