package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.command.CommandSender;

public class PaperForwardingCommandSender {

    public static boolean isSenderExists() {
        return false;
    }

    private final CommandSender feedbackSender;

    public PaperForwardingCommandSender(Object event) {
        this.feedbackSender = null;
    }

    public CommandSender getFeedbackSender() {
        return feedbackSender;
    }
}
