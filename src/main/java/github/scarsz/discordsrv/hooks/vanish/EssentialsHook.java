package github.scarsz.discordsrv.hooks.vanish;

import github.scarsz.discordsrv.util.PluginUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Deprecated
public class EssentialsHook implements VanishHook {

    @Override
    public boolean isVanished(Player player) {
        return false;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Essentials");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
