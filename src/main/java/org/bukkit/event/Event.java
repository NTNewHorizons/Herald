package org.bukkit.event;

public abstract class Event {
    private String name;

    public String getEventName() {
        if (name == null) {
            name = getClass().getSimpleName();
        }
        return name;
    }

    public HandlerList getHandlers() {
        return null;
    }
}
