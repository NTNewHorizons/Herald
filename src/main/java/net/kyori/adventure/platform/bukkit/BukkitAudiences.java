package net.kyori.adventure.platform.bukkit;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

/**
 * Forge-native replacement for adventure-platform-bukkit.
 *
 * <p>
 * adventure-platform-bukkit is built against the real Spigot/Bukkit API and
 * references a large portion of it (boss bars, wither entities, NMS packets,
 * Spigot/Paper reflection, ...), most of which can never work on a Forge-only
 * server. Herald only needs it to send chat components to players and the
 * console, so this class implements {@link Audience}s that serialize the
 * component to legacy section-sign text and hand it to the existing Forge
 * chat pipeline through Herald's Bukkit shim.
 * </p>
 */
public final class BukkitAudiences {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private BukkitAudiences() {}

    public static BukkitAudiences create(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return new BukkitAudiences();
    }

    public Audience sender(CommandSender sender) {
        return new CommandSenderAudience(sender);
    }

    public Audience player(Player player) {
        return new CommandSenderAudience(player);
    }

    public Audience consoleSender() {
        Server server = Bukkit.getServer();
        ConsoleCommandSender console = server != null ? server.getConsoleSender() : null;
        return new CommandSenderAudience(console != null ? console : new NullCommandSender());
    }

    public void close() {}

    private static final class CommandSenderAudience implements Audience {

        private final CommandSender sender;

        private CommandSenderAudience(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void sendMessage(Identity source, Component message, MessageType type) {
            sender.sendMessage(LEGACY.serialize(message));
        }
    }

    private static final class NullCommandSender implements ConsoleCommandSender {

        @Override
        public String getName() {
            return "CONSOLE";
        }

        @Override
        public Server getServer() {
            return Bukkit.getServer();
        }

        @Override
        public void sendMessage(String message) {}

        @Override
        public void sendMessage(String[] messages) {}

        @Override
        public boolean isPermissionSet(String name) {
            return true;
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
    }
}
