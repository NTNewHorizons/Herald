package org.bukkit.event.server;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ServerCommandEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final CommandSender sender;
    private String command;

    public ServerCommandEvent(CommandSender sender, String command) {
        this.sender = sender;
        this.command = command;
    }

    public CommandSender getSender() {
        return sender;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
