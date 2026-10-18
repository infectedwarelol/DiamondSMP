package dev.diamondsmp.core.effect.effects;

import dev.diamondsmp.core.effect.CorpsePose;
import dev.diamondsmp.core.effect.EffectMath;
import dev.diamondsmp.core.effect.EffectSession;
import dev.diamondsmp.core.effect.KillEffect;
import dev.diamondsmp.core.effect.Particles;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;

import java.util.List;

public final class Guillotine implements KillEffect {

    private static final Color BLOOD = Color.fromRGB(0x8A0303);
    private static final double FRAME_Z = 0.55D;
    private static final double BLADE_TOP = 3.05D;
    private static final double BLADE_BOTTOM = 0.75D;
    private static final double NECK_Y = 0.95D;

    @Override
    public String id() {
        return "guillotine";
    }

    @Override
    public String name() {
        return "&7&lGuillotine";
    }

    @Override
    public Material icon() {
        return Material.IRON_AXE;
    }

    @Override
    public List<String> description() {
        return List.of("&7A guillotine rises, the drums roll", "&7and the blade comes down. Off with it!");
    }

    @Override
    public void play(EffectSession s) {
        ArmorStand[] body = new ArmorStand[1];
        BlockDisplay[] blade = new BlockDisplay[1];
        s.at(0, () -> {
            body[0] = s.corpse(CorpsePose.KNEELING, 0, 0, 0.0F);
            s.block(Material.DARK_OAK_LOG, EffectMath.box(-0.85D, 1.7D, FRAME_Z, 0.25D, 3.4D, 0.25D, EffectMath.spin(0, 0, 0)));
            s.block(Material.DARK_OAK_LOG, EffectMath.box(0.85D, 1.7D, FRAME_Z, 0.25D, 3.4D, 0.25D, EffectMath.spin(0, 0, 0)));
            s.block(Material.DARK_OAK_PLANKS, EffectMath.box(0, 3.45D, FRAME_Z, 2.0D, 0.3D, 0.35D, EffectMath.spin(0, 0, 0)));
            s.block(Material.DARK_OAK_SLAB, EffectMath.box(0, 0.62D, FRAME_Z, 1.45D, 0.2D, 0.4D, EffectMath.spin(0, 0, 0)));
            blade[0] = s.block(Material.IRON_BLOCK, EffectMath.box(0, BLADE_TOP, FRAME_Z, 1.45D, 0.45D, 0.06D,
                    EffectMath.spin(0, 0, 0.3D)));
            s.sound("block.wood.place", 1.0F, 0.6F);
            s.sound("block.anvil.place", 0.6F, 1.4F);
        });
        s.every(4, 2, 12, step -> s.sound("block.note_block.snare", 0.4F + step * 0.05F, 0.9F + step * 0.03F));
        s.every(4, 1, 22, step -> s.particle(Particles.SMOKE, s.random(-1.5, 1.5), 0.1D, s.random(-1.5, 1.5), 1, 0.05D, 0.0D));
        s.at(26, () -> s.sound("block.note_block.basedrum", 1.0F, 0.5F));
        s.at(28, () -> {
            s.move(blade[0], EffectMath.box(0, BLADE_BOTTOM, FRAME_Z, 1.45D, 0.45D, 0.06D, EffectMath.spin(0, 0, 0.3D)), 3);
            s.sound("entity.player.attack.sweep", 1.0F, 0.6F);
        });
        ItemDisplay[] head = new ItemDisplay[1];
        s.at(31, () -> {
            s.decapitate(body[0]);
            head[0] = s.head(EffectMath.item(0, NECK_Y + 0.3D, 0.2D, 0.75D, EffectMath.spin(0, 0, 0)));
            s.particle(Particles.DUST, 0, NECK_Y, 0.2D, 40, 0.25D, 0.25D, 0.25D, 0.0D, BLOOD, 1.6F);
            s.particle(Particles.DAMAGE, 0, NECK_Y, 0.2D, 10, 0.3D, 0.1D);
            s.particle(Particles.CRIT, 0, NECK_Y, 0.4D, 20, 0.3D, 0.3D);
            s.sound("block.anvil.land", 1.0F, 1.5F);
            s.sound("entity.player.hurt", 1.0F, 0.6F);
            s.sound("entity.generic.big_fall", 1.0F, 0.8F);
            s.shake();
        });
        s.at(32, () -> s.move(head[0], EffectMath.item(0.6D, 1.6D, 1.4D, 0.75D, EffectMath.spin(1.5D, 2.0D, 0.5D)), 5));
        s.at(37, () -> s.move(head[0], EffectMath.item(1.2D, 0.25D, 2.2D, 0.75D, EffectMath.spin(3.0D, 4.0D, 1.0D)), 5));
        s.at(42, () -> s.move(head[0], EffectMath.item(1.6D, 0.25D, 2.6D, 0.75D, EffectMath.spin(4.0D, 5.6D, 1.6D)), 8));
        s.at(42, () -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 0; dz <= 2; dz++) {
                    s.floor(dx, dz, Material.REDSTONE_BLOCK);
                }
            }
            s.sound("block.honey_block.slide", 1.0F, 0.6F);
        });
        s.every(32, 2, 14, step -> s.particle(Particles.DUST, s.random(-0.2, 0.2), NECK_Y - step * 0.04D,
                s.random(0.0, 0.3), 3, 0.05D, 0.05D, 0.05D, 0.0D, BLOOD, 1.0F));
        s.at(48, () -> s.sound("block.bell.use", 0.8F, 0.6F));
    }
}
