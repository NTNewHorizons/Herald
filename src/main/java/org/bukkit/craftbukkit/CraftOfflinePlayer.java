package org.bukkit.craftbukkit;

import java.io.File;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CraftOfflinePlayer implements OfflinePlayer {

    private final UUID uuid;
    private final String name;

    public CraftOfflinePlayer(UUID uuid) {
        this.uuid = uuid;
        this.name = resolveName(uuid);
    }

    public CraftOfflinePlayer(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    private static String resolveName(UUID uuid) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null) {
                com.mojang.authlib.GameProfile profile = server.func_152358_ax()
                    .func_152652_a(uuid);
                if (profile != null && profile.getName() != null) return profile.getName();
            }
        } catch (Exception ignored) {}
        return uuid.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public boolean isOnline() {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public boolean hasPlayedBefore() {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server == null || server.worldServers == null || server.worldServers.length == 0) return false;

            // Forge dispatches the player-login event before this shim is guaranteed to receive WorldEvent.Load.
            // Using Bukkit.getWorlds() here therefore made every join look like a first join. Player data is
            // always stored with the primary world, so consult Forge's live world array directly.
            WorldServer primaryWorld = server.worldServers[0];
            if (primaryWorld == null) return false;
            File worldFolder = primaryWorld.getChunkSaveLocation();
            if (worldFolder == null) return false;
            File playerData = new File(worldFolder, "playerdata");
            return new File(playerData, uuid + ".dat").exists();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    public boolean isOp() {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) return player.isOp();
        try {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null && server.getConfigurationManager() != null) {
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(uuid, name);
                return server.getConfigurationManager()
                    .func_152596_g(profile);
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public void setOp(boolean value) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) player.setOp(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftOfflinePlayer)) return false;
        CraftOfflinePlayer that = (CraftOfflinePlayer) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
