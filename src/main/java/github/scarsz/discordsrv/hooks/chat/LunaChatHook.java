package github.scarsz.discordsrv.hooks.chat;

import net.kyori.adventure.text.Component;

import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.util.PluginUtil;

public class LunaChatHook implements ChatHook {

    @Override
    public void broadcastMessageToChannel(String channel, Component message) {}

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("LunaChat");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
