package org.bukkit.event.player;

import org.bukkit.entity.Player;

public class PlayerKickEvent extends PlayerEvent implements org.bukkit.event.Cancellable {

    private String leaveMessage;
    private String reason;
    private boolean cancelled;

    public PlayerKickEvent(Player player, String reason, String leaveMessage) {
        super(player);
        this.reason = reason;
        this.leaveMessage = leaveMessage;
    }

    public String getLeaveMessage() {
        return leaveMessage;
    }

    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
