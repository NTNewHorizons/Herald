package org.bukkit.event.player;

import java.net.InetAddress;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerLoginEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();
    private Result result = Result.ALLOWED;
    private String kickMessage = "";

    public enum Result {
        ALLOWED,
        KICK_FULL,
        KICK_BANNED,
        KICK_WHITELIST,
        KICK_OTHER;
    }

    public PlayerLoginEvent(Player player) {
        super(player);
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public InetAddress getAddress() {
        return null;
    }

    public void disallow(Result result, String message) {
        this.result = result;
        this.kickMessage = message;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
