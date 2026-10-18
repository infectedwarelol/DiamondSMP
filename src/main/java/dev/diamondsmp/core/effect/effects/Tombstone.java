package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;

import java.util.List;

public final class Tombstone implements KillEffect {

    private static final Material[] FLOWERS = {
            Material.POPPY, Material.WITHER_ROSE, Material.DANDELION, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY
    };
    private static final Color FOG = Color.fromRGB(0x4A4F57);
    private static final double STONE_Z = -0.9D;

    @Override
    public String id() {
        return "rest_in_peace";
    }

    @Override
    public String name() {
        return "&8&lRest in Peace";
    }

    @Override
    public Material icon() {
        return Material.MOSSY_STONE_BRICKS;
    }

    @Override
    public List<String> description() {
        return List.of("&7The ground cracks, a tombstone rises", "&7and flowers bloom on the grave.");
    }

    @Override
    public void play(EffectSession s) {
        BlockDisplay[] stone = new BlockDisplay[2];
        s.at(0, () -> {
            s.corpse(CorpsePose.SLUMPED, 0, 0.6D, 180.0F);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    s.floor(dx, dz, (dx + dz) % 2 == 0 ? Material.PODZOL : Material.COARSE_DIRT);
                }
            }
            stone[0] = s.block(Material.MOSSY_STONE_BRICKS, EffectMath.box(0, -0.8D, STONE_Z, 1.0D, 1.5D, 0.25D, EffectMath.spin(0, 0, 0)));
            stone[1] = s.block(Material.CHISELED_STONE_BRICKS, EffectMath.box(0, -0.2D, STONE_Z, 0.6D, 0.3D, 0.26D, EffectMath.spin(0, 0, 0)));
            s.sound("block.rooted_dirt.break", 1.0F, 0.6F);
            s.sound("ambient.cave", 1.0F, 0.6F);
        });
        s.every(0, 1, 70, step -> {
            s.particle(Particles.DUST, s.random(-3, 3), s.random(0, 0.6), s.random(-3, 3), 1, 0.2D, 0.05D, 0.2D, 0.0D, FOG, 2.5F);
            if (step < 20) {
                s.particle(Particles.CLOUD, s.random(-0.5, 0.5), 0.1D, STONE_Z, 2, 0.1D, 0.02D);
            }
        });
        s.at(1, () -> {
            s.move(stone[0], EffectMath.box(0, 0.75D, STONE_Z, 1.0D, 1.5D, 0.25D, EffectMath.spin(0, 0, 0.04D)), 18);
            s.move(stone[1], EffectMath.box(0, 1.6D, STONE_Z, 0.6D, 0.3D, 0.26D, EffectMath.spin(0, 0, 0.04D)), 18);
        });
        s.every(1, 4, 5, step -> s.sound("block.stone.place", 0.8F, 0.5F + step * 0.08F));
        s.at(20, () -> {
            s.text(Component.text("R.I.P", TextColor.color(0xD9D9D9)).decorate(TextDecoration.BOLD)
                            .append(Component.newline())
                            .append(Component.text(s.victimName(), TextColor.color(0xA8A8A8))),
                    EffectMath.item(0, 0.9D, STONE_Z + 0.14D, 0.5D, EffectMath.spin(0, 0, 0)), Display.Billboard.FIXED);
            s.sound("block.bell.use", 1.0F, 0.5F);
        });
        ItemDisplay[] flowers = new ItemDisplay[FLOWERS.length];
        for (int index = 0; index < FLOWERS.length; index++) {
            int flower = index;
            double x = -0.9D + flower * 0.45D;
            s.at(26 + flower * 3, () -> {
                flowers[flower] = s.item(FLOWERS[flower], EffectMath.item(x, 0.05D, -0.45D, 0.01D, EffectMath.spin(flower, 0, 0)));
                s.sound("block.azalea.place", 0.8F, 1.0F + flower * 0.1F);
            });
            s.at(27 + flower * 3, () -> s.move(flowers[flower], EffectMath.item(x, 0.3D, -0.45D, 0.6D,
                    EffectMath.spin(flower, 0, 0)), 4));
            s.at(27 + flower * 3, () -> s.particle(Particles.CLOUD, x, 0.2D, -0.45D, 3, 0.05D, 0.01D));
        }
        s.every(30, 2, 18, step -> s.particle(Particles.SOUL, s.random(-0.4, 0.4), 0.5D + step * 0.12D, 0.6D, 1, 0.05D, 0.0D));
        s.at(38, () -> s.sound("entity.vex.ambient", 0.6F, 0.5F));
        s.at(50, () -> s.sound("block.bell.use", 0.8F, 0.5F));
        s.at(62, () -> s.sound("block.bell.resonate", 0.6F, 0.6F));
    }
}
