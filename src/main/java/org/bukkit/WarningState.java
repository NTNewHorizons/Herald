package org.bukkit;

public enum WarningState {

    ON,
    OFF,
    DEFAULT;

    public static WarningState get() {
        return DEFAULT;
    }

    public boolean printFor(Warning warning) {
        return true;
    }
}
