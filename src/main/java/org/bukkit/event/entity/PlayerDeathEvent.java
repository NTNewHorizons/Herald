package org.bukkit.event.entity;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class PlayerDeathEvent extends EntityEvent implements Cancellable {

    private String deathMessage;
    private boolean cancelled;

    public PlayerDeathEvent(Player player, String deathMessage) {
        super(player);
        this.deathMessage = deathMessage;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public void setDeathMessage(String deathMessage) {
        this.deathMessage = deathMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Player getEntity() {
        return (Player) entity;
    }
}
