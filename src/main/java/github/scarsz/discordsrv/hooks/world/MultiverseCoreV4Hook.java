package github.scarsz.discordsrv.hooks.world;

import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.plugin.Plugin;

public class MultiverseCoreV4Hook implements WorldHook {

    @Override
    public String getWorldAlias(String world) {
        return world;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Multiverse-Core");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
