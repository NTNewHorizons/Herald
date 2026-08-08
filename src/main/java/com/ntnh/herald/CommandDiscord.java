package com.ntnh.herald;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.command.CraftConsoleCommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import github.scarsz.discordsrv.DiscordSRV;

public class CommandDiscord extends CommandBase {

    private static final List<String> ALIASES = Collections.singletonList("discordsrv");

    @Override
    public String getCommandName() {
        return "discord";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/discord <help|link|unlink|linked|broadcast|debug|reload|resync|language|debugger>";
    }

    @Override
    public List<String> getCommandAliases() {
        return ALIASES;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        DiscordSRV plugin = DiscordSRV.getPlugin();
        if (plugin == null) {
            sender.addChatMessage(new ChatComponentText("\u00a7cDiscordSRV is not initialized yet."));
            return;
        }

        plugin.onCommand(toBukkitSender(sender), createBukkitCommand(plugin), getCommandName(), args);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        DiscordSRV plugin = DiscordSRV.getPlugin();
        if (plugin == null) return Collections.emptyList();

        String[] completionArgs = args.length == 0 ? new String[] { "" } : Arrays.copyOf(args, args.length);
        List<String> completions = plugin
            .onTabComplete(toBukkitSender(sender), createBukkitCommand(plugin), getCommandName(), completionArgs);
        return completions != null ? completions : Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    private static CommandSender toBukkitSender(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            if (org.bukkit.Bukkit.getServer() instanceof CraftServer) {
                return ((CraftServer) org.bukkit.Bukkit.getServer()).getCraftPlayer((EntityPlayerMP) sender);
            }
            return new CraftPlayer((EntityPlayerMP) sender);
        }
        return CraftConsoleCommandSender.getInstance();
    }

    private static PluginCommand createBukkitCommand(DiscordSRV plugin) {
        PluginCommand command = new PluginCommand("discord", plugin);
        command.setAliases(ALIASES);
        return command;
    }
}
