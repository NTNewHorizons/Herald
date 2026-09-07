package com.ntnh.herald;

import net.minecraft.util.IChatComponent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import github.scarsz.discordsrv.DiscordSRV;
import me.scarsz.jdaappender.ChannelLoggingHandler;
import me.scarsz.jdaappender.LogItem;
import me.scarsz.jdaappender.LogLevel;

/** Ensures native server command feedback reaches both the physical and Discord consoles. */
public final class MinecraftServerCommandOutputForwarder {

    public static final String OUTPUT_LOGGER_NAME = "HeraldMinecraftServerCommandOutput";

    private static final Logger OUTPUT_LOGGER = LogManager.getLogger(OUTPUT_LOGGER_NAME);

    private MinecraftServerCommandOutputForwarder() {}

    public static boolean forward(IChatComponent message) {
        DiscordSRV plugin = DiscordSRV.getPlugin();
        ChannelLoggingHandler appender = plugin != null ? plugin.getConsoleAppender() : null;
        if (appender == null) return false;

        String output = message == null ? "" : message.getUnformattedText();

        // Preserve the physical console line. This logger is excluded from the Discord appender below because the
        // equivalent synthetic LogItem is enqueued directly with MinecraftServer's normal logger identity.
        OUTPUT_LOGGER.info(output);
        appender.enqueue(new LogItem(appender, "net.minecraft.server.MinecraftServer", LogLevel.INFO, output));
        return true;
    }
}
