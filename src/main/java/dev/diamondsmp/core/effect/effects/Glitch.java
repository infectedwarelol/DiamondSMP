package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.TextDisplay;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Glitch implements KillEffect {

    private static final String GLYPHS = "01アイウエオカキクケコサシスセソタチツテト#$%&?";
    private static final Material[] CORRUPTION = {
            Material.LIME_CONCRETE, Material.BLACK_CONCRETE, Material.MAGENTA_CONCRETE, Material.CYAN_CONCRETE
    };
    private static final Color MATRIX = Color.fromRGB(0x00FF41);
    private static final int COLUMNS = 8;
    private static final int COLUMN_LENGTH = 6;
    private static final double COLUMN_RADIUS = 2.6D;

    @Override
    public String id() {
        return "glitch";
    }

    @Override
    public String name() {
        return "&a&lGlitch";
    }

    @Override
    public Material icon() {
        return Material.COMMAND_BLOCK;
    }

    @Override
    public List<String> description() {
        return List.of("&7Reality corrupts, the matrix rains", "&7down and your victim is deleted.");
    }

    @Override
    public void play(EffectSession s) {
        TextDisplay[] columns = new TextDisplay[COLUMNS];
        BlockDisplay[] body = new BlockDisplay[2];
        s.at(0, () -> {
            for (int index = 0; index < COLUMNS; index++) {
                double angle = EffectMath.TAU * index / COLUMNS;
                columns[index] = s.text(code(), EffectMath.item(Math.cos(angle) * COLUMN_RADIUS, 4.5D,
                        Math.sin(angle) * COLUMN_RADIUS, 1.2D, EffectMath.spin(0, 0, 0)));
            }
            body[0] = s.block(Material.LIME_CONCRETE, EffectMath.box(0, 0.45D, 0, 0.5D, 0.9D, 0.3D, EffectMath.spin(0, 0, 0)));
            body[1] = s.block(Material.LIME_CONCRETE, EffectMath.box(0, 1.35D, 0, 0.6D, 0.9D, 0.35D, EffectMath.spin(0, 0, 0)));
            s.sound("block.beacon.activate", 0.8F, 2.0F);
        });
        s.every(1, 2, 20, step -> {
            for (int index = 0; index < COLUMNS; index++) {
                TextDisplay column = columns[index];
                if (column != null && column.isValid()) {
                    column.text(code());
                    double angle = EffectMath.TAU * index / COLUMNS + step * 0.05D;
                    s.move(column, EffectMath.item(Math.cos(angle) * COLUMN_RADIUS, 4.5D - (step % 10) * 0.3D,
                            Math.sin(angle) * COLUMN_RADIUS, 1.2D, EffectMath.spin(0, 0, 0)), 2);
                }
            }
            Material corruption = CORRUPTION[ThreadLocalRandom.current().nextInt(CORRUPTION.length)];
            for (int part = 0; part < body.length; part++) {
                BlockDisplay piece = body[part];
                if (piece != null && piece.isValid()) {
                    piece.setBlock(corruption.createBlockData());
                    double height = part == 0 ? 0.45D : 1.35D;
                    s.move(piece, EffectMath.box(s.random(-0.15, 0.15), height + s.random(-0.1, 0.1), s.random(-0.15, 0.15),
                            part == 0 ? 0.5D : 0.6D, 0.9D, part == 0 ? 0.3D : 0.35D,
                            EffectMath.spin(s.random(-0.2, 0.2), 0, s.random(-0.15, 0.15))), 1);
                }
            }
            s.sound("block.note_block.bit", 0.7F, (float) s.random(0.5, 2.0));
        });
        s.every(0, 1, 40, step -> {
            int pixels = s.count(4);
            for (int pixel = 0; pixel < pixels; pixel++) {
                s.dust(s.random(-1.2, 1.2), s.random(0, 2.2), s.random(-1.2, 1.2), step % 3 == 0 ? Color.FUCHSIA : MATRIX, 1.0F);
            }
            if (step % 4 == 0) {
                s.particle(Particles.ELECTRIC_SPARK, 0, 1, 0, 6, 0.5D, 0.3D);
            }
        });
        s.every(10, 7, 4, step -> s.sound("entity.enderman.teleport", 0.6F, 0.5F + step * 0.25F));
        TextDisplay[] deleted = new TextDisplay[1];
        s.at(40, () -> {
            for (BlockDisplay piece : body) {
                s.move(piece, EffectMath.box(0, 0.9D, 0, 0.01D, 1.8D, 0.01D, EffectMath.spin(0, 0, 0)), 3);
            }
            deleted[0] = s.text(Component.text("[ DELETED ]", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                    EffectMath.item(0, 2.6D, 0, 1.6D, EffectMath.spin(0, 0, 0)));
            s.particle(Particles.FLASH, 0, 1, 0, 1, 0, 0, 0, 0, MATRIX, 1.0F);
            s.particle(Particles.SONIC_BOOM, 0, 1, 0);
            s.sound("block.beacon.deactivate", 1.0F, 0.5F);
            s.sound("entity.zombie_villager.cure", 0.5F, 2.0F);
            s.shake();
        });
        s.every(42, 2, 6, step -> {
            if (deleted[0] != null && deleted[0].isValid()) {
                deleted[0].text(Component.text(step % 2 == 0 ? "[ DELETED ]" : "[ D3L3T3D ]",
                        step % 2 == 0 ? NamedTextColor.RED : NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));
            }
            s.sound("block.note_block.bass", 0.8F, 0.5F + step * 0.1F);
        });
        s.at(54, () -> {
            for (TextDisplay column : columns) {
                s.move(column, EffectMath.item(0, 0, 0, 0.01D, EffectMath.spin(0, 0, 0)), 4);
            }
            s.move(deleted[0], EffectMath.item(0, 2.6D, 0, 0.01D, EffectMath.spin(0, 0, 0)), 4);
        });
    }

    private static Component code() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder builder = new StringBuilder(COLUMN_LENGTH * 2);
        for (int index = 0; index < COLUMN_LENGTH; index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(GLYPHS.charAt(random.nextInt(GLYPHS.length())));
        }
        return Component.text(builder.toString(), TextColor.color(random.nextBoolean() ? 0x00FF41 : 0x008F11));
    }
}
