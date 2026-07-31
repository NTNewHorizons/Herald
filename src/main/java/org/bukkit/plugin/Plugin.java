package org.bukkit.plugin;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.PluginCommand;

public interface Plugin {

    String getName();

    Logger getLogger();

    File getDataFolder();

    boolean isEnabled();

    PluginDescriptionFile getDescription();

    Server getServer();

    void onEnable();

    void onDisable();

    PluginCommand getCommand(String name);

    void onLoad();
}
