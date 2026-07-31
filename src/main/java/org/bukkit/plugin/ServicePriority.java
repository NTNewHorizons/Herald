package org.bukkit.plugin;

public enum ServicePriority {

    Lowest(0),
    Low(1),
    Normal(2),
    High(3),
    Highest(4);

    private final int value;

    ServicePriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
