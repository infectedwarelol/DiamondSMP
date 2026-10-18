package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.ChatService;
import dev.diamondsmp.core.storage.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.List;

public final class RelationCommand extends BaseCommand {

    private final boolean block;

    public RelationCommand(DiamondSMP plugin, String name, boolean block) {
        super(plugin, name, "diamondsmp." + name, true);
        this.block = block;
    }

    @Override
    protected void execute(CommandContext context) {
        Player player = context.player();
        Player target = context.online(0);
        String messages = block ? "block." : "ignore.";
        if (target.equals(player)) {
            throw context.fail(messages + "self");
        }
        if (target.hasPermission(ChatService.IGNORE_BYPASS)) {
            throw context.fail(messages + "exempt", "player", target.getName());
        }
        PlayerProfile profile = plugin.profiles().profile(player);
        if (profile == null) {
            throw context.fail("general.data-loading");
        }
        boolean added = block ? profile.toggleBlock(target.getUniqueId()) : profile.toggleIgnore(target.getUniqueId());
        if (added && block) {
            plugin.chat().dropConversation(player, target);
        }
        context.reply(messages + (added ? "added" : "removed"), "player", target.getName());
    }

    @Override
    protected List<String> complete(CommandContext context) {
        return players(context, 0);
    }
}
