package github.scarsz.discordsrv.hooks;

import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.plugin.Plugin;

public class DynmapHook implements PluginHook {

    public void broadcastMessageToDynmap(String name, String message) {
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("dynmap");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
