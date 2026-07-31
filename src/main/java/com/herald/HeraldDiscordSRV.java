package com.herald;

import java.io.File;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import github.scarsz.discordsrv.DiscordSRV;

public class HeraldDiscordSRV {

    private static final Logger log = LogManager.getLogger("HeraldDiscordSRV");
    private static HeraldDiscordSRV instance;

    private DiscordSRV discordSRV;
    private boolean enabled;
    private CraftServer craftServer;

    public HeraldDiscordSRV() {
        instance = this;
    }

    public static HeraldDiscordSRV getInstance() {
        if (instance == null) {
            instance = new HeraldDiscordSRV();
        }
        return instance;
    }

    public void init(FMLInitializationEvent event) {
        log.info("Initializing Herald DiscordSRV Bridge");

        File dataFolder = new File(
            MinecraftServer.getServer() != null ? MinecraftServer.getServer()
                .getFile("config/herald")
                .getAbsolutePath() : "config/herald");
        dataFolder.mkdirs();

        this.craftServer = new CraftServer();
        PluginDescriptionFile desc = new PluginDescriptionFile("Herald", Tags.VERSION);

        JavaPlugin.setGlobalInit(craftServer, desc, dataFolder);

        try {
            discordSRV = new DiscordSRV();
            discordSRV.setEnabled(true);
        } catch (Exception e) {
            log.error("Failed to create DiscordSRV instance", e);
            return;
        }

        if (discordSRV != null) {
            try {
                MinecraftForge.EVENT_BUS.register(this);
                FMLCommonHandler.instance()
                    .bus()
                    .register(this);
                discordSRV.onEnable();
                this.enabled = discordSRV.isEnabled();
                log.info("Herald DiscordSRV initialized: enabled=" + enabled);
            } catch (Exception e) {
                log.error("Failed to enable DiscordSRV", e);
            }
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        // no-op for now
    }

    public void serverStarting(FMLServerStartingEvent event) {
        if (discordSRV != null) {
            event.registerServerCommand(new CommandDiscord());
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (discordSRV == null || !discordSRV.isEnabled()) return;
        EntityPlayerMP player = event.player;
        if (player == null) return;
        CraftPlayer craftPlayer = craftServer.getCraftPlayer(player);
        if (craftPlayer == null) return;

        AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(craftPlayer, event.message);
        Bukkit.getPluginManager()
            .callEvent(chatEvent);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (discordSRV == null || !discordSRV.isEnabled()) return;
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        CraftPlayer craftPlayer = craftServer.getCraftPlayer(player);
        if (craftPlayer == null) return;

        PlayerJoinEvent joinEvent = new PlayerJoinEvent(craftPlayer, craftPlayer.getName() + " joined the game");
        Bukkit.getPluginManager()
            .callEvent(joinEvent);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (discordSRV == null || !discordSRV.isEnabled()) return;
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        CraftPlayer craftPlayer = craftServer.getCraftPlayer(player);
        if (craftPlayer == null) return;

        PlayerQuitEvent quitEvent = new PlayerQuitEvent(craftPlayer, craftPlayer.getName() + " left the game");
        Bukkit.getPluginManager()
            .callEvent(quitEvent);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (discordSRV == null) return;
        if (craftServer != null) {
            craftServer.addWorld(event.world);
        }
    }

    public void shutdown() {
        if (discordSRV != null && discordSRV.isEnabled()) {
            log.info("Shutting down Herald DiscordSRV");
            discordSRV.onDisable();
        }
        this.enabled = false;
    }

    public DiscordSRV getDiscordSRV() {
        return discordSRV;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
