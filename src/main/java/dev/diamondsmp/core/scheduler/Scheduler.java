package dev.diamondsmp.core.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public final class Scheduler {

    private static final long MILLIS_PER_TICK = 50L;

    private final Plugin plugin;
    private final boolean regionized;
    private final Set<ScheduledTask> pending = ConcurrentHashMap.newKeySet();

    public Scheduler(Plugin plugin, boolean regionized) {
        this.plugin = plugin;
        this.regionized = regionized;
    }

    public void global(Runnable action) {
        if (!regionized && Bukkit.isPrimaryThread()) {
            action.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getGlobalRegionScheduler().execute(plugin, action);
    }

    public Task globalTimer(Runnable action, long delayTicks, long periodTicks) {
        if (!plugin.isEnabled()) {
            return Task.NONE;
        }
        return remember(Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, task -> action.run(), ticks(delayTicks), ticks(periodTicks)));
    }

    public void region(Location location, Runnable action) {
        if (Bukkit.isOwnedByCurrentRegion(location)) {
            action.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, location, action);
    }

    public void region(World world, int chunkX, int chunkZ, Runnable action) {
        if (Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ)) {
            action.run();
            return;
        }
        if (!plugin.isEnabled()) {
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, action);
    }

    public void regionRepeat(Location location, long periodTicks, int times, IntConsumer step) {
        if (times <= 0 || !plugin.isEnabled()) {
            return;
        }
        int[] counter = {0};
        ScheduledTask scheduled = Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, task -> {
            int index = counter[0]++;
            if (index >= times) {
                finish(task);
                return;
            }
            step.accept(index);
            if (index + 1 >= times) {
                finish(task);
            }
        }, 1L, ticks(periodTicks));
        remember(scheduled);
    }

    public void entity(Entity entity, Runnable action) {
        entity(entity, action, null);
    }

    public void entity(Entity entity, Runnable action, Runnable retired) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            action.run();
            return;
        }
        if (!plugin.isEnabled()) {
            if (retired != null) {
                retired.run();
            }
            return;
        }
        ScheduledTask scheduled = entity.getScheduler().run(plugin, task -> action.run(), retired);
        if (scheduled == null && retired != null) {
            retired.run();
        }
    }

    public void entityLater(Entity entity, Runnable action, long delayTicks) {
        once(consumer -> entity.getScheduler().runDelayed(plugin, consumer, null, ticks(delayTicks)), action);
    }

    public Task entityTimer(Entity entity, Runnable action, long delayTicks, long periodTicks) {
        if (!plugin.isEnabled()) {
            return Task.NONE;
        }
        return remember(entity.getScheduler()
                .runAtFixedRate(plugin, task -> action.run(), null, ticks(delayTicks), ticks(periodTicks)));
    }

    public void asyncLater(Runnable action, long delayTicks) {
        once(consumer -> Bukkit.getAsyncScheduler().runDelayed(plugin, consumer,
                ticks(delayTicks) * MILLIS_PER_TICK, TimeUnit.MILLISECONDS), action);
    }

    public Task asyncTimer(Runnable action, long delayTicks, long periodTicks) {
        if (!plugin.isEnabled()) {
            return Task.NONE;
        }
        return remember(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> action.run(),
                ticks(delayTicks) * MILLIS_PER_TICK, ticks(periodTicks) * MILLIS_PER_TICK, TimeUnit.MILLISECONDS));
    }

    public void shutdown() {
        for (ScheduledTask task : pending) {
            task.cancel();
        }
        pending.clear();
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }

    private void once(Function<Consumer<ScheduledTask>, ScheduledTask> submit, Runnable action) {
        if (!plugin.isEnabled()) {
            return;
        }
        ScheduledTask scheduled = submit.apply(task -> {
            pending.remove(task);
            action.run();
        });
        remember(scheduled);
    }

    private Task remember(ScheduledTask scheduled) {
        if (scheduled == null) {
            return Task.NONE;
        }
        pending.add(scheduled);
        if (isDone(scheduled)) {
            pending.remove(scheduled);
        }
        return () -> finish(scheduled);
    }

    private void finish(ScheduledTask task) {
        task.cancel();
        pending.remove(task);
    }

    private static boolean isDone(ScheduledTask task) {
        ScheduledTask.ExecutionState state = task.getExecutionState();
        return state == ScheduledTask.ExecutionState.FINISHED
                || state == ScheduledTask.ExecutionState.CANCELLED
                || state == ScheduledTask.ExecutionState.CANCELLED_RUNNING;
    }

    private static long ticks(long value) {
        return Math.max(1L, value);
    }
}
