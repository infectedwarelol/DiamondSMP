package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.message.Text;
import dev.diamondsmp.core.util.SoundSpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class BroadcastCommand extends BaseCommand {

    public BroadcastCommand(DiamondSMP plugin) {
        super(plugin, "broadcast", "diamondsmp.broadcast", false);
    }

    @Override
    protected void execute(CommandContext context) {
        String message = context.joined(0);
        if (message.isEmpty()) {
            throw context.usage();
        }
        Component text = Text.parse(plugin.messages().theme(message));
        String sender = context.isPlayer() ? context.player().getName()
                : plugin.settings().getString("broadcast.console-name", "Console");
        Component head = context.isPlayer() ? Text.head(sender) : Component.empty();
        plugin.messages().broadcast("broadcast.format", "message", text, "player", sender, "head", head);
        FileConfiguration config = plugin.settings();
        Title title = config.getBoolean("broadcast.title.enabled", true)
                ? plugin.messages().title("broadcast.title", Title.Times.times(
                        ticks(config.getInt("broadcast.title.fade-in-ticks", 10)),
                        ticks(config.getInt("broadcast.title.stay-ticks", 100)),
                        ticks(config.getInt("broadcast.title.fade-out-ticks", 20))),
                "message", text, "player", sender, "head", head)
                : null;
        SoundSpec sound = SoundSpec.from(config.getConfigurationSection("broadcast.sound"));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (title != null) {
                online.showTitle(title);
            }
            sound.play(online);
        }
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }
}
