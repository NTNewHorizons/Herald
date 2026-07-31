package github.scarsz.discordsrv.hooks.world;

import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.util.PluginUtil;

@Deprecated
public class MultiverseCoreHook implements PluginHook {

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Multiverse-Core");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
