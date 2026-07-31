package github.scarsz.discordsrv.hooks;

import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.util.PluginUtil;

public class VaultHook implements PluginHook {

    public static String getPrimaryGroup(Object player) {
        return " ";
    }

    public static String[] getPlayersGroups(Object player) {
        return new String[] {};
    }

    public static String[] getGroups() {
        return new String[] {};
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Vault");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
