package dev.diamondsmp.core.command.chat;

import dev.diamondsmp.core.DiamondSMP;
import dev.diamondsmp.core.command.BaseCommand;
import dev.diamondsmp.core.command.CommandContext;
import dev.diamondsmp.core.service.ChatService;
import dev.diamondsmp.core.storage.PlayerProfile;

public final class SocialSpyCommand extends BaseCommand {

    public SocialSpyCommand(DiamondSMP plugin) {
        super(plugin, "socialspy", ChatService.SOCIALSPY_PERMISSION, true);
    }

    @Override
    protected void execute(CommandContext context) {
        PlayerProfile profile = plugin.profiles().profile(context.player());
        if (profile == null) {
            throw context.fail("general.data-loading");
        }
        boolean enabled = !profile.socialSpy();
        profile.socialSpy(enabled);
        context.reply("socialspy.toggled", "state", state(enabled));
    }
}
