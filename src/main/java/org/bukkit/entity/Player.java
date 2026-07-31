package org.bukkit.entity;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

public interface Player extends Entity, OfflinePlayer, CommandSender, Permissible {

    String getName();

    UUID getUniqueId();

    String getDisplayName();

    void setDisplayName(String name);

    String getPlayerListName();

    void setPlayerListName(String name);

    boolean isOp();

    void setOp(boolean value);

    boolean hasPlayedBefore();

    InetSocketAddress getAddress();

    void kickPlayer(String message);

    Location getLocation();

    void sendMessage(String message);

    void sendMessage(String[] messages);

    void playSound(Location location, Sound sound, float volume, float pitch);

    boolean isDead();

    boolean isOnline();
}
