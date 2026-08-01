package org.bukkit.craftbukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.WarningState;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.command.CraftConsoleCommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicesManager;

public class CraftServer implements Server {

    private MinecraftServer console;
    private final CraftScheduler scheduler = new CraftScheduler();
    private final CraftPluginManager pluginManager = new CraftPluginManager();
    private final SimpleCommandMap commandMap = new SimpleCommandMap();
    private final CraftServicesManager servicesManager = new CraftServicesManager();
    private final String version;
    private final Map<UUID, CraftPlayer> playerCache = new HashMap<>();
    private final List<World> worlds = new ArrayList<>();

    public CraftServer() {
        this.console = MinecraftServer.getServer();
        Bukkit.setServer(this);
        this.version = "1.7.10-Forge";
    }

    public CraftServer(MinecraftServer console) {
        this.console = console;
        Bukkit.setServer(this);
        this.version = "1.7.10-Forge";
    }

    public CraftPlayer getCraftPlayer(EntityPlayerMP ep) {
        UUID uuid = ep.getUniqueID();
        if (playerCache.containsKey(uuid)) {
            CraftPlayer cached = playerCache.get(uuid);
            if (cached.getHandle() == ep) return cached;
        }
        CraftPlayer cp = new CraftPlayer(ep);
        playerCache.put(uuid, cp);
        return cp;
    }

    public void addWorld(net.minecraft.world.World world) {
        if (world instanceof net.minecraft.world.WorldServer) {
            worlds.add(new CraftWorld((net.minecraft.world.WorldServer) world));
        }
    }

    @Override
    public String getName() {
        return "Herald";
    }

    @Override
    public String getVersion() {
        return MinecraftServer.getServer()
            .getMinecraftVersion() + "-Forge";
    }

    @Override
    public String getBukkitVersion() {
        return version;
    }

    @Override
    public int getMaxPlayers() {
        return console.getMaxPlayers();
    }

    @Override
    public Collection<? extends Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        if (console.worldServers != null) {
            for (WorldServer world : console.worldServers) {
                if (world.playerEntities != null) {
                    for (Object obj : world.playerEntities) {
                        if (obj instanceof EntityPlayerMP) {
                            players.add(getCraftPlayer((EntityPlayerMP) obj));
                        }
                    }
                }
            }
        }
        return players;
    }

    @Override
    public Player getPlayer(UUID uuid) {
        if (console.worldServers != null) {
            for (WorldServer world : console.worldServers) {
                if (world.playerEntities != null) {
                    for (Object obj : world.playerEntities) {
                        if (obj instanceof EntityPlayerMP) {
                            EntityPlayerMP ep = (EntityPlayerMP) obj;
                            if (ep.getUniqueID()
                                .equals(uuid)) {
                                return getCraftPlayer(ep);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public OfflinePlayer[] getOfflinePlayers() {
        List<OfflinePlayer> players = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        for (Player player : getOnlinePlayers()) {
            players.add(player);
            seen.add(player.getUniqueId());
        }

        if (console != null) {
            String[] usernames = console.func_152358_ax()
                .func_152654_a();
            for (String username : usernames) {
                if (username == null) continue;
                com.mojang.authlib.GameProfile profile = console.func_152358_ax()
                    .func_152655_a(username);
                if (profile == null || profile.getId() == null || seen.contains(profile.getId())) continue;
                players.add(new CraftOfflinePlayer(profile.getName(), profile.getId()));
                seen.add(profile.getId());
            }
        }

        return players.toArray(new OfflinePlayer[0]);
    }

    @Override
    public OfflinePlayer getOfflinePlayer(UUID uuid) {
        Player online = getPlayer(uuid);
        if (online != null) return new CraftOfflinePlayer(online.getName(), uuid);

        if (console != null) {
            com.mojang.authlib.GameProfile profile = console.func_152358_ax()
                .func_152652_a(uuid);
            if (profile != null) return new CraftOfflinePlayer(profile.getName(), uuid);
        }

        return new CraftOfflinePlayer(uuid);
    }

    @Override
    public Set<OfflinePlayer> getBannedPlayers() {
        return new HashSet<>();
    }

    @Override
    public Set<OfflinePlayer> getWhitelistedPlayers() {
        return new HashSet<>();
    }

    @Override
    public Set<String> getIPBans() {
        return new HashSet<>();
    }

    @Override
    public BanList getBanList(BanList.Type type) {
        return new BanList() {

            @Override
            public Set<org.bukkit.BanEntry> getBanEntries() {
                return new HashSet<>();
            }

            @Override
            public boolean isBanned(String target) {
                return false;
            }

            @Override
            public void addBan(String target, String reason, java.util.Date expires, String source) {}

            @Override
            public void pardon(String target) {}
        };
    }

    @Override
    public ConsoleCommandSender getConsoleSender() {
        return CraftConsoleCommandSender.getInstance();
    }

    @Override
    public CraftPluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public ServicesManager getServicesManager() {
        return servicesManager;
    }

    @Override
    public CraftScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public List<World> getWorlds() {
        return worlds;
    }

    @Override
    public boolean dispatchCommand(CommandSender sender, String command) {
        ICommandSender icommandsender = console;
        if (sender instanceof org.bukkit.craftbukkit.entity.CraftPlayer) {
            icommandsender = ((org.bukkit.craftbukkit.entity.CraftPlayer) sender).getHandle();
        }
        console.getCommandManager()
            .executeCommand(icommandsender, command);
        return true;
    }

    @Override
    public List<Entity> selectEntities(CommandSender sender, String selector) {
        return new ArrayList<>();
    }

    @Override
    public SimpleCommandMap getCommandMap() {
        return commandMap;
    }

    @Override
    public boolean getOnlineMode() {
        return true;
    }

    @Override
    public WarningState getWarningState() {
        return WarningState.DEFAULT;
    }

    @Override
    public org.bukkit.command.PluginCommand getPluginCommand(String name) {
        return null;
    }

    @Override
    public String getMotd() {
        return console != null ? console.getMOTD() : "A Minecraft Server";
    }
}
