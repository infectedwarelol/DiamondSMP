package dev.diamondsmp.core;

import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.admin.DiamondSMPCommand;
import dev.diamondsmp.core.command.chat.BroadcastCommand;
import dev.diamondsmp.core.command.chat.LinkCommand;
import dev.diamondsmp.core.command.chat.ClearChatCommand;
import dev.diamondsmp.core.command.chat.MsgCommand;
import dev.diamondsmp.core.command.chat.MuteChatCommand;
import dev.diamondsmp.core.command.chat.RelationCommand;
import dev.diamondsmp.core.command.chat.ReplyCommand;
import dev.diamondsmp.core.command.chat.SocialSpyCommand;
import dev.diamondsmp.core.command.killeffect.KillEffectCommand;
import dev.diamondsmp.core.command.player.ClearCommand;
import dev.diamondsmp.core.command.player.EnderChestCommand;
import dev.diamondsmp.core.command.player.FeedCommand;
import dev.diamondsmp.core.command.player.FlyCommand;
import dev.diamondsmp.core.command.player.FlySpeedCommand;
import dev.diamondsmp.core.command.player.GamemodeCommand;
import dev.diamondsmp.core.command.player.GodCommand;
import dev.diamondsmp.core.command.player.HealCommand;
import dev.diamondsmp.core.command.player.ListCommand;
import dev.diamondsmp.core.command.player.NearCommand;
import dev.diamondsmp.core.command.player.NightVisionCommand;
import dev.diamondsmp.core.command.player.PingCommand;
import dev.diamondsmp.core.command.player.PlaytimeCommand;
import dev.diamondsmp.core.command.player.RepairCommand;
import dev.diamondsmp.core.command.player.SeenCommand;
import dev.diamondsmp.core.command.player.SpeedCommand;
import dev.diamondsmp.core.command.player.TrashCommand;
import dev.diamondsmp.core.command.player.VanishCommand;
import dev.diamondsmp.core.command.screenshare.FreezeCommand;
import dev.diamondsmp.core.command.screenshare.FreezePointCommand;
import dev.diamondsmp.core.command.screenshare.ScreenShareCommand;
import dev.diamondsmp.core.command.screenshare.UnfreezeCommand;
import dev.diamondsmp.core.command.spawn.DelSpawnCommand;
import dev.diamondsmp.core.command.team.TeamCommand;
import dev.diamondsmp.core.command.spawn.SetSpawnCommand;
import dev.diamondsmp.core.command.spawn.SpawnCommand;
import dev.diamondsmp.core.command.stats.KillStreakCommand;
import dev.diamondsmp.core.command.stats.KillTokenCommand;
import dev.diamondsmp.core.command.stats.LeaderboardCommand;
import dev.diamondsmp.core.command.stats.StatsCommand;
import dev.diamondsmp.core.command.teleport.BackCommand;
import dev.diamondsmp.core.command.teleport.TeleportCommand;
import dev.diamondsmp.core.command.teleport.TopCommand;
import dev.diamondsmp.core.command.teleport.TpAllCommand;
import dev.diamondsmp.core.command.teleport.TpHereCommand;
import dev.diamondsmp.core.command.warp.DelWarpCommand;
import dev.diamondsmp.core.command.warp.SetWarpCommand;
import dev.diamondsmp.core.command.warp.WarpCommand;
import dev.diamondsmp.core.command.warp.WarpsCommand;
import dev.diamondsmp.core.command.world.TimeCommand;
import dev.diamondsmp.core.command.world.WeatherCommand;
import dev.diamondsmp.core.config.ConfigFile;
import dev.diamondsmp.core.gui.Menu;
import dev.diamondsmp.core.hook.PlaceholderHook;
import dev.diamondsmp.core.gui.MenuListener;
import dev.diamondsmp.core.gui.PlayerHeads;
import dev.diamondsmp.core.listener.ChatListener;
import dev.diamondsmp.core.listener.ClearerListener;
import dev.diamondsmp.core.listener.ConnectionListener;
import dev.diamondsmp.core.listener.DamageListener;
import dev.diamondsmp.core.listener.EnvironmentListener;
import dev.diamondsmp.core.listener.DeathListener;
import dev.diamondsmp.core.listener.FreezeListener;
import dev.diamondsmp.core.listener.MovementListener;
import dev.diamondsmp.core.listener.ProfileListener;
import dev.diamondsmp.core.listener.SpawnListener;
import dev.diamondsmp.core.listener.TeamListener;
import dev.diamondsmp.core.listener.TotemListener;
import dev.diamondsmp.core.message.Messages;
import dev.diamondsmp.core.scheduler.Scheduler;
import dev.diamondsmp.core.service.Announcer;
import dev.diamondsmp.core.service.BlockClearer;
import dev.diamondsmp.core.service.ChatFilter;
import dev.diamondsmp.core.service.ChatService;
import dev.diamondsmp.core.service.EnvironmentService;
import dev.diamondsmp.core.service.FfaRules;
import dev.diamondsmp.core.service.FreezePointService;
import dev.diamondsmp.core.service.GodService;
import dev.diamondsmp.core.service.KillEffectService;
import dev.diamondsmp.core.service.KillStreakService;
import dev.diamondsmp.core.service.KillTokenService;
import dev.diamondsmp.core.service.ProfileService;
import dev.diamondsmp.core.service.ReactiveDistanceService;
import dev.diamondsmp.core.service.ScreenShareService;
import dev.diamondsmp.core.service.Service;
import dev.diamondsmp.core.service.SpawnService;
import dev.diamondsmp.core.service.StatsService;
import dev.diamondsmp.core.service.TeamService;
import dev.diamondsmp.core.service.TeleportService;
import dev.diamondsmp.core.service.TotemLimit;
import dev.diamondsmp.core.service.VisibilityService;
import dev.diamondsmp.core.service.WarpService;
import dev.diamondsmp.core.storage.Database;
import dev.diamondsmp.core.storage.SqliteStorage;
import dev.diamondsmp.core.storage.StorageException;
import dev.diamondsmp.core.util.ServerVersion;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class DiamondSMP extends JavaPlugin {

    private final List<Service> services = new ArrayList<>();
    private final List<BaseCommand> commands = new ArrayList<>();

    private ServerVersion serverVersion;
    private String version = "";
    private Scheduler scheduler;
    private ConfigFile configFile;
    private Messages messages;
    private Database database;

    private ProfileService profiles;
    private TeleportService teleports;
    private SpawnService spawn;
    private WarpService warps;
    private ReactiveDistanceService reactiveDistance;
    private VisibilityService visibility;
    private KillEffectService killEffects;
    private StatsService stats;
    private KillStreakService killStreaks;
    private KillTokenService killTokens;
    private PlaceholderHook placeholders;
    private BlockClearer clearer;
    private ChatService chat;
    private ChatFilter chatFilter;
    private GodService god;
    private FfaRules ffaRules;
    private FreezePointService freezePoints;
    private ScreenShareService screenShare;
    private PlayerHeads heads;
    private TotemLimit totems;
    private TeamService teams;
    private EnvironmentService environment;
    private boolean ready;

    @Override
    public void onEnable() {
        serverVersion = ServerVersion.detect();
        version = readVersion();
        scheduler = new Scheduler(this, serverVersion.folia());
        configFile = new ConfigFile(this, "config.yml", Set.of("reactive-distance.thresholds"), Map.of(
                "teleport.warmup.spawn", Set.of("3s"),
                "teleport.warmup.warp", Set.of("3s"),
                "teleport.warmup.back", Set.of("3s")));
        configFile.load();
        messages = new Messages(this);
        messages.reload();
        messages.tokens(links());

        File databaseFile = new File(getDataFolder(), settings().getString("storage.file", "data.db"));
        database = new Database(getLogger(), new SqliteStorage(databaseFile));
        try {
            database.open();
        } catch (StorageException ex) {
            getLogger().log(Level.SEVERE, "The database could not be opened; DiamondSMP is disabled.", ex);
            database.close();
            database = null;
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        createServices();
        CompletableFuture.allOf(spawn.load(), warps.load(), clearer.load(), freezePoints.load(), teams.load())
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        getLogger().log(Level.SEVERE, "Saved data could not be loaded; DiamondSMP is disabled to protect it.",
                                error);
                        scheduler.global(() -> getServer().getPluginManager().disablePlugin(this));
                    }
                });
        for (Service service : services) {
            service.reload();
        }

        registerListeners();
        registerCommands();
        for (Player online : getServer().getOnlinePlayers()) {
            profiles.loadLater(online);
            visibility.joined(online);
            reactiveDistance.apply(online);
        }
        if (PlaceholderHook.available()) {
            placeholders = new PlaceholderHook(this);
            placeholders.register();
        }
        ready = true;
        getLogger().info("Enabled on " + serverVersion.describe() + ".");
    }

    private void createServices() {
        profiles = register(new ProfileService(this));
        teleports = register(new TeleportService(this));
        spawn = register(new SpawnService(this));
        warps = register(new WarpService(this));
        reactiveDistance = register(new ReactiveDistanceService(this));
        visibility = register(new VisibilityService(this));
        killEffects = register(new KillEffectService(this));
        stats = register(new StatsService(this));
        killStreaks = register(new KillStreakService(this));
        killTokens = register(new KillTokenService(this));
        clearer = register(new BlockClearer(this));
        chat = register(new ChatService(this));
        chatFilter = register(new ChatFilter(this));
        god = register(new GodService());
        ffaRules = register(new FfaRules(this));
        freezePoints = register(new FreezePointService(this));
        screenShare = register(new ScreenShareService(this));
        heads = register(new PlayerHeads(this));
        totems = register(new TotemLimit(this));
        teams = register(new TeamService(this));
        environment = register(new EnvironmentService(this));
        register(new Announcer(this));
    }

    private <T extends Service> T register(T service) {
        services.add(service);
        return service;
    }

    private void registerListeners() {
        Listener[] listeners = {
                new ProfileListener(this),
                new ConnectionListener(this),
                new SpawnListener(this),
                new DamageListener(this),
                new DeathListener(this),
                new ClearerListener(this),
                new ChatListener(this),
                new MovementListener(this),
                new FreezeListener(this),
                new TotemListener(this),
                new TeamListener(this),
                new EnvironmentListener(this),
                new MenuListener()
        };
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void registerCommands() {
        commands.add(new GamemodeCommand(this, "gamemode", null));
        commands.add(new GamemodeCommand(this, "gmc", GameMode.CREATIVE));
        commands.add(new GamemodeCommand(this, "gms", GameMode.SURVIVAL));
        commands.add(new GamemodeCommand(this, "gma", GameMode.ADVENTURE));
        commands.add(new GamemodeCommand(this, "gmsp", GameMode.SPECTATOR));
        commands.add(new HealCommand(this));
        commands.add(new FeedCommand(this));
        commands.add(new FlyCommand(this));
        commands.add(new GodCommand(this));
        commands.add(new NightVisionCommand(this));
        commands.add(new SpeedCommand(this));
        commands.add(new FlySpeedCommand(this));
        commands.add(new ClearCommand(this));
        commands.add(new RepairCommand(this));
        commands.add(new VanishCommand(this));
        commands.add(new TrashCommand(this));
        commands.add(new EnderChestCommand(this));
        commands.add(new PingCommand(this));
        commands.add(new NearCommand(this));
        commands.add(new SeenCommand(this));
        commands.add(new PlaytimeCommand(this));
        commands.add(new ListCommand(this));
        commands.add(new TeleportCommand(this));
        commands.add(new TpHereCommand(this));
        commands.add(new TpAllCommand(this));
        commands.add(new BackCommand(this));
        commands.add(new TopCommand(this));
        commands.add(new SetSpawnCommand(this));
        commands.add(new DelSpawnCommand(this));
        commands.add(new SpawnCommand(this));
        commands.add(new SetWarpCommand(this));
        commands.add(new WarpCommand(this));
        commands.add(new DelWarpCommand(this));
        commands.add(new WarpsCommand(this));
        commands.add(new KillEffectCommand(this));
        commands.add(new StatsCommand(this));
        commands.add(new LeaderboardCommand(this));
        commands.add(new KillStreakCommand(this));
        commands.add(new KillTokenCommand(this));
        commands.add(new TeamCommand(this));
        commands.add(new MsgCommand(this));
        commands.add(new ReplyCommand(this));
        commands.add(new RelationCommand(this, "ignore", false));
        commands.add(new RelationCommand(this, "block", true));
        commands.add(new SocialSpyCommand(this));
        commands.add(new BroadcastCommand(this));
        commands.add(new ClearChatCommand(this));
        commands.add(new MuteChatCommand(this));
        commands.add(new LinkCommand(this, "store"));
        commands.add(new LinkCommand(this, "discord"));
        commands.add(new FreezeCommand(this));
        commands.add(new UnfreezeCommand(this));
        commands.add(new FreezePointCommand(this, "setfreezepoint", "diamondsmp.screenshare.freezepoint.set",
                FreezePointCommand.Mode.SET));
        commands.add(new FreezePointCommand(this, "delfreezepoint", "diamondsmp.screenshare.freezepoint.delete",
                FreezePointCommand.Mode.DELETE));
        commands.add(new FreezePointCommand(this, "freezepoints", "diamondsmp.screenshare.freezepoint.list",
                FreezePointCommand.Mode.LIST));
        commands.add(new ScreenShareCommand(this));
        commands.add(new TimeCommand(this, "day", "day"));
        commands.add(new TimeCommand(this, "night", "night"));
        commands.add(new TimeCommand(this, "time", null));
        commands.add(new WeatherCommand(this));
        commands.add(new DiamondSMPCommand(this));
        for (BaseCommand command : commands) {
            command.register();
        }
    }

    public void reloadAll() {
        configFile.load();
        messages.reload();
        messages.tokens(links());
        for (Service service : services) {
            service.reload();
        }
        for (BaseCommand command : commands) {
            command.refresh();
        }
    }

    @Override
    public void onDisable() {
        if (placeholders != null) {
            placeholders.unregister();
            placeholders = null;
        }
        if (ready) {
            closeMenus();
            shutdownServices();
        }
        ready = false;
        services.clear();
        for (BaseCommand command : commands) {
            command.unregister();
        }
        commands.clear();
        HandlerList.unregisterAll(this);
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (database != null) {
            database.close();
            database = null;
        }
    }

    private void closeMenus() {
        for (Player online : getServer().getOnlinePlayers()) {
            scheduler.entity(online, () -> {
                if (online.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                    online.closeInventory();
                }
            });
        }
    }

    private void shutdownServices() {
        for (int index = services.size() - 1; index >= 0; index--) {
            Service service = services.get(index);
            try {
                service.shutdown();
            } catch (RuntimeException ex) {
                getLogger().log(Level.SEVERE, "A service did not shut down cleanly", ex);
            }
        }
    }

    public YamlConfiguration settings() {
        return configFile.get();
    }

    private Map<String, String> links() {
        Map<String, String> links = new HashMap<>();
        ConfigurationSection section = settings().getConfigurationSection("links");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                links.put(key, section.getString(key, ""));
            }
        }
        return links;
    }

    private String readVersion() {
        try (InputStream stream = getResource("plugin.yml")) {
            if (stream == null) {
                return "";
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getString("version", "");
        } catch (IOException ex) {
            return "";
        }
    }

    public String version() {
        return version;
    }

    public ServerVersion serverVersion() {
        return serverVersion;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public Messages messages() {
        return messages;
    }

    public Database database() {
        return database;
    }

    public ProfileService profiles() {
        return profiles;
    }

    public TeleportService teleports() {
        return teleports;
    }

    public SpawnService spawn() {
        return spawn;
    }

    public WarpService warps() {
        return warps;
    }

    public ReactiveDistanceService reactiveDistance() {
        return reactiveDistance;
    }

    public VisibilityService visibility() {
        return visibility;
    }

    public KillEffectService killEffects() {
        return killEffects;
    }

    public StatsService stats() {
        return stats;
    }

    public KillStreakService killStreaks() {
        return killStreaks;
    }

    public KillTokenService killTokens() {
        return killTokens;
    }

    public BlockClearer clearer() {
        return clearer;
    }

    public ChatService chat() {
        return chat;
    }

    public ChatFilter chatFilter() {
        return chatFilter;
    }

    public GodService god() {
        return god;
    }

    public FfaRules ffaRules() {
        return ffaRules;
    }

    public FreezePointService freezePoints() {
        return freezePoints;
    }

    public ScreenShareService screenShare() {
        return screenShare;
    }

    public PlayerHeads heads() {
        return heads;
    }

    public TotemLimit totems() {
        return totems;
    }

    public TeamService teams() {
        return teams;
    }

    public EnvironmentService environment() {
        return environment;
    }
}
