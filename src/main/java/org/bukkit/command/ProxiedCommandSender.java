package org.bukkit.command;

public interface ProxiedCommandSender extends CommandSender {
    CommandSender getCallee();
    CommandSender getCaller();
}
