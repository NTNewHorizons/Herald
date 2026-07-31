package org.bukkit.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleCommandMap {

    private final Map<String, Command> knownCommands = new ConcurrentHashMap<>();

    public void register(String label, String fallbackPrefix, Command command) {
        knownCommands.put(fallbackPrefix + ":" + label, command);
        knownCommands.put(label, command);
    }

    public boolean register(String label, String fallbackPrefix, Command command, boolean registered) {
        register(label, fallbackPrefix, command);
        return true;
    }

    public boolean dispatch(CommandSender sender, String commandLine) {
        String[] split = commandLine.split(" ", 2);
        String commandName = split[0].startsWith("/") ? split[0].substring(1) : split[0];
        Command cmd = knownCommands.get(commandName);
        if (cmd != null) {
            String[] args = split.length > 1 ? split[1].split(" ") : new String[0];
            return cmd.execute(sender, commandName, args);
        }
        return false;
    }

    public Command getCommand(String name) {
        return knownCommands.get(name);
    }

    public void clearCommands() {
        knownCommands.clear();
    }
}
