package github.scarsz.discordsrv.hooks.world;

import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.util.PluginUtil;

public class MultiverseCoreV5Hook implements WorldHook {

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
