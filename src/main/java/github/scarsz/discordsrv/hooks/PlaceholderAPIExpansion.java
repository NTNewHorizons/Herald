package github.scarsz.discordsrv.hooks;

import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.plugin.Plugin;

public class PlaceholderAPIExpansion implements PluginHook {

    public PlaceholderAPIExpansion() {
    }

    public void register() {
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("PlaceholderAPI");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
