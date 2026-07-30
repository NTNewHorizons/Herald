package org.bukkit.plugin.java;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

public abstract class JavaPlugin implements Plugin, CommandExecutor, TabCompleter {
    private static JavaPlugin instance;

    private PluginDescriptionFile description;
    private boolean enabled = false;

    public JavaPlugin() {
        instance = this;
        if (globalDescription != null) {
            this.description = globalDescription;
        }
    }

    private static File globalDataFolder;
    private static PluginDescriptionFile globalDescription;
    private static Server globalServer;
    private static Logger globalLogger;

    public static void setGlobalInit(Server server, PluginDescriptionFile description, File dataFolder) {
        globalServer = server;
        globalDescription = description;
        globalDataFolder = dataFolder;
        globalLogger = Logger.getLogger(description.getName());
    }

    @Override
    public String getName() {
        return description != null ? description.getName() : "Unknown";
    }

    @Override
    public Logger getLogger() {
        return globalLogger;
    }

    @Override
    public File getDataFolder() {
        return globalDataFolder;
    }

    @Override
    public Server getServer() {
        return globalServer;
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return description;
    }

    @Override
    public PluginCommand getCommand(String name) {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onLoad() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    public void saveDefaultConfig() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends JavaPlugin> T getPlugin(Class<T> clazz) {
        if (instance != null && clazz.isInstance(instance)) {
            return (T) instance;
        }
        return null;
    }
}
