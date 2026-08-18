package com.ntnh.herald;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import com.mojang.authlib.GameProfile;
import com.ntnh.herald.security.IpAuthManager;
import com.ntnh.herald.security.TrustedIp;

public final class CommandHerald extends CommandBase {

    private final IpAuthManager ipAuthManager;

    public CommandHerald(IpAuthManager ipAuthManager) {
        this.ipAuthManager = ipAuthManager;
    }

    @Override
    public String getCommandName() {
        return "herald";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/herald ipauth <status|reset> <player|uuid>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 4;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 3 || !"ipauth".equalsIgnoreCase(args[0])) {
            reply(sender, "\u00a7cUsage: " + getCommandUsage(sender));
            return;
        }

        GameProfile profile = resolveProfile(args[2]);
        if (profile == null || profile.getId() == null) {
            reply(sender, "\u00a7cUnknown player or UUID: " + args[2]);
            return;
        }

        if ("status".equalsIgnoreCase(args[1])) {
            showStatus(sender, profile);
        } else if ("reset".equalsIgnoreCase(args[1])) {
            reset(sender, profile);
        } else {
            reply(sender, "\u00a7cUsage: " + getCommandUsage(sender));
        }
    }

    private void showStatus(ICommandSender sender, GameProfile profile) {
        IpAuthManager.Status status = ipAuthManager.getStatus(profile.getId());
        List<TrustedIp> trustedIps = status.getTrustedIps();
        reply(
            sender,
            "\u00a7bHerald IP auth for " + profile.getName()
                + " ("
                + profile.getId()
                + "): "
                + trustedIps.size()
                + " trusted IP(s), "
                + status.getPendingChallenges()
                + " pending challenge(s).");
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        for (TrustedIp trustedIp : trustedIps) {
            reply(
                sender,
                "\u00a77- " + trustedIp.getAddress()
                    .getText()
                    + " | first verified "
                    + format.format(new Date(trustedIp.getFirstVerifiedAt()))
                    + " | last seen "
                    + format.format(new Date(trustedIp.getLastSeenAt())));
        }
    }

    private void reset(ICommandSender sender, GameProfile profile) {
        try {
            ipAuthManager.reset(profile.getId());
            reply(sender, "\u00a7aReset trusted IPs and pending challenges for " + profile.getName() + ".");
        } catch (IOException e) {
            Herald.LOG.error("Could not reset IP authentication for " + profile.getId(), e);
            reply(sender, "\u00a7cCould not persist the IP-auth reset; nothing was changed.");
        }
    }

    private static GameProfile resolveProfile(String target) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return null;
        try {
            UUID uuid = UUID.fromString(target);
            GameProfile profile = server.func_152358_ax()
                .func_152652_a(uuid);
            return profile != null ? profile : new GameProfile(uuid, target);
        } catch (IllegalArgumentException ignored) {
            return server.func_152358_ax()
                .func_152655_a(target);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "ipauth");
        if (args.length == 2 && "ipauth".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "status", "reset");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 2;
    }

    private static void reply(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }
}
