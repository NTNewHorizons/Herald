package org.bukkit.craftbukkit.entity;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;

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
        return true;
    }

    @Override
    public void setOp(boolean value) {}

    @Override
    public boolean hasPlayedBefore() {
        return true;
    }

    @Override
    public InetSocketAddress getAddress() {
        return null;
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
    public void playSound(Location location, Sound sound, float volume, float pitch) {}

    @Override
    public Server getServer() {
        return null;
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
        return false;
    }

    @Override
    public boolean hasPermission(String name) {
        return true;
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
