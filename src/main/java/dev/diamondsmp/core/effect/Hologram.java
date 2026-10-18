package dev.diamondsmp.core.effect;

import dev.diamondsmp.core.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public record Hologram(boolean enabled, float scale, int durationTicks, double height, double rise, double spin) {

    private static final long STEP_TICKS = 2L;
    private static final int GROW_STEPS = 4;

    public void show(Scheduler scheduler, Location at, Material material) {
        World world = at.getWorld();
        if (!enabled || world == null || material == null || !material.isItem()) {
            return;
        }
        Location start = at.clone().add(0.0D, height, 0.0D);
        ItemDisplay display = world.spawn(start, ItemDisplay.class);
        display.setPersistent(false);
        display.setItemStack(new ItemStack(material));
        display.setBillboard(Display.Billboard.FIXED);
        display.setBrightness(new Display.Brightness(15, 15));
        display.setTransformation(transform(0.0F, 0.0F, 0.01F));
        int steps = Math.max(GROW_STEPS * 2 + 1, (int) (durationTicks / STEP_TICKS));
        scheduler.regionRepeat(start, STEP_TICKS, steps, step -> {
            if (!display.isValid()) {
                return;
            }
            if (step >= steps - 1) {
                display.remove();
                return;
            }
            int fromEnd = steps - 1 - step;
            float size = scale * Math.min(1.0F, Math.min(step + 1, fromEnd) / (float) GROW_STEPS);
            float lift = (float) (rise * step / steps);
            display.setInterpolationDelay(0);
            display.setInterpolationDuration((int) STEP_TICKS);
            display.setTransformation(transform((float) (spin * step), lift, Math.max(0.01F, size)));
        });
    }

    private static Transformation transform(float angle, float lift, float size) {
        return new Transformation(new Vector3f(0.0F, lift, 0.0F), new AxisAngle4f(angle, 0.0F, 1.0F, 0.0F),
                new Vector3f(size), new AxisAngle4f());
    }
}
