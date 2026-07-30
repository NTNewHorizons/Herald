package github.scarsz.discordsrv.api.events;

import org.bukkit.event.Cancellable;

public class VentureChatMessagePostProcessEvent extends VentureChatMessageEvent implements Cancellable {

    private boolean cancelled;
    private String channel;
    private String processedMessage;

    public VentureChatMessagePostProcessEvent(String channel, String processedMessage, boolean cancelled) {
        this.channel = channel;
        this.processedMessage = processedMessage;
        setCancelled(cancelled);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public String getChannel() {
        return this.channel;
    }

    public String getProcessedMessage() {
        return this.processedMessage;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setProcessedMessage(String processedMessage) {
        this.processedMessage = processedMessage;
    }
}
