package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector3f;

import java.util.List;

public final class BloodMoon implements KillEffect {

    private static final Color BLOOD = Color.fromRGB(0x8A0303);
    private static final Color CRIMSON = Color.fromRGB(0xD7263D);
    private static final double MOON_Y = 11.0D;

    @Override
    public String id() {
        return "blood_moon";
    }

    @Override
    public String name() {
        return "&4&lBlood Moon";
    }

    @Override
    public Material icon() {
        return Material.REDSTONE_BLOCK;
    }

    @Override
    public List<String> description() {
        return List.of("&7A crimson moon rises, blood rains", "&7down and the ground is stained red.");
    }

    @Override
    public void play(EffectSession s) {
        TextDisplay[] moon = new TextDisplay[1];
        s.at(0, () -> {
            moon[0] = s.text(Component.text("●", TextColor.color(0xB3001B)),
                    EffectMath.item(0, MOON_Y, 0, 0.01D, EffectMath.spin(0, 0, 0)));
            s.sound("ambient.cave", 1.0F, 0.5F);
            s.sound("entity.wither.ambient", 0.6F, 0.4F);
        });
        s.at(1, () -> s.move(moon[0], EffectMath.item(0, MOON_Y, 0, 9.0D, EffectMath.spin(0, 0, 0)), 15));
        s.every(0, 1, 15, step -> {
            double height = EffectMath.lerp(0.2D, MOON_Y, step / 15.0D);
            s.particle(Particles.DUST, 0, height, 0, 3, 0.15D, 0.15D, 0.15D, 0.0D, BLOOD, 1.5F);
        });
        s.every(15, 1, 35, step -> {
            int drops = s.count(6);
            for (int drop = 0; drop < drops; drop++) {
                double x = s.random(-4.5, 4.5);
                double z = s.random(-4.5, 4.5);
                double top = s.random(2.0, 7.0);
                s.dust(x, top, z, BLOOD, 0.9F);
                s.dust(x, top - 0.25D, z, CRIMSON, 0.7F);
                s.dust(x, top - 0.5D, z, BLOOD, 0.5F);
            }
            s.particle(Particles.LANDING_LAVA, s.random(-3, 3), 0.1D, s.random(-3, 3), 2, 0.2D, 0.0D);
        });
        for (int radius = 0; radius <= 3; radius++) {
            int ringRadius = radius;
            s.at(16 + radius * 4, () -> {
                for (int dx = -ringRadius; dx <= ringRadius; dx++) {
                    for (int dz = -ringRadius; dz <= ringRadius; dz++) {
                        int distance = Math.max(Math.abs(dx), Math.abs(dz));
                        if (distance == ringRadius && dx * dx + dz * dz <= 12) {
                            s.floor(dx, dz, distance % 2 == 0 ? Material.NETHER_WART_BLOCK : Material.RED_NETHER_BRICKS);
                        }
                    }
                }
                s.sound("block.wart_block.place", 0.8F, 0.6F + ringRadius * 0.15F);
            });
        }
        s.every(18, 8, 4, step -> s.sound("entity.warden.heartbeat", 1.0F, 0.5F + step * 0.1F));
        s.at(50, () -> {
            s.move(moon[0], EffectMath.item(0, MOON_Y, 0, 13.0D, EffectMath.spin(0, 0, 0)), 3);
            int total = s.count(80);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                double distance = s.random(0.5, 3.0);
                s.particle(Particles.DUST, direction.x * distance, 1.0D + direction.y * distance, direction.z * distance,
                        1, 0, 0, 0, 0, index % 2 == 0 ? BLOOD : CRIMSON, 2.0F);
            }
            s.particle(Particles.DAMAGE, 0, 1, 0, 25, 0.8D, 0.2D);
            s.sound("entity.wither.death", 0.8F, 0.8F);
            s.sound("entity.generic.explode", 0.7F, 0.5F);
            s.shake();
        });
        s.at(54, () -> s.move(moon[0], EffectMath.item(0, MOON_Y + 4, 0, 0.01D, EffectMath.spin(0, 0, 0)), 12));
        s.every(52, 1, 18, step -> s.particle(Particles.CRIMSON_SPORE, s.random(-3, 3), s.random(0.2, 3), s.random(-3, 3),
                3, 0.2D, 0.0D));
    }
}
