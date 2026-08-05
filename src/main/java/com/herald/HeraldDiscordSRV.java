package com.herald;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;

import net.minecraft.command.ICommand;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AchievementEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import github.scarsz.discordsrv.DiscordSRV;

public class HeraldDiscordSRV {

    private static final Logger log = LogManager.getLogger("HeraldDiscordSRV");
    private static HeraldDiscordSRV instance;

    private DiscordSRV discordSRV;
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
            applyForgeConfigOverrides();
            discordSRV.setEnabled(true);
        } catch (Exception e) {
            log.error("Failed to create DiscordSRV instance", e);
            return;
        }

        if (discordSRV != null) {
            try {
                // register a Forge-native permission provider so group-role synchronization and %primarygroup% have a
                // source
                craftServer.getServicesManager()
                    .register(
                        net.milkbowl.vault.permission.Permission.class,
                        new net.milkbowl.vault.permission.Permission(),
                        DiscordSRV.getPlugin(),
                        org.bukkit.plugin.ServicePriority.Normal);
                MinecraftForge.EVENT_BUS.register(this);
                FMLCommonHandler.instance()
                    .bus()
                    .register(this);
                discordSRV.onEnable();
                log.info("Herald DiscordSRV startup scheduled; waiting for Discord connection");
            } catch (Exception e) {
                log.error("Failed to enable DiscordSRV", e);
            }
        }
    }

    private void applyForgeConfigOverrides() {
        if (StringUtils.isNotBlank(Config.discordToken)) {
            DiscordSRV.config()
                .setRuntimeValue("BotToken", Config.discordToken.trim());
            log.info("Using Discord bot token from config/herald.cfg");
        }
        if (StringUtils.isNotBlank(Config.discordChatChannelId)) {
            DiscordSRV.config()
                .setRuntimeValue("Channels", Collections.singletonMap("global", Config.discordChatChannelId.trim()));
            log.info("Using Discord chat channel from config/herald.cfg");
        }
        if (StringUtils.isNotBlank(Config.discordConsoleChannelId)) {
            DiscordSRV.config()
                .setRuntimeValue("DiscordConsoleChannelId", Config.discordConsoleChannelId.trim());
            log.info("Using Discord console channel from config/herald.cfg");
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

    public void serverStarted(FMLServerStartedEvent event) {
        if (discordSRV == null) return;
        // Channel placeholders may query ServerConfigurationManager, which Forge does not create until this event.
        discordSRV.restartChannelUpdaters();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (craftServer == null) return;
        if (event.phase != TickEvent.Phase.END) return;
        craftServer.getScheduler()
            .tick();
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (discordSRV == null || !discordSRV.isEnabled()) return;
        if (event == null || event.entityLiving == null) return;
        if (event.entityLiving.worldObj.isRemote) return;
        if (!(event.entityLiving instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
        CraftPlayer craftPlayer = craftServer != null ? craftServer.getCraftPlayer(player) : null;
        if (craftPlayer == null) return;

        String deathMessage = resolveDeathMessage(event.entityLiving);
        PlayerDeathEvent deathEvent = new PlayerDeathEvent(craftPlayer, deathMessage);
        Bukkit.getPluginManager()
            .callEvent(deathEvent);
    }

    private static String resolveDeathMessage(EntityLivingBase entity) {
        try {
            net.minecraft.util.CombatTracker tracker = entity.func_110142_aN();
            if (tracker == null) return null;
            IChatComponent message = tracker.func_151521_b();
            return message != null ? message.getUnformattedText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (discordSRV == null || !discordSRV.isEnabled()) return;
        if (event == null || event.command == null || event.sender == null) return;

        String command = buildCommandString(event.command, event.parameters);
        if (StringUtils.isBlank(command)) return;

        if (event.sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.sender;
            CraftPlayer craftPlayer = craftServer != null ? craftServer.getCraftPlayer(player) : null;
            if (craftPlayer == null) return;

            PlayerCommandPreprocessEvent commandEvent = new PlayerCommandPreprocessEvent(craftPlayer, command);
            Bukkit.getPluginManager()
                .callEvent(commandEvent);
        } else if (event.sender == MinecraftServer.getServer()) {
            ServerCommandEvent commandEvent = new ServerCommandEvent(Bukkit.getConsoleSender(), command);
            Bukkit.getPluginManager()
                .callEvent(commandEvent);
        }
    }

    private static String buildCommandString(ICommand command, String[] parameters) {
        StringBuilder builder = new StringBuilder("/").append(command.getCommandName());
        if (parameters != null) {
            for (String parameter : parameters) {
                if (parameter != null) builder.append(' ')
                    .append(parameter);
            }
        }
        return builder.toString();
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

        InetAddress address = getPlayerAddress(player);
        AsyncPlayerPreLoginEvent preLoginEvent = new AsyncPlayerPreLoginEvent(
            craftPlayer.getName(),
            craftPlayer.getUniqueId(),
            address);
        Bukkit.getPluginManager()
            .callEvent(preLoginEvent);
        if (!preLoginEvent.getLoginResult()
            .allows()) {
            kickPlayer(player, preLoginEvent.getKickMessage());
            return;
        }

        PlayerLoginEvent loginEvent = new PlayerLoginEvent(craftPlayer, address);
        Bukkit.getPluginManager()
            .callEvent(loginEvent);
        if (loginEvent.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            kickPlayer(player, loginEvent.getKickMessage());
            return;
        }

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

        // Forge 1.7.10 has no kick-specific event; kicks funnel through PlayerLoggedOutEvent too.
        // Firing PlayerKickEvent here lets PlayerBanListener run its ban check on every logout - it is
        // self-filtering (only acts when the player actually ends up on the ban list).
        PlayerKickEvent kickEvent = new PlayerKickEvent(craftPlayer, "", craftPlayer.getName() + " left the game");
        Bukkit.getPluginManager()
            .callEvent(kickEvent);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (discordSRV == null) return;
        if (craftServer != null) {
            craftServer.addWorld(event.world);
            DiscordSRV.updatePlayerDataFolder();
        }
    }

    @SubscribeEvent
    public void onPlayerAchievement(AchievementEvent event) {
        if (discordSRV == null || !discordSRV.isEnabled()) return;
        if (!Config.broadcastAchievements) return;
        if (event == null || event.achievement == null) return;
        if (!(event.entityPlayer instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        CraftPlayer craftPlayer = craftServer != null ? craftServer.getCraftPlayer(player) : null;
        if (craftPlayer == null) return;

        // Forge posts AchievementEvent before StatisticsFile applies the award. Match the checks Minecraft performs
        // afterward: an already-owned achievement is not new, and an achievement whose parent is still locked will
        // not be recorded at all. Announcing the latter caused repeat messages on every qualifying action (for
        // example, Monster Hunter on every mob kill).
        StatisticsFile statistics = player.func_147099_x();
        if (statistics == null || statistics.hasAchievementUnlocked(event.achievement)
            || !statistics.canUnlockAchievement(event.achievement)) return;

        String achievementName = resolveAchievementName(event.achievement);
        if (StringUtils.isBlank(achievementName)) return;

        Bukkit.getPluginManager()
            .callEvent(new PlayerAchievementAwardedEvent(craftPlayer, achievementName));
    }

    private static String resolveAchievementName(Achievement achievement) {
        String statId = achievement.statId;
        if (StringUtils.isBlank(statId)) return null;

        String translated = achievement.func_150951_e()
            .getUnformattedText();
        if (StringUtils.isNotBlank(translated) && !translated.equals(statId)) {
            return translated;
        }

        String localized = StatCollector.translateToLocal(statId);
        if (StringUtils.isNotBlank(localized) && !localized.equals(statId)) {
            return localized;
        }

        String namePart = statId.startsWith("achievement.") ? statId.substring("achievement.".length()) : statId;
        return humanizeAchievementName(namePart);
    }

    private static String humanizeAchievementName(String name) {
        if (StringUtils.isBlank(name)) return null;

        StringBuilder spaced = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                spaced.append(' ');
            } else if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) {
                spaced.append(' ')
                    .append(c);
            } else {
                spaced.append(c);
            }
        }

        StringBuilder result = new StringBuilder();
        for (String word : spaced.toString()
            .split("\\s+")) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) result.append(
                word.substring(1)
                    .toLowerCase());
        }
        return result.toString();
    }

    public void shutdown() {
        if (discordSRV != null && discordSRV.isEnabled()) {
            log.info("Shutting down Herald DiscordSRV");
            discordSRV.onDisable();
        }
        if (craftServer != null) {
            craftServer.getScheduler()
                .shutdown();
        }
    }

    public DiscordSRV getDiscordSRV() {
        return discordSRV;
    }

    public boolean isEnabled() {
        return discordSRV != null && discordSRV.isEnabled() && DiscordSRV.isReady;
    }

    private InetAddress getPlayerAddress(EntityPlayerMP player) {
        if (player == null || player.playerNetServerHandler == null) return null;
        SocketAddress socketAddress = player.playerNetServerHandler.netManager.getSocketAddress();
        if (socketAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) socketAddress).getAddress();
        }
        return null;
    }

    private void kickPlayer(EntityPlayerMP player, String message) {
        if (player == null || player.playerNetServerHandler == null) return;
        player.playerNetServerHandler.kickPlayerFromServer(message == null ? "" : message);
    }
}
