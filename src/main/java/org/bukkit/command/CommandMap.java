package org.bukkit.command;

public interface CommandMap {

    void register(String label, String fallbackPrefix, Command command);

    boolean dispatch(CommandSender sender, String commandLine);

    Command getCommand(String name);

    void clearCommands();
}
