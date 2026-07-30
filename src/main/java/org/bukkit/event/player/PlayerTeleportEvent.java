package org.bukkit.event.player;

import org.bukkit.entity.Player;

public class PlayerTeleportEvent extends PlayerEvent {
    public PlayerTeleportEvent(Player player) {
        super(player);
    }
}
