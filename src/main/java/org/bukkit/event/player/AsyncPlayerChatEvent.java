package org.bukkit.event.player;

import java.util.Set;
import org.bukkit.entity.Player;

public class AsyncPlayerChatEvent extends PlayerEvent implements org.bukkit.event.Cancellable {
    private String message;
    private boolean cancelled;

    public AsyncPlayerChatEvent(Player player, String message) {
        super(player);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Set<Player> getRecipients() {
        return java.util.Collections.emptySet();
    }
}
