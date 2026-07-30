package github.scarsz.discordsrv.objects.proxy;

import dev.vankka.dynamicproxy.processor.Original;
import dev.vankka.dynamicproxy.processor.Proxy;
import github.scarsz.discordsrv.util.DiscordChatChannelCommandFeedbackForwarder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Proxy(CommandSender.class)
public abstract class CommandSenderDynamic implements CommandSender {

    @Original
    private final CommandSender original;
    private final DiscordChatChannelCommandFeedbackForwarder sendUtil;

    public CommandSenderDynamic(CommandSender original, GuildMessageReceivedEvent event) {
        this.original = original;
        this.sendUtil = new DiscordChatChannelCommandFeedbackForwarder(event);
    }

    private void doSend(String message) {
        sendUtil.send(message);
    }

    @Override
    public void sendMessage(@NotNull String s) {
        original.sendMessage(s);
        doSend(s);
    }

    @Override
    public void sendMessage(@NotNull String[] strings) {
        original.sendMessage(strings);
        for (String string : strings) {
            doSend(string);
        }
    }

    public void sendMessage(@Nullable UUID uuid, @NotNull String s) {
        original.sendMessage(s);
        doSend(s);
    }

    public void sendMessage(@Nullable UUID uuid, @NotNull String[] strings) {
        original.sendMessage(strings);
        for (String string : strings) {
            doSend(string);
        }
    }
}
