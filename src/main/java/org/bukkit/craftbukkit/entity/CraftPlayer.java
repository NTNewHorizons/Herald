package org.bukkit.craftbukkit.entity;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public class CraftPlayer implements Player {

    private final EntityPlayerMP handle;
    private final UUID uuid;

    public CraftPlayer(EntityPlayerMP handle) {
        this.handle = handle;
        this.uuid = handle.getUniqueID();
    }

    public EntityPlayerMP getHandle() {
        return handle;
    }

    @Override
    public String getName() {
        return handle.getCommandSenderName();
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String getDisplayName() {
        return handle.getCommandSenderName();
    }

    @Override
    public void setDisplayName(String name) {}

    @Override
    public String getPlayerListName() {
        return getName();
    }

    @Override
    public void setPlayerListName(String name) {}

    @Override
    public boolean isOp() {
        return handle.mcServer.getConfigurationManager()
            .func_152596_g(handle.getGameProfile());
    }

    @Override
    public void setOp(boolean value) {
        if (value) {
            handle.mcServer.getConfigurationManager()
                .func_152605_a(handle.getGameProfile());
        } else {
            handle.mcServer.getConfigurationManager()
                .func_152610_b(handle.getGameProfile());
        }
    }

    @Override
    public boolean hasPlayedBefore() {
        return Bukkit.getServer() != null && Bukkit.getServer()
            .getOfflinePlayer(uuid)
            .hasPlayedBefore();
    }

    @Override
    public InetSocketAddress getAddress() {
        if (handle.playerNetServerHandler == null) return null;
        java.net.SocketAddress socketAddress = handle.playerNetServerHandler.netManager.getSocketAddress();
        return socketAddress instanceof InetSocketAddress ? (InetSocketAddress) socketAddress : null;
    }

    @Override
    public void kickPlayer(String message) {
        handle.playerNetServerHandler.kickPlayerFromServer(message);
    }

    @Override
    public Location getLocation() {
        return new Location(
            getWorld(),
            handle.posX,
            handle.posY,
            handle.posZ,
            handle.rotationYaw,
            handle.rotationPitch);
    }

    @Override
    public void sendMessage(String message) {
        handle.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String msg : messages) {
            sendMessage(msg);
        }
    }

    @Override
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        if (sound == null || handle.playerNetServerHandler == null) return;
        Location target = location != null ? location : getLocation();
        handle.playerNetServerHandler.sendPacket(
            new S29PacketSoundEffect(
                sound.getLegacyKey(),
                target.getX(),
                target.getY(),
                target.getZ(),
                volume,
                pitch));
    }

    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public World getWorld() {
        return new CraftWorld((WorldServer) handle.worldObj);
    }

    @Override
    public EntityType getType() {
        return EntityType.PLAYER;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public boolean isDead() {
        return handle.isDead;
    }

    @Override
    public Player getPlayer() {
        return this;
    }

    @Override
    public boolean isPermissionSet(String name) {
        return name != null && name.startsWith("discordsrv.");
    }

    /**
     * Mirrors the {@code default} values declared in DiscordSRV's plugin.yml for a Forge server that has no Bukkit
     * permission plugin. In particular, Bukkit's {@code default: false} permissions must remain false for operators;
     * otherwise every operator is treated as having {@code discordsrv.silentjoin} and {@code discordsrv.silentquit}.
     */
    @Override
    public boolean hasPermission(String name) {
        if (name == null) return false;
        if (Bukkit.getServer() != null) {
            net.milkbowl.vault.permission.Permission permissions = Bukkit.getServer()
                .getServicesManager()
                .load(net.milkbowl.vault.permission.Permission.class);
            if (permissions != null) return permissions.playerHas(null, this, name);
        }
        return new com.ntnh.herald.permissions.VanillaPermissionBackend().playerHas(this, name);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return null;
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return Collections.emptySet();
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue metadataValue) {}

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return false;
    }

    @Override
    public void removeMetadata(String metadataKey, Object plugin) {}
}
