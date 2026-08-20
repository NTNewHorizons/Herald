package org.bukkit.craftbukkit.entity;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftOfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import com.mojang.authlib.GameProfile;
import com.ntnh.herald.permissions.VanillaPermissionBackend;

/** Lightweight Bukkit player view used before Minecraft creates or admits an EntityPlayerMP. */
public final class CraftLoginPlayer implements Player {

    private final GameProfile profile;
    private final InetSocketAddress address;
    private final CraftOfflinePlayer offlinePlayer;
    private final List<Runnable> admissionAcceptedCallbacks = new ArrayList<>();

    public CraftLoginPlayer(GameProfile profile, InetSocketAddress address) {
        this.profile = profile;
        this.address = address;
        this.offlinePlayer = new CraftOfflinePlayer(profile.getName(), profile.getId());
    }

    /** Defers post-login work until every pre-admission authentication check has allowed this connection. */
    public void runWhenAdmissionAccepted(Runnable callback) {
        if (callback != null) admissionAcceptedCallbacks.add(callback);
    }

    public void markAdmissionAccepted() {
        for (Runnable callback : admissionAcceptedCallbacks) {
            try {
                callback.run();
            } catch (RuntimeException exception) {
                com.ntnh.herald.Herald.LOG.error("Could not run deferred post-login work", exception);
            }
        }
        admissionAcceptedCallbacks.clear();
    }

    @Override
    public String getName() {
        return profile.getName();
    }

    @Override
    public UUID getUniqueId() {
        return profile.getId();
    }

    @Override
    public String getDisplayName() {
        return getName();
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
        MinecraftServer server = MinecraftServer.getServer();
        return server != null && server.getConfigurationManager() != null
            && server.getConfigurationManager()
                .func_152596_g(profile);
    }

    @Override
    public void setOp(boolean value) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) return;
        if (value) {
            server.getConfigurationManager()
                .func_152605_a(profile);
        } else {
            server.getConfigurationManager()
                .func_152610_b(profile);
        }
    }

    @Override
    public boolean hasPlayedBefore() {
        return offlinePlayer.hasPlayedBefore();
    }

    @Override
    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public void kickPlayer(String message) {}

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public void sendMessage(String message) {}

    @Override
    public void sendMessage(String[] messages) {}

    @Override
    public void playSound(Location location, Sound sound, float volume, float pitch) {}

    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public World getWorld() {
        return null;
    }

    @Override
    public EntityType getType() {
        return EntityType.PLAYER;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public boolean isPermissionSet(String name) {
        return name != null && name.startsWith("discordsrv.");
    }

    @Override
    public boolean hasPermission(String name) {
        if (name == null) return false;
        if (Bukkit.getServer() != null) {
            net.milkbowl.vault.permission.Permission permissions = Bukkit.getServer()
                .getServicesManager()
                .load(net.milkbowl.vault.permission.Permission.class);
            if (permissions != null) return permissions.playerHas(null, this, name);
        }
        return new VanillaPermissionBackend().playerHas(this, name);
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
