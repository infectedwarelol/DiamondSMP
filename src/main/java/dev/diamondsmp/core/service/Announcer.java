package dev.diamondsmp.core.service;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.scheduler.Task;
import dev.diamondsmp.core.util.Format;
import dev.diamondsmp.core.util.SoundSpec;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class Announcer implements Service {

    private record Group(List<List<String>> announcements, SoundSpec sound, AtomicInteger next) {
    }

    private final DiamondSMP plugin;
    private final List<Task> timers = new ArrayList<>();

    public Announcer(DiamondSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public synchronized void reload() {
        cancel();
        ConfigurationSection groups = plugin.settings().getConfigurationSection("announcer.groups");
        if (!plugin.settings().getBoolean("announcer.enabled", true) || groups == null) {
            return;
        }
        for (String name : groups.getKeys(false)) {
            ConfigurationSection section = groups.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            List<List<String>> announcements = announcements(section.getList("messages"));
            if (announcements.isEmpty()) {
                continue;
            }
            long seconds = Math.max(10L, Format.parseSeconds(section.getString("interval"), 600L));
            Group group = new Group(announcements, SoundSpec.from(section.getConfigurationSection("sound")),
                    new AtomicInteger());
            timers.add(plugin.scheduler().globalTimer(() -> announce(group), seconds * 20L, seconds * 20L));
        }
    }

    private static List<List<String>> announcements(List<?> raw) {
        List<List<String>> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        for (Object entry : raw) {
            List<String> lines = new ArrayList<>();
            if (entry instanceof List<?> list) {
                for (Object line : list) {
                    lines.add(String.valueOf(line));
                }
            } else if (entry != null) {
                lines.add(String.valueOf(entry));
            }
            if (!lines.isEmpty()) {
                result.add(List.copyOf(lines));
            }
        }
        return result;
    }

    private void announce(Group group) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        List<String> lines = group.announcements().get(
                Math.floorMod(group.next().getAndIncrement(), group.announcements().size()));
        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            Component component = plugin.messages().format(line);
            components.add(component == null ? Component.empty() : component);
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (Component component : components) {
                online.sendMessage(component);
            }
            group.sound().play(online);
        }
    }

    private void cancel() {
        for (Task timer : timers) {
            timer.cancel();
        }
        timers.clear();
    }

    @Override
    public synchronized void shutdown() {
        cancel();
    }
}
