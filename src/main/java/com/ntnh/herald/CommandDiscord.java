package com.ntnh.herald;

import java.util.Arrays;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.command.CraftConsoleCommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;

import github.scarsz.discordsrv.DiscordSRV;

public class CommandDiscord extends CommandBase {

    @Override
    public String getCommandName() {
        return "discord";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/discord <help|link|unlink|linked|broadcast|debug|reload|resync|language|debugger>";
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

        CommandSender bukkitSender;
        if (sender instanceof EntityPlayerMP) {
            bukkitSender = new CraftPlayer((EntityPlayerMP) sender);
        } else {
            bukkitSender = CraftConsoleCommandSender.getInstance();
        }

        String command = args.length > 0 ? args[0] : "";
        String[] commandArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        plugin.getCommandManager()
            .handle(bukkitSender, command, commandArgs);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }
}
