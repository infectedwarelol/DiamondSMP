package dev.diamondsmp.core.effect;

import org.bukkit.Material;

import java.util.List;

public interface KillEffect {

    String id();

    String name();

    Material icon();

    List<String> description();

    void play(EffectSession session);
}
