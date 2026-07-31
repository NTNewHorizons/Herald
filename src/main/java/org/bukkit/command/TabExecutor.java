package org.bukkit.command;

import java.util.List;

public interface TabExecutor extends CommandExecutor {

    List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args);
}
