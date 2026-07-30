package org.bukkit;

public class GameRule<T> {
    public static final GameRule<Boolean> ANNOUNCE_ADVANCEMENTS = new GameRule<>("announceAdvancements", Boolean.class);

    private final String name;
    private final Class<T> type;

    public GameRule(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }
}
