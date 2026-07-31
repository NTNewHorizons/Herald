package org.bukkit.craftbukkit;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CraftOfflinePlayer implements OfflinePlayer {

    private final UUID uuid;
    private final String name;

    public CraftOfflinePlayer(UUID uuid) {
        this.uuid = uuid;
        this.name = uuid.toString();
    }

    public CraftOfflinePlayer(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
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
        return false;
    }

    @Override
    public boolean hasPlayedBefore() {
        return true;
    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public boolean isOp() {
        return false;
    }

    @Override
    public void setOp(boolean value) {}
}
