package dev.diamondsmp.core.effect;

import dev.diamondsmp.core.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;

public record KillEffect(String id, String name, Material icon, List<String> description, Animation animation) {

    @FunctionalInterface
    public interface Animation {

        void play(Scheduler scheduler, Location location);
    }

    public KillEffect {
        description = List.copyOf(description);
    }
}
