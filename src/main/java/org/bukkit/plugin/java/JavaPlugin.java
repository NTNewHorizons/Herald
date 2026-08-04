package org.bukkit.plugin.java;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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
        globalLogger = new Log4jBackedLogger(description.getName());
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
    public void onEnable() {}

    @Override
    public void onDisable() {}

    @Override
    public void onLoad() {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    public void saveDefaultConfig() {}

    @SuppressWarnings("unchecked")
    public static <T extends JavaPlugin> T getPlugin(Class<T> clazz) {
        if (instance != null && clazz.isInstance(instance)) {
            return (T) instance;
        }
        return null;
    }

    private static final class Log4jBackedLogger extends Logger {

        private final org.apache.logging.log4j.Logger delegate;

        private Log4jBackedLogger(String name) {
            super(name, null);
            setLevel(Level.ALL);
            setUseParentHandlers(false);
            this.delegate = org.apache.logging.log4j.LogManager.getLogger(name);
        }

        @Override
        public void log(LogRecord record) {
            if (record == null || !isLoggable(record.getLevel())) return;

            String message = record.getMessage();
            Object[] parameters = record.getParameters();
            if (parameters != null && parameters.length > 0) {
                try {
                    message = MessageFormat.format(message, parameters);
                } catch (IllegalArgumentException ignored) {}
            }

            Throwable thrown = record.getThrown();
            int level = record.getLevel()
                .intValue();
            if (level >= Level.SEVERE.intValue()) {
                delegate.error(message, thrown);
            } else if (level >= Level.WARNING.intValue()) {
                delegate.warn(message, thrown);
            } else if (level >= Level.INFO.intValue()) {
                delegate.info(message, thrown);
            } else {
                delegate.debug(message, thrown);
            }
        }
    }
}
