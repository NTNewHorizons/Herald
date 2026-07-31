package github.scarsz.discordsrv.api.events;

import net.kyori.adventure.text.Component;

import org.bukkit.event.Cancellable;

import github.scarsz.discordsrv.util.MessageUtil;

public class VentureChatMessagePreProcessEvent extends VentureChatMessageEvent implements Cancellable {

    private boolean cancelled;
    private String channel;
    private Component messageComponent;

    public VentureChatMessagePreProcessEvent(String channel, Component message) {
        this.channel = channel;
        this.messageComponent = message;
    }

    @Deprecated
    public VentureChatMessagePreProcessEvent(String channel, String message) {
        this(channel, MessageUtil.toComponent(message, true));
    }

    @Deprecated
    public String getMessage() {
        return MessageUtil.toLegacy(messageComponent);
    }

    @Deprecated
    public void setMessage(String legacy) {
        this.messageComponent = MessageUtil.toComponent(legacy, true);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public String getChannel() {
        return this.channel;
    }

    public Component getMessageComponent() {
        return this.messageComponent;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setMessageComponent(Component messageComponent) {
        this.messageComponent = messageComponent;
    }
}
