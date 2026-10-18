package dev.diamondsmp.core.effect;

import dev.diamondsmp.core.effect.effects.AngelAscension;
import dev.diamondsmp.core.effect.effects.BlackHole;
import dev.diamondsmp.core.effect.effects.BloodMoon;
import dev.diamondsmp.core.effect.effects.ChainBind;
import dev.diamondsmp.core.effect.effects.DragonsBreath;
import dev.diamondsmp.core.effect.effects.FrozenShatter;
import dev.diamondsmp.core.effect.effects.Glitch;
import dev.diamondsmp.core.effect.effects.InfernoTornado;
import dev.diamondsmp.core.effect.effects.MeteorStrike;
import dev.diamondsmp.core.effect.effects.Shatter;
import dev.diamondsmp.core.effect.effects.SoulHarvest;
import dev.diamondsmp.core.effect.effects.Supernova;
import dev.diamondsmp.core.effect.effects.ThunderGod;
import dev.diamondsmp.core.effect.effects.VoidRift;

import java.util.List;

public final class KillEffectRegistry {

    private final List<KillEffect> effects = List.of(
            new ChainBind(),
            new BlackHole(),
            new ThunderGod(),
            new SoulHarvest(),
            new BloodMoon(),
            new DragonsBreath(),
            new Glitch(),
            new Shatter(),
            new MeteorStrike(),
            new AngelAscension(),
            new VoidRift(),
            new Supernova(),
            new FrozenShatter(),
            new InfernoTornado());

    public List<KillEffect> all() {
        return effects;
    }
}
