package org.bukkit.command;

import org.bukkit.plugin.Plugin;

public class PluginCommand extends Command {
    private final Plugin plugin;
    private CommandExecutor executor;

    public PluginCommand(String name, Plugin plugin) {
        super(name);
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }

    public void unregister(SimpleCommandMap commandMap) {
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (executor != null) {
            return executor.onCommand(sender, this, commandLabel, args);
        }
        return false;
    }
}
