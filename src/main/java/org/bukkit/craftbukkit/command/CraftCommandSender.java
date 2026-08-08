package org.bukkit.craftbukkit.command;

import java.util.Collections;
import java.util.Set;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

public final class CraftCommandSender implements CommandSender {

    private final ICommandSender handle;

    public CraftCommandSender(ICommandSender handle) {
        if (handle == null) throw new IllegalArgumentException("handle cannot be null");
        this.handle = handle;
    }

    public ICommandSender getHandle() {
        return handle;
    }

    @Override
    public String getName() {
        return handle.getCommandSenderName();
    }

    @Override
    public Server getServer() {
        return org.bukkit.Bukkit.getServer();
    }

    @Override
    public void sendMessage(String message) {
        handle.addChatMessage(new ChatComponentText(message == null ? "" : message));
    }

    @Override
    public void sendMessage(String[] messages) {
        if (messages == null) return;
        for (String message : messages) {
            sendMessage(message);
        }
    }

    @Override
    public boolean isPermissionSet(String name) {
        return true;
    }

    @Override
    public boolean hasPermission(String name) {
        return handle.canCommandSenderUseCommand(0, name == null ? "" : name);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        return null;
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return Collections.emptySet();
    }
}
