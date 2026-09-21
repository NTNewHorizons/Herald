package com.ntnh.herald;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import me.scarsz.jdaappender.ChannelLoggingHandler;
import me.scarsz.jdaappender.adapter.Log4JLoggingAdapter;

/** Forwards Minecraft loggers that do not propagate to Log4j's root logger. */
public final class MinecraftConsoleLogBridge {

    private static Log4JLoggingAdapter adapter;

    private MinecraftConsoleLogBridge() {}

    public static void attach(ChannelLoggingHandler handler) {
        detach();
        adapter = new Log4JLoggingAdapter(handler);
        for (String name : new String[] { "net.minecraft", "com.mojang" }) {
            ((Logger) LogManager.getLogger(name)).addAppender(adapter);
        }
    }

    public static void detach() {
        if (adapter == null) return;
        for (String name : new String[] { "net.minecraft", "com.mojang" }) {
            ((Logger) LogManager.getLogger(name)).removeAppender(adapter);
        }
        adapter = null;
    }
}
