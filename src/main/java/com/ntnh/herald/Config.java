package com.ntnh.herald;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String discordToken = "";
    public static String discordChatChannelId = "";
    public static String discordConsoleChannelId = "";
    public static String botNickname = "Herald";
    public static String botActivity = "Minecraft 1.7.10";
    public static boolean broadcastChat = true;
    public static boolean broadcastJoinLeave = true;
    public static boolean broadcastDeath = true;
    public static boolean broadcastAchievements = true;
    public static List<String> botAdmins = Arrays.asList("");

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        discordToken = configuration
            .getString("discordToken", Configuration.CATEGORY_GENERAL, discordToken, "Discord bot token");
        discordChatChannelId = configuration
            .getString("discordChatChannelId", "discord", discordChatChannelId, "Discord channel ID for chat");
        discordConsoleChannelId = configuration
            .getString("discordConsoleChannelId", "discord", discordConsoleChannelId, "Discord channel ID for console");
        botNickname = configuration.getString("botNickname", "discord", botNickname, "Nickname for the bot");
        botActivity = configuration.getString("botActivity", "discord", botActivity, "Activity status for the bot");
        broadcastChat = configuration
            .getBoolean("broadcastChat", "broadcasting", broadcastChat, "Broadcast chat messages to Discord");
        broadcastJoinLeave = configuration.getBoolean(
            "broadcastJoinLeave",
            "broadcasting",
            broadcastJoinLeave,
            "Broadcast join/leave messages to Discord");
        broadcastDeath = configuration
            .getBoolean("broadcastDeath", "broadcasting", broadcastDeath, "Broadcast death messages to Discord");
        broadcastAchievements = configuration.getBoolean(
            "broadcastAchievements",
            "broadcasting",
            broadcastAchievements,
            "Broadcast achievement messages to Discord");
        botAdmins = Arrays.asList(
            configuration
                .getStringList("botAdmins", "discord", new String[0], "Discord user IDs with bot admin privileges"));

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
