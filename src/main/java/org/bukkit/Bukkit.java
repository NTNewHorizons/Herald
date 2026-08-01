package org.bukkit;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public final class Bukkit {

    private static Server server;

    public static Server getServer() {
        return server;
    }

    public static void setServer(Server server) {
        Bukkit.server = server;
    }

    public static Player getPlayer(UUID uuid) {
        return server != null ? server.getPlayer(uuid) : null;
    }

    public static OfflinePlayer getOfflinePlayer(UUID uuid) {
        return server != null ? server.getOfflinePlayer(uuid) : null;
    }

    public static ConsoleCommandSender getConsoleSender() {
        return server != null ? server.getConsoleSender() : null;
    }

    public static boolean getOnlineMode() {
        return server != null && server.getOnlineMode();
    }

    public static int getMaxPlayers() {
        return server != null ? server.getMaxPlayers() : 0;
    }

    public static List<World> getWorlds() {
        return server != null ? server.getWorlds() : java.util.Collections.emptyList();
    }

    public static String getBukkitVersion() {
        return server != null ? server.getBukkitVersion() : "1.7.10-Forge";
    }

    public static String getVersion() {
        return server != null ? server.getVersion() : "1.7.10";
    }

    public static PluginManager getPluginManager() {
        return server != null ? server.getPluginManager() : null;
    }

    public static BukkitScheduler getScheduler() {
        return server != null ? server.getScheduler() : null;
    }

    public static Set<OfflinePlayer> getBannedPlayers() {
        return server != null ? server.getBannedPlayers() : java.util.Collections.emptySet();
    }

    public static BanList getBanList(BanList.Type type) {
        return server != null ? server.getBanList(type) : null;
    }

    public static SimpleCommandMap getCommandMap() {
        return server != null ? server.getCommandMap() : null;
    }

    public static Player getPlayerExact(String name) {
        for (Player p : getOnlinePlayers()) {
            if (p.getName()
                .equals(name)) return p;
        }
        return null;
    }

    public static Collection<? extends Player> getOnlinePlayers() {
        return server != null ? server.getOnlinePlayers() : java.util.Collections.emptyList();
    }

    public static WarningState getWarningState() {
        return server != null ? server.getWarningState() : WarningState.DEFAULT;
    }

    public static OfflinePlayer getOfflinePlayer(String name) {
        return server != null
            ? server.getOfflinePlayer(
                java.util.UUID
                    .nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            : null;
    }

    public static boolean isPrimaryThread() {
        return true;
    }

    public static OfflinePlayer[] getOfflinePlayers() {
        return server != null ? server.getOfflinePlayers() : new OfflinePlayer[0];
    }

    public static Set<OfflinePlayer> getWhitelistedPlayers() {
        return server != null ? server.getWhitelistedPlayers() : java.util.Collections.emptySet();
    }

    public static Set<String> getIPBans() {
        return server != null ? server.getIPBans() : java.util.Collections.emptySet();
    }

    public static PluginCommand getPluginCommand(String name) {
        return server != null ? server.getPluginCommand(name) : null;
    }

    public static String getMotd() {
        return server != null ? server.getMotd() : "A Minecraft Server";
    }

    public static boolean dispatchCommand(CommandSender sender, String command) {
        return server != null && server.dispatchCommand(sender, command);
    }
}
