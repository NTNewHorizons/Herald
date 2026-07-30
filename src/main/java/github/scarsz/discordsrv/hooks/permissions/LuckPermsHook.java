package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.plugin.Plugin;

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
