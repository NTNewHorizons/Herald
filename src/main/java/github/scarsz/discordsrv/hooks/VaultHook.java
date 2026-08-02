package github.scarsz.discordsrv.hooks;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.util.PluginUtil;

public class VaultHook implements PluginHook {

    /**
     * Vault does not exist on Forge. Without a permissions plugin the only group-like distinction available is operator
     * status, so it is surfaced here so {@code %primarygroup%} still resolves to something meaningful.
     */
    public static String getPrimaryGroup(Object player) {
        if (player instanceof Player) {
            return ((Player) player).isOp() ? "op" : "default";
        }
        return "default";
    }

    public static String[] getPlayersGroups(Object player) {
        if (player instanceof Player) {
            return ((Player) player).isOp() ? new String[] { "op" } : new String[] { "default" };
        }
        return new String[] { "default" };
    }

    public static String[] getGroups() {
        return new String[] { "default", "op" };
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
