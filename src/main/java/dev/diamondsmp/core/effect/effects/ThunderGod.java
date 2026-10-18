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

import java.util.List;

public final class ThunderGod implements KillEffect {

    private static final Color STORM = Color.fromRGB(0x3A3F55);
    private static final Color VOLT = Color.fromRGB(0x9AE6FF);
    private static final double CLOUD_Y = 7.5D;
    private static final double[] STRIKE_ANGLES = {0.0D, 2.094D, 4.188D};

    @Override
    public String id() {
        return "thunder_god";
    }

    @Override
    public String name() {
        return "&e&lThunder God";
    }

    @Override
    public Material icon() {
        return Material.LIGHTNING_ROD;
    }

    @Override
    public List<String> description() {
        return List.of("&7A storm gathers, three bolts fall", "&7and a hammer of lightning lands.");
    }

    @Override
    public void play(EffectSession s) {
        s.at(0, () -> {
            s.sound("entity.lightning_bolt.thunder", 0.6F, 0.5F);
            s.sound("weather.rain.above", 1.0F, 0.6F);
        });
        s.every(0, 1, 20, step -> {
            double radius = EffectMath.lerp(5.0D, 1.6D, EffectMath.easeOut(step / 20.0D));
            s.ring(Particles.CLOUD, CLOUD_Y, radius, 16, step * 0.2D, null, 1.0F);
            s.ring(Particles.DUST, CLOUD_Y + 0.3D, radius * 0.8D, 12, -step * 0.25D, STORM, 2.4F);
            if (step % 3 == 0) {
                s.particle(Particles.ELECTRIC_SPARK, s.random(-radius, radius), CLOUD_Y - 0.3D,
                        s.random(-radius, radius), 6, 0.3D, 0.05D);
            }
        });
        for (int index = 0; index < STRIKE_ANGLES.length; index++) {
            double angle = STRIKE_ANGLES[index];
            double x = Math.cos(angle) * 3.0D;
            double z = Math.sin(angle) * 3.0D;
            float pitch = 0.9F + index * 0.2F;
            s.at(20 + index * 6, () -> {
                s.lightning(x, z);
                s.line(Particles.ELECTRIC_SPARK, x, CLOUD_Y, z, x, 0.1D, z, 0.35D, null, 1.0F);
                s.ring(Particles.ELECTRIC_SPARK, 0.1D, 1.0D, 10, 0, null, 1.0F);
                s.sound("entity.lightning_bolt.impact", 1.0F, pitch);
            });
        }
        ItemDisplay[] hammer = new ItemDisplay[1];
        s.at(32, () -> {
            hammer[0] = s.item(Material.LIGHTNING_ROD, EffectMath.item(0, CLOUD_Y + 3, 0, 3.2D, EffectMath.spin(0, 0, Math.PI)));
            s.sound("item.trident.thunder", 1.0F, 0.8F);
        });
        s.at(33, () -> s.move(hammer[0], EffectMath.item(0, 1.4D, 0, 3.2D, EffectMath.spin(Math.PI, 0, Math.PI)), 7));
        s.every(33, 1, 7, step -> s.line(Particles.END_ROD, 0, CLOUD_Y + 3 - step * 1.2D, 0, 0,
                CLOUD_Y + 2 - step * 1.2D, 0, 0.25D, null, 1.0F));
        s.at(40, () -> {
            s.lightning(0, 0);
            s.particle(Particles.FLASH, 0, 1, 0, 1, 0, 0, 0, 0, VOLT, 1.0F);
            s.particle(Particles.EXPLOSION, 0, 0.5D, 0, 3, 0.6D, 0.0D);
            s.sound("entity.generic.explode", 1.0F, 0.6F);
            s.sound("entity.lightning_bolt.thunder", 1.0F, 0.8F);
            s.sound("block.anvil.land", 0.8F, 0.5F);
            s.shake();
            s.flair(Component.text("⚡ THUNDER GOD ⚡", TextColor.color(0xFFE066)).decorate(TextDecoration.BOLD),
                    Component.text(s.victimName() + " was struck down", TextColor.color(0x9AE6FF)));
        });
        s.every(41, 1, 14, step -> {
            double radius = 0.8D + EffectMath.easeOut(step / 14.0D) * 7.0D;
            s.ring(Particles.ELECTRIC_SPARK, 0.2D, radius, 28, step * 0.07D, null, 1.0F);
            s.ring(Particles.CRIT, 0.1D, radius - 0.4D, 16, 0, null, 1.0F);
        });
        s.every(42, 3, 6, step -> s.particle(Particles.ELECTRIC_SPARK, 0, 0.5D + step * 0.2D, 0, 8, 0.6D, 0.1D));
        s.at(56, () -> s.move(hammer[0], EffectMath.item(0, 1.4D, 0, 0.01D, EffectMath.spin(0, 0, Math.PI)), 5));
        s.every(46, 6, 3, step -> s.sound("entity.lightning_bolt.thunder", 0.4F - step * 0.1F, 0.5F));
    }
}
