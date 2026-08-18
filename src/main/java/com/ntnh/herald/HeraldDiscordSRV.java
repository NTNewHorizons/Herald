package com.ntnh.herald;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.concurrent.Future;

import net.dv8tion.jda.api.JDA;
import net.minecraft.command.ICommand;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.rcon.RConConsoleSource;
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
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.command.CraftCommandSender;
import org.bukkit.craftbukkit.entity.CraftLoginPlayer;
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
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.authlib.GameProfile;
import com.ntnh.herald.auth.AuthenticationReadiness;
import com.ntnh.herald.auth.LoginDecision;
import com.ntnh.herald.auth.PreAdmissionLoginHandler;
import com.ntnh.herald.security.IpAuthAuditLogger;
import com.ntnh.herald.security.IpAuthManager;
import com.ntnh.herald.security.IpAuthStore;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;

public class HeraldDiscordSRV {

    private static final Logger log = LogManager.getLogger("HeraldDiscordSRV");
    private static HeraldDiscordSRV instance;

    private DiscordSRV discordSRV;
    private final AuthenticationReadiness authenticationReadiness = new AuthenticationReadiness(log);
    private CraftServer craftServer;
    private final PreAdmissionLoginHandler preAdmissionLoginHandler = new PreAdmissionLoginHandler();
    private IpAuthManager ipAuthManager;

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
            authenticationReadiness.markFailed("DiscordSRV could not be constructed");
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
                initializeIpAuthentication(dataFolder);
                discordSRV.onEnable();
                log.info("Herald DiscordSRV startup scheduled; waiting for Discord connection");
            } catch (Exception e) {
                log.error("Failed to enable DiscordSRV", e);
                authenticationReadiness.markFailed("DiscordSRV or Herald IP authentication could not be enabled");
            }
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        processAlert(event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        if (discordSRV != null) {
            event.registerServerCommand(new CommandDiscord());
        }
        if (ipAuthManager != null) event.registerServerCommand(new CommandHerald(ipAuthManager));
        processAlert(event);
    }

    private void initializeIpAuthentication(File dataFolder) throws IOException {
        IpAuthStore store = new IpAuthStore(new File(dataFolder, "ip-auth-trusted-ips.tsv").toPath());
        IpAuthAuditLogger auditLogger = new IpAuthAuditLogger(
            new File(dataFolder, "logs/ip-auth-audit.log").toPath(),
            HeraldConfig.ipAuthenticationAuditEnabled);
        try {
            ipAuthManager = new IpAuthManager(
                HeraldConfig.ipAuthSettings(),
                store,
                auditLogger,
                this::getCurrentDiscordId,
                this::sendIpAuthenticationDm);
        } catch (IOException e) {
            try {
                auditLogger.close();
            } catch (IOException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
        log.info(
            "Herald IP authentication " + (HeraldConfig.ipAuthenticationEnabled ? "enabled" : "disabled")
                + "; DiscordSRV configuration remains authoritative for Discord behavior");
    }

    private String getCurrentDiscordId(java.util.UUID uuid) {
        if (discordSRV == null) return null;
        AccountLinkManager manager = discordSRV.getAccountLinkManager();
        return manager != null ? manager.getDiscordId(uuid) : null;
    }

    private void sendIpAuthenticationDm(String discordId, String message) {
        net.dv8tion.jda.api.JDA jda = DiscordUtil.getJda();
        if (jda == null) {
            log.warn("Could not send Herald IP-auth DM because JDA is not ready");
            return;
        }
        jda.retrieveUserById(discordId)
            .queue(
                user -> user.openPrivateChannel()
                    .queue(
                        channel -> channel.sendMessage(message)
                            .queue(
                                ignored -> {},
                                error -> log
                                    .warn("Could not deliver Herald IP-auth DM to Discord ID " + discordId, error)),
                        error -> log.warn("Could not open a DM channel for Discord ID " + discordId, error)),
                error -> log.warn("Could not resolve Discord ID " + discordId + " for Herald IP-auth DM", error));
    }

    public String handleIpVerificationMessage(String content, String authorDiscordId) {
        return ipAuthManager != null ? ipAuthManager.handleDiscordMessage(content, authorDiscordId) : null;
    }

    public boolean isIpVerificationMessage(String content) {
        return ipAuthManager != null && ipAuthManager.recognizesVerificationMessage(content);
    }

    public Future<LoginDecision> beginPreAdmissionLogin(GameProfile profile, SocketAddress remoteAddress) {
        return preAdmissionLoginHandler.begin(profile, remoteAddress);
    }

    private AuthenticationReadiness.State authenticationStateForLogin() {
        AuthenticationReadiness.State state = authenticationReadiness.getState();
        if (state == AuthenticationReadiness.State.FAILED) return state;

        if (state == AuthenticationReadiness.State.LOADING && discordSRV != null && !discordSRV.isEnabled()) {
            authenticationReadiness.markFailed("DiscordSRV was disabled during initialization");
            return AuthenticationReadiness.State.FAILED;
        }
        if (state == AuthenticationReadiness.State.LOADING) return state;

        String missingComponent = describeMissingAuthenticationComponent();
        if (missingComponent != null) {
            authenticationReadiness.markFailed(missingComponent);
            return AuthenticationReadiness.State.FAILED;
        }

        JDA jda = discordSRV.getJda();
        if (jda == null) {
            authenticationReadiness.markFailed("DiscordSRV JDA is unavailable");
            return AuthenticationReadiness.State.FAILED;
        }

        String jdaStatus = jda.getStatus()
            .name();
        boolean connected = "CONNECTED".equals(jdaStatus);
        boolean permanentFailure = isPermanentJdaFailure(jdaStatus);
        String reason = permanentFailure ? "Discord JDA entered terminal state " + jdaStatus
            : "Discord connection state is " + jdaStatus;
        return authenticationReadiness.refreshAvailability(true, connected, permanentFailure, reason);
    }

    public LoginDecision checkLoginBeforeAdmission(String username, UUID uuid, InetAddress address,
        InetSocketAddress socketAddress) {
        AuthenticationReadiness.State state = authenticationStateForLogin();
        if (state == AuthenticationReadiness.State.LOADING) {
            return LoginDecision.reject("Herald is still loading. Please try again shortly.");
        }
        if (state == AuthenticationReadiness.State.UNAVAILABLE) {
            return LoginDecision.reject("Herald authentication is temporarily unavailable. Please try again shortly.");
        }
        if (state == AuthenticationReadiness.State.FAILED) {
            return LoginDecision.reject("Herald failed to load. Please contact a server administrator.");
        }

        if (username == null || uuid == null || address == null || socketAddress == null) {
            return LoginDecision.reject("Herald could not determine the source of this login. Please try again.");
        }

        AsyncPlayerPreLoginEvent preLoginEvent = new AsyncPlayerPreLoginEvent(username, uuid, address);
        Bukkit.getPluginManager()
            .callEvent(preLoginEvent);
        if (!preLoginEvent.getLoginResult()
            .allows()) {
            return LoginDecision.reject(preLoginEvent.getKickMessage());
        }

        CraftLoginPlayer loginPlayer = new CraftLoginPlayer(new GameProfile(uuid, username), socketAddress);
        PlayerLoginEvent loginEvent = new PlayerLoginEvent(loginPlayer, address);
        Bukkit.getPluginManager()
            .callEvent(loginEvent);
        if (loginEvent.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return LoginDecision.reject(loginEvent.getKickMessage());
        }

        if (HeraldConfig.ipAuthenticationEnabled && ipAuthManager == null) {
            return LoginDecision.reject(
                "Herald IP authentication is temporarily unavailable.\n\nThe connection was rejected for safety.");
        }

        if (ipAuthManager != null) {
            IpAuthManager.LoginResult ipAuthResult = ipAuthManager.checkLogin(username, uuid, address);
            if (!ipAuthResult.isAllowed()) return LoginDecision.reject(ipAuthResult.getKickMessage());
        }

        loginPlayer.markAdmissionAccepted();
        return LoginDecision.allow();
    }

    /** Called by DiscordSRV's initialization thread even when initialization returns early or throws. */
    public void onDiscordInitializationFinished(boolean completedNormally) {
        if (!completedNormally) {
            authenticationReadiness.markFailed("DiscordSRV initialization terminated with an exception");
            return;
        }
        markAuthenticationReadyOrFailed();
    }

    private void markAuthenticationReadyOrFailed() {
        String missingComponent = describeMissingAuthenticationComponent();
        if (missingComponent != null) {
            authenticationReadiness.markFailed(missingComponent);
            return;
        }

        JDA jda = discordSRV.getJda();
        if (jda == null) {
            authenticationReadiness.markFailed("DiscordSRV initialization completed without JDA");
            return;
        }
        String jdaStatus = jda.getStatus()
            .name();
        if ("CONNECTED".equals(jdaStatus)) {
            authenticationReadiness.markReady();
        } else if (isPermanentJdaFailure(jdaStatus)) {
            authenticationReadiness.markFailed("DiscordSRV initialization ended with JDA state " + jdaStatus);
        } else {
            authenticationReadiness.markUnavailable("Discord connection state is " + jdaStatus);
        }
    }

    private String describeMissingAuthenticationComponent() {
        DiscordSRV current = discordSRV;
        if (current == null) return "DiscordSRV is unavailable";
        if (!current.isEnabled()) return "DiscordSRV is disabled";
        if (!DiscordSRV.isReady) return "DiscordSRV initialization completed without becoming ready";
        if (current.getAccountLinkManager() == null) return "DiscordSRV account linking is unavailable";
        if (HeraldConfig.ipAuthenticationEnabled && ipAuthManager == null) {
            return "Herald IP authentication is unavailable";
        }
        return null;
    }

    private static boolean isPermanentJdaFailure(String status) {
        return "SHUTTING_DOWN".equals(status) || "SHUTDOWN".equals(status) || "FAILED_TO_LOGIN".equals(status);
    }

    public void serverStarted(FMLServerStartedEvent event) {
        if (discordSRV == null) return;
        // Channel placeholders may query ServerConfigurationManager, which Forge does not create until this event.
        discordSRV.restartChannelUpdaters();
        processAlert(event);
    }

    @SubscribeEvent
    public void onNativeEvent(cpw.mods.fml.common.eventhandler.Event event) {
        processAlert(event);
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
        if (deathEvent.isCancelled()) event.setCanceled(true);
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
            if (commandEvent.isCancelled()) event.setCanceled(true);
        } else {
            String serverCommand = command.startsWith("/") ? command.substring(1) : command;
            CommandSender sender = event.sender == MinecraftServer.getServer() ? Bukkit.getConsoleSender()
                : new CraftCommandSender(event.sender);
            ServerCommandEvent commandEvent = event.sender instanceof RConConsoleSource
                ? new RemoteServerCommandEvent(sender, serverCommand)
                : new ServerCommandEvent(sender, serverCommand);
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
        if (chatEvent.isCancelled()) event.setCanceled(true);
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

    public void rememberInitialLinkAttempt(String linkingCode, String username, java.util.UUID uuid,
        String rawAddress) {
        if (ipAuthManager == null || rawAddress == null) return;
        try {
            ipAuthManager.rememberInitialLinkAttempt(linkingCode, username, uuid, InetAddress.getByName(rawAddress));
        } catch (java.net.UnknownHostException e) {
            log.warn("Could not capture the IP that initiated Discord registration for " + uuid, e);
        }
    }

    /** Completes only the enrollment bound to the exact code DiscordSRV successfully consumed. */
    public void completeInitialLinkEnrollment(String linkingCode, java.util.UUID uuid, String discordId) {
        if (ipAuthManager != null && ipAuthManager.completeInitialLinkEnrollment(linkingCode, uuid, discordId)) {
            log.info("Automatically enrolled the Discord-linking login IP for Minecraft UUID " + uuid);
        }
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

    public void shutdown(FMLServerStoppingEvent event) {
        preAdmissionLoginHandler.close();
        processAlert(event);
        if (ipAuthManager != null) {
            try {
                ipAuthManager.close();
            } catch (IOException e) {
                log.error("Could not close Herald IP-auth resources", e);
            } finally {
                ipAuthManager = null;
            }
        }
        if (discordSRV != null && discordSRV.isEnabled()) {
            log.info("Shutting down Herald DiscordSRV");
            discordSRV.onDisable();
        }
        if (craftServer != null) {
            craftServer.getScheduler()
                .shutdown();
        }
    }

    private void processAlert(Object event) {
        if (discordSRV == null || !discordSRV.isEnabled() || discordSRV.getAlertListener() == null) return;
        discordSRV.getAlertListener()
            .processEvent(event);
    }

    public DiscordSRV getDiscordSRV() {
        return discordSRV;
    }

    public boolean isEnabled() {
        return discordSRV != null && discordSRV.isEnabled() && DiscordSRV.isReady;
    }
}
