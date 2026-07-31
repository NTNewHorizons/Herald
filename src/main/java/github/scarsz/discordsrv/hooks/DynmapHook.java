package github.scarsz.discordsrv.hooks;

import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.util.PluginUtil;

public class DynmapHook implements PluginHook {

    public void broadcastMessageToDynmap(String name, String message) {}

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("dynmap");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
