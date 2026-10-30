package dev.diamondsmp.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class ConfigFile {

    private static final String VERSION_KEY = "config-version";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Plugin plugin;
    private final String resource;
    private final File file;
    private final Set<String> freeSections;
    private final Map<String, Set<String>> retiredDefaults;
    private volatile YamlConfiguration configuration = new YamlConfiguration();

    public ConfigFile(Plugin plugin, String resource, Set<String> freeSections,
                      Map<String, Set<String>> retiredDefaults) {
        this.plugin = plugin;
        this.resource = resource;
        this.file = new File(plugin.getDataFolder(), resource);
        this.freeSections = Set.copyOf(freeSections);
        this.retiredDefaults = Map.copyOf(retiredDefaults);
    }

    public YamlConfiguration get() {
        return configuration;
    }

    public synchronized boolean set(String path, Object value) {
        YamlConfiguration current = new YamlConfiguration();
        if (file.isFile()) {
            try {
                current.load(file);
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().log(Level.WARNING, "Could not read " + resource + " to change " + path, ex);
                return false;
            }
        }
        current.set(path, value);
        return write(current);
    }

    public void load() {
        if (!file.exists() && plugin.getResource(resource) != null) {
            plugin.saveResource(resource, false);
        }
        YamlConfiguration loaded = read();
        String bundledText = bundled();
        YamlConfiguration defaults = bundledText == null ? null : parse(bundledText);
        if (defaults == null) {
            configuration = loaded;
            return;
        }
        int missing = countMissing(loaded, defaults);
        int refreshed = refreshRetired(loaded, defaults);
        boolean outdated = loaded.getInt(VERSION_KEY, 0) < defaults.getInt(VERSION_KEY, 0);
        if (missing > 0 || refreshed > 0 || outdated) {
            YamlConfiguration rebuilt = parse(bundledText);
            if (rebuilt != null) {
                overlay(rebuilt, loaded);
                if (defaults.contains(VERSION_KEY)) {
                    rebuilt.set(VERSION_KEY, defaults.getInt(VERSION_KEY));
                }
                keepPrevious();
                write(rebuilt);
                loaded = rebuilt;
                plugin.getLogger().info("Updated " + resource + " with " + missing + " new option(s) and "
                        + refreshed + " refreshed default(s); your own values were kept.");
            }
        }
        loaded.setDefaults(defaults);
        configuration = loaded;
    }

    private int refreshRetired(YamlConfiguration loaded, YamlConfiguration defaults) {
        int refreshed = 0;
        for (Map.Entry<String, Set<String>> entry : retiredDefaults.entrySet()) {
            String key = entry.getKey();
            String current = loaded.getString(key);
            if (current != null && entry.getValue().contains(current) && defaults.contains(key)) {
                loaded.set(key, defaults.get(key));
                refreshed++;
            }
        }
        return refreshed;
    }

    private YamlConfiguration read() {
        YamlConfiguration loaded = new YamlConfiguration();
        if (!file.isFile()) {
            return loaded;
        }
        try {
            loaded.load(file);
            return loaded;
        } catch (IOException | InvalidConfigurationException ex) {
            setAside(ex);
            return new YamlConfiguration();
        }
    }

    private String bundled() {
        InputStream stream = plugin.getResource(resource);
        if (stream == null) {
            return null;
        }
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not read the bundled " + resource, ex);
            return null;
        }
    }

    private YamlConfiguration parse(String content) {
        YamlConfiguration parsed = new YamlConfiguration();
        try {
            parsed.loadFromString(content);
            return parsed;
        } catch (InvalidConfigurationException ex) {
            plugin.getLogger().log(Level.WARNING, "The bundled " + resource + " is not valid YAML", ex);
            return null;
        }
    }

    private void overlay(YamlConfiguration template, ConfigurationSection existing) {
        for (String section : freeSections) {
            if (existing.isConfigurationSection(section)) {
                template.set(section, null);
                template.createSection(section);
            }
        }
        for (String key : existing.getKeys(true)) {
            boolean known = template.contains(key, true);
            if (existing.isConfigurationSection(key)) {
                if (!template.isConfigurationSection(key)) {
                    template.createSection(key);
                }
            } else {
                template.set(key, existing.get(key));
            }
            if (!known) {
                template.setComments(key, existing.getComments(key));
                template.setInlineComments(key, existing.getInlineComments(key));
            }
        }
    }

    private int countMissing(ConfigurationSection target, ConfigurationSection defaults) {
        int missing = 0;
        for (String key : defaults.getKeys(true)) {
            if (!target.contains(key, true) && !underScalar(target, key) && !underFreeSection(target, key)) {
                missing++;
            }
        }
        return missing;
    }

    private boolean underFreeSection(ConfigurationSection target, String key) {
        for (String section : freeSections) {
            if (key.startsWith(section + ".") && target.isConfigurationSection(section)) {
                return true;
            }
        }
        return false;
    }

    private static boolean underScalar(ConfigurationSection target, String key) {
        int dot = key.lastIndexOf('.');
        while (dot > 0) {
            String parent = key.substring(0, dot);
            if (target.contains(parent, true)) {
                return !target.isConfigurationSection(parent);
            }
            dot = parent.lastIndexOf('.');
        }
        return false;
    }

    private void keepPrevious() {
        if (!file.isFile()) {
            return;
        }
        Path previous = file.toPath().resolveSibling(file.getName() + ".old");
        try {
            Files.copy(file.toPath(), previous, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not keep a copy of " + resource, ex);
        }
    }

    private void setAside(Exception cause) {
        Path source = file.toPath();
        Path kept = source.resolveSibling(file.getName() + ".broken-" + LocalDateTime.now().format(STAMP));
        try {
            Files.move(source, kept);
            plugin.getLogger().log(Level.SEVERE, "Could not read " + resource + "; it was kept as "
                    + kept.getFileName() + " and the defaults are used instead.", cause);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not read " + resource
                    + "; the defaults are used for this session.", cause);
        }
    }

    private boolean write(YamlConfiguration target) {
        Path path = file.toPath();
        Path temp = path.resolveSibling(file.getName() + "." + System.nanoTime() + ".tmp");
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(temp, target.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not update " + resource, ex);
            return false;
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
        }
    }
}
