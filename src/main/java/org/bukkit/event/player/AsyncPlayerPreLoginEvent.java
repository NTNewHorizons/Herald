package org.bukkit.event.player;

import java.net.InetAddress;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AsyncPlayerPreLoginEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String name;
    private final UUID uniqueId;
    private Result result = Result.ALLOWED;
    private String kickMessage = "";
    private final InetAddress address;

    public enum Result {
        ALLOWED,
        KICK_FULL,
        KICK_BANNED,
        KICK_WHITELIST,
        KICK_OTHER;

        public boolean allows() {
            return this == ALLOWED;
        }
    }

    public AsyncPlayerPreLoginEvent(String name, UUID uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
        this.address = null;
    }

    public AsyncPlayerPreLoginEvent(String name, UUID uniqueId, InetAddress address) {
        this.name = name;
        this.uniqueId = uniqueId;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public Result getLoginResult() {
        return result;
    }

    public void setLoginResult(Result result) {
        this.result = result;
    }

    public InetAddress getAddress() {
        return address;
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
