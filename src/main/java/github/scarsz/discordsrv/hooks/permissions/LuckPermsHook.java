package github.scarsz.discordsrv.hooks.permissions;

import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.util.PluginUtil;

public class LuckPermsHook implements PluginHook {

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("LuckPerms");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
