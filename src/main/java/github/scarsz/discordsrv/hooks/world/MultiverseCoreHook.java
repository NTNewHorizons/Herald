package github.scarsz.discordsrv.hooks.world;

import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.plugin.Plugin;

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
