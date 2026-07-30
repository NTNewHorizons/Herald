package me.clip.placeholderapi;

import me.clip.placeholderapi.expansion.ExpansionManager;

public class PlaceholderAPIPlugin {
    private static PlaceholderAPIPlugin instance;

    public static PlaceholderAPIPlugin getInstance() {
        if (instance == null) instance = new PlaceholderAPIPlugin();
        return instance;
    }

    public static String booleanTrue() { return "true"; }
    public static String booleanFalse() { return "false"; }

    public ExpansionManager getLocalExpansionManager() { return new ExpansionManager(); }
}
