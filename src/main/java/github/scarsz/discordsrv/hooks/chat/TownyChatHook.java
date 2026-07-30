package github.scarsz.discordsrv.hooks.chat;

import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;

public class TownyChatHook implements ChatHook {

    public static String getMainChannelName() {
        return null;
    }

    @Override
    public void broadcastMessageToChannel(String channel, Component message) {
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("TownyChat");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
