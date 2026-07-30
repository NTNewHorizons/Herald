package org.bukkit;

import java.util.UUID;
import org.bukkit.entity.Player;

public interface OfflinePlayer {
    String getName();
    UUID getUniqueId();
    boolean isOnline();
    boolean hasPlayedBefore();
    Player getPlayer();
    boolean isOp();
    void setOp(boolean value);
}
