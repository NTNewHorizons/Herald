package org.bukkit.event.player;

import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

public class PlayerAdvancementDoneEvent extends PlayerEvent {

    private final Advancement advancement;

    public PlayerAdvancementDoneEvent(Player who, Advancement advancement) {
        super(who);
        this.advancement = advancement;
    }

    public Advancement getAdvancement() {
        return advancement;
    }
}
