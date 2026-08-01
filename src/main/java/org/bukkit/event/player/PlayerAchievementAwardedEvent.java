package org.bukkit.event.player;

import org.bukkit.entity.Player;

/**
 * Called when a player earns an achievement. Unlike the upstream Bukkit event this carries the
 * resolved display name of the achievement rather than a hardcoded {@code org.bukkit.Achievement}
 * enum, so modded 1.7.10 achievements are supported without maintaining a static list.
 */
public class PlayerAchievementAwardedEvent extends PlayerEvent {

    private final String achievementName;

    public PlayerAchievementAwardedEvent(Player who, String achievementName) {
        super(who);
        this.achievementName = achievementName;
    }

    public String getAchievementName() {
        return achievementName;
    }
}
