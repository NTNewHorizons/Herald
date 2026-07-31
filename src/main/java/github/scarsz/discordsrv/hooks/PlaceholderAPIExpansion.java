package github.scarsz.discordsrv.hooks;

import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.util.PluginUtil;

public class PlaceholderAPIExpansion implements PluginHook {

    public PlaceholderAPIExpansion() {}

    public void register() {}

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("PlaceholderAPI");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
