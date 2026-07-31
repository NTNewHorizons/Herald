package org.bukkit.plugin.messaging;

public interface PluginMessageListener {

    void onPluginMessageReceived(String channel, Object player, byte[] message);
}
