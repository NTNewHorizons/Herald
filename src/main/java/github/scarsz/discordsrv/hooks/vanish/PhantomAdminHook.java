package github.scarsz.discordsrv.hooks.vanish;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.util.PluginUtil;

@Deprecated
public class PhantomAdminHook implements VanishHook {

    @Override
    public boolean isVanished(Player player) {
        return false;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("PhantomAdmin");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
