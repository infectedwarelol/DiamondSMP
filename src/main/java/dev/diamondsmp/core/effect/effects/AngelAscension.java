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

public final class AngelAscension implements KillEffect {

    private static final Color GOLD = Color.fromRGB(0xFFD86B);
    private static final Color HOLY = Color.fromRGB(0xFFFBEA);
    private static final float[] ARPEGGIO = {0.5F, 0.63F, 0.75F, 1.0F, 1.26F, 1.5F, 2.0F};
    private static final int FEATHERS = 6;
    private static final double RISE = 6.0D;

    @Override
    public String id() {
        return "angel_ascension";
    }

    @Override
    public String name() {
        return "&e&lAngel Ascension";
    }

    @Override
    public Material icon() {
        return Material.FEATHER;
    }

    @Override
    public List<String> description() {
        return List.of("&7Heaven opens, wings unfold and", "&7your victim ascends in golden light.");
    }

    @Override
    public void play(EffectSession s) {
        s.every(0, 1, 14, step -> {
            double bottom = 16.0D - step * 1.15D;
            s.line(Particles.END_ROD, 0, 16.0D, 0, 0, Math.max(0.0D, bottom), 0, 0.6D, null, 1.0F);
            s.ring(Particles.DUST, Math.max(0.0D, bottom), 0.6D, 10, step * 0.4D, GOLD, 1.2F);
        });
        s.at(0, () -> s.sound("block.beacon.power_select", 1.0F, 1.4F));
        s.at(14, () -> {
            s.sound("block.beacon.activate", 1.0F, 1.6F);
            s.particle(Particles.FLASH, 0, 1, 0, 1, 0, 0, 0, 0, GOLD, 1.0F);
        });
        s.every(14, 1, 44, step -> {
            double lift = EffectMath.easeInOut(step / 44.0D) * RISE;
            double flap = Math.sin(step * 0.35D) * 0.45D;
            drawWings(s, 1.2D + lift, flap);
            s.ring(Particles.DUST, 2.35D + lift, 0.42D, 14, step * 0.2D, GOLD, 0.9F);
            if (step % 2 == 0) {
                s.particle(Particles.END_ROD, s.random(-0.5, 0.5), lift, s.random(-0.5, 0.5), 1, 0.0D, 0.0D);
            }
        });
        for (int note = 0; note < ARPEGGIO.length; note++) {
            float pitch = ARPEGGIO[note];
            s.at(16 + note * 5, () -> {
                s.sound("block.note_block.chime", 0.9F, pitch);
                s.sound("block.note_block.bell", 0.4F, pitch);
            });
        }
        s.at(20, () -> s.sound("entity.allay.ambient_with_item", 1.0F, 1.2F));
        ItemDisplay[] feathers = new ItemDisplay[FEATHERS];
        s.at(16, () -> {
            for (int index = 0; index < FEATHERS; index++) {
                double angle = EffectMath.TAU * index / FEATHERS;
                feathers[index] = s.item(Material.FEATHER, EffectMath.item(Math.cos(angle) * 0.8D, 4.5D,
                        Math.sin(angle) * 0.8D, 0.6D, EffectMath.spin(angle, 0.4D, 0)));
            }
        });
        s.every(17, 6, 7, step -> {
            for (int index = 0; index < FEATHERS; index++) {
                double angle = EffectMath.TAU * index / FEATHERS + (step + 1) * 0.6D;
                double radius = 0.8D + (step + 1) * 0.35D;
                s.move(feathers[index], EffectMath.item(Math.cos(angle) * radius, Math.max(0.1D, 4.5D - (step + 1) * 0.65D),
                        Math.sin(angle) * radius, 0.6D, EffectMath.spin(angle, 0.4D + step * 0.3D, Math.sin(step) * 0.4D)), 6);
            }
        });
        s.at(58, () -> {
            double top = 1.2D + RISE;
            s.particle(Particles.FLASH, 0, top, 0, 1, 0, 0, 0, 0, HOLY, 1.0F);
            s.particle(Particles.TOTEM, 0, top, 0, 60, 0.6D, 0.5D);
            int total = s.count(40);
            for (int index = 0; index < total; index++) {
                Vector3f direction = EffectMath.sphere(index, total);
                s.motion(Particles.END_ROD, 0, top, 0, direction.x, direction.y, direction.z, 0.35D);
            }
            s.sound("ui.toast.challenge_complete", 0.8F, 1.2F);
            s.sound("block.beacon.power_select", 1.0F, 2.0F);
            s.flair(Component.text("✟ ASCENDED ✟", TextColor.color(0xFFD86B)).decorate(TextDecoration.BOLD),
                    Component.text(s.victimName() + " has been sent above", TextColor.color(0xFFFBEA)));
        });
    }

    private static void drawWings(EffectSession s, double centerY, double flap) {
        for (int side = -1; side <= 1; side += 2) {
            for (double t = 0.0D; t <= Math.PI; t += Math.PI / 9.0D) {
                double reach = Math.sin(t) * (1.4D - 0.3D * Math.sin(t * 3.0D));
                double height = Math.cos(t) * 0.9D + Math.sin(t * 2.0D) * 0.2D;
                double x = side * (0.25D + reach * Math.cos(flap));
                double z = -0.3D - reach * Math.sin(flap) * 0.8D;
                s.dust(x, centerY + height, z, t > Math.PI * 0.6D ? GOLD : HOLY, 1.1F);
            }
        }
    }
}
