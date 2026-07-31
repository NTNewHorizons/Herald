package org.bukkit;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;

public interface Server {

    String getName();

    String getVersion();

    String getBukkitVersion();

    int getMaxPlayers();

    Collection<? extends Player> getOnlinePlayers();

    Player getPlayer(UUID uuid);

    OfflinePlayer getOfflinePlayer(UUID uuid);

    Set<OfflinePlayer> getBannedPlayers();

    Set<OfflinePlayer> getWhitelistedPlayers();

    Set<String> getIPBans();

    BanList getBanList(BanList.Type type);

    ConsoleCommandSender getConsoleSender();

    PluginManager getPluginManager();

    ServicesManager getServicesManager();

    BukkitScheduler getScheduler();

    List<World> getWorlds();

    boolean dispatchCommand(CommandSender sender, String command);

    List<Entity> selectEntities(CommandSender sender, String selector);

    SimpleCommandMap getCommandMap();

    boolean getOnlineMode();

    WarningState getWarningState();

    PluginCommand getPluginCommand(String name);

    String getMotd();
}
