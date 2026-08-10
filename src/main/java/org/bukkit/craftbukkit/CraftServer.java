package org.bukkit.craftbukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.IPBanEntry;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.server.management.UserListWhitelist;
import net.minecraft.world.WorldServer;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.WarningState;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.command.CraftCommandSender;
import org.bukkit.craftbukkit.command.CraftConsoleCommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicesManager;

import com.mojang.authlib.GameProfile;

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
        ServerConfigurationManager manager = getConfigurationManager();
        return manager != null ? manager.getMaxPlayers() : 0;
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

    private ServerConfigurationManager getConfigurationManager() {
        return console != null ? console.getConfigurationManager() : null;
    }

    @Override
    public Set<OfflinePlayer> getBannedPlayers() {
        Set<OfflinePlayer> banned = new HashSet<>();
        ServerConfigurationManager manager = getConfigurationManager();
        if (manager == null) return banned;
        UserListBans list = manager.func_152608_h();
        for (String name : list.func_152685_a()) {
            GameProfile profile = list.func_152703_a(name);
            if (profile != null && profile.getId() != null) {
                banned.add(new CraftOfflinePlayer(profile.getName(), profile.getId()));
            }
        }
        return banned;
    }

    @Override
    public Set<OfflinePlayer> getWhitelistedPlayers() {
        Set<OfflinePlayer> whitelisted = new HashSet<>();
        ServerConfigurationManager manager = getConfigurationManager();
        if (manager == null) return whitelisted;
        UserListWhitelist list = manager.func_152599_k();
        for (String name : list.func_152685_a()) {
            GameProfile profile = list.func_152706_a(name);
            if (profile != null && profile.getId() != null) {
                whitelisted.add(new CraftOfflinePlayer(profile.getName(), profile.getId()));
            }
        }
        return whitelisted;
    }

    @Override
    public Set<String> getIPBans() {
        Set<String> bans = new HashSet<>();
        ServerConfigurationManager manager = getConfigurationManager();
        if (manager == null) return bans;
        Collections.addAll(
            bans,
            manager.getBannedIPs()
                .func_152685_a());
        return bans;
    }

    @Override
    public BanList getBanList(BanList.Type type) {
        ServerConfigurationManager manager = getConfigurationManager();
        if (manager == null) {
            return new EmptyBanList();
        }
        return type == BanList.Type.IP ? new IpBanListAdapter(manager.getBannedIPs())
            : new NameBanListAdapter(manager.func_152608_h());
    }

    private static class EmptyBanList implements BanList {

        @Override
        public Set<BanEntry> getBanEntries() {
            return new HashSet<>();
        }

        @Override
        public boolean isBanned(String target) {
            return false;
        }

        @Override
        public void addBan(String target, String reason, Date expires, String source) {}

        @Override
        public void pardon(String target) {}
    }

    private static BanEntry toBukkitBanEntry(String target, net.minecraft.server.management.BanEntry entry) {
        BanEntry bukkitEntry = new BanEntry(target);
        bukkitEntry.setReason(entry.getBanReason());
        bukkitEntry.setExpiration(entry.getBanEndDate());
        return bukkitEntry;
    }

    private class NameBanListAdapter implements BanList {

        private final UserListBans list;

        NameBanListAdapter(UserListBans list) {
            this.list = list;
        }

        @Override
        public Set<BanEntry> getBanEntries() {
            Set<BanEntry> entries = new HashSet<>();
            for (String name : list.func_152685_a()) {
                GameProfile profile = list.func_152703_a(name);
                if (profile == null) continue;
                net.minecraft.server.management.UserListEntry entry = list.func_152683_b(profile);
                if (entry instanceof net.minecraft.server.management.BanEntry) {
                    entries.add(toBukkitBanEntry(profile.getName(), (net.minecraft.server.management.BanEntry) entry));
                }
            }
            return entries;
        }

        @Override
        public boolean isBanned(String target) {
            return list.func_152703_a(target) != null;
        }

        @Override
        public void addBan(String target, String reason, Date expires, String source) {
            GameProfile profile = list.func_152703_a(target);
            if (profile == null && console != null) {
                profile = console.func_152358_ax()
                    .func_152655_a(target);
            }
            if (profile == null) {
                // cannot ban an unknown player, mirrors vanilla /ban behavior
                return;
            }
            list.func_152687_a(new UserListBansEntry(profile, new Date(), source, expires, reason));
        }

        @Override
        public void pardon(String target) {
            GameProfile profile = list.func_152703_a(target);
            if (profile != null) {
                list.func_152684_c(profile);
            }
        }
    }

    private class IpBanListAdapter implements BanList {

        private final net.minecraft.server.management.BanList list;

        IpBanListAdapter(net.minecraft.server.management.BanList list) {
            this.list = list;
        }

        @Override
        public Set<BanEntry> getBanEntries() {
            Set<BanEntry> entries = new HashSet<>();
            for (String ip : list.func_152685_a()) {
                net.minecraft.server.management.UserListEntry entry = list.func_152683_b(ip);
                if (entry instanceof net.minecraft.server.management.BanEntry) {
                    entries.add(toBukkitBanEntry(ip, (net.minecraft.server.management.BanEntry) entry));
                }
            }
            return entries;
        }

        @Override
        public boolean isBanned(String target) {
            return list.func_152683_b(target) != null;
        }

        @Override
        public void addBan(String target, String reason, Date expires, String source) {
            list.func_152687_a(new IPBanEntry(target, new Date(), source, expires, reason));
        }

        @Override
        public void pardon(String target) {
            list.func_152684_c(target);
        }
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
        } else if (sender instanceof CraftCommandSender) {
            icommandsender = ((CraftCommandSender) sender).getHandle();
        }
        console.getCommandManager()
            .executeCommand(icommandsender, command);
        return true;
    }

    @Override
    public List<Entity> selectEntities(CommandSender sender, String selector) {
        List<Entity> entities = new ArrayList<>();
        if (sender == null || selector == null || console == null) return entities;
        ICommandSender commandSender = sender instanceof CraftPlayer ? ((CraftPlayer) sender).getHandle()
            : sender instanceof CraftCommandSender ? ((CraftCommandSender) sender).getHandle() : console;
        try {
            EntityPlayerMP[] players = PlayerSelector.matchPlayers(commandSender, selector);
            if (players != null) {
                for (EntityPlayerMP player : players) {
                    entities.add(getCraftPlayer(player));
                }
            }
        } catch (Exception e) {
            // invalid selector or lacking permission; leave as literal
        }
        return entities;
    }

    @Override
    public SimpleCommandMap getCommandMap() {
        return commandMap;
    }

    @Override
    public boolean getOnlineMode() {
        return console != null && console.isServerInOnlineMode();
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
