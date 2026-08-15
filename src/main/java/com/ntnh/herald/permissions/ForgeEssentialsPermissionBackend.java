package com.ntnh.herald.permissions;

import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import net.minecraftforge.permission.PermissionLevel;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.forgeessentials.api.APIRegistry;
import com.forgeessentials.api.UserIdent;
import com.forgeessentials.api.permissions.GroupEntry;
import com.forgeessentials.api.permissions.IPermissionsHelper;
import com.forgeessentials.api.permissions.PermissionEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import github.scarsz.discordsrv.util.SchedulerUtil;

public final class ForgeEssentialsPermissionBackend implements PermissionBackend {

    public ForgeEssentialsPermissionBackend() {
        registerPermissions();
        APIRegistry.getFEEventBus()
            .register(this);
    }

    private static void registerPermissions() {
        IPermissionsHelper permissions = permissions();

        String[] playerPermissions = { "discordsrv.player", "discordsrv.chat", "discordsrv.help", "discordsrv.link",
            "discordsrv.linked", "discordsrv.discord", "discordsrv.nicknamesync" };
        for (String permission : playerPermissions) {
            permissions.registerPermission(permission, PermissionLevel.TRUE);
        }

        String[] adminPermissions = { "discordsrv.admin", "discordsrv.updatenotification", "discordsrv.bcast",
            "discordsrv.reload", "discordsrv.debug", "discordsrv.link.others", "discordsrv.linked.others",
            "discordsrv.unlink", "discordsrv.unlink.others", "discordsrv.resync", "discordsrv.groupsyncwithcommands",
            "discordsrv.language" };
        for (String permission : adminPermissions) {
            permissions.registerPermission(permission, PermissionLevel.OP);
        }

        permissions.registerPermission("discordsrv.silentjoin", PermissionLevel.FALSE);
        permissions.registerPermission("discordsrv.silentquit", PermissionLevel.FALSE);

        DiscordSRV plugin = DiscordSRV.getPlugin();
        if (plugin == null) return;
        for (String group : plugin.getGroupSynchronizables()
            .keySet()) {
            permissions.registerPermission("discordsrv.sync." + group, PermissionLevel.FALSE);
            permissions.registerPermission("discordsrv.sync.deny." + group, PermissionLevel.FALSE);
        }
    }

    @Override
    public boolean isAvailable() {
        return APIRegistry.perms != null;
    }

    @Override
    public String getName() {
        return "ForgeEssentials";
    }

    @Override
    public String getPrimaryGroup(OfflinePlayer player) {
        String group = permissions().getPrimaryGroup(ident(player));
        return group != null ? group : "default";
    }

    @Override
    public String[] getPlayerGroups(OfflinePlayer player) {
        return GroupEntry.toList(permissions().getPlayerGroups(ident(player)))
            .toArray(new String[0]);
    }

    @Override
    public String[] getGroups() {
        Set<String> groups = permissions().getServerZone()
            .getGroups();
        return groups.toArray(new String[0]);
    }

    @Override
    public boolean playerInGroup(OfflinePlayer player, String group) {
        return group != null && containsGroup(permissions().getPlayerGroups(ident(player)), group);
    }

    @Override
    public boolean playerHas(OfflinePlayer player, String permission) {
        return permission != null && permissions().checkUserPermission(ident(player), permission);
    }

    @Override
    public boolean playerAddGroup(OfflinePlayer player, String group) {
        if (group == null || !permissions().groupExists(group)) return false;
        UserIdent ident = ident(player);
        permissions().addPlayerToGroup(ident, group);
        return containsGroup(permissions().getStoredPlayerGroups(ident), group);
    }

    @Override
    public boolean playerRemoveGroup(OfflinePlayer player, String group) {
        if (group == null || !permissions().groupExists(group)) return false;
        UserIdent ident = ident(player);
        permissions().removePlayerFromGroup(ident, group);
        return !containsGroup(permissions().getStoredPlayerGroups(ident), group);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onModifyGroups(PermissionEvent.User.ModifyGroups event) {
        if (event.isCanceled() || event.ident == null) return;
        UUID uuid = event.ident.getUuid();
        if (uuid == null || DiscordSRV.getPlugin() == null || !DiscordSRV.isReady) return;

        SchedulerUtil.runTaskLaterAsynchronously(DiscordSRV.getPlugin(), () -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player == null || DiscordSRV.getPlugin()
                .getGroupSynchronizationManager() == null) return;
            DiscordSRV.getPlugin()
                .getGroupSynchronizationManager()
                .resync(
                    player,
                    GroupSynchronizationManager.SyncDirection.TO_DISCORD,
                    GroupSynchronizationManager.SyncCause.MINECRAFT_GROUP_EDIT_COMMAND);
        }, 1);
    }

    private static IPermissionsHelper permissions() {
        if (APIRegistry.perms == null) throw new IllegalStateException("ForgeEssentials permissions are not ready");
        return APIRegistry.perms;
    }

    private static UserIdent ident(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) throw new IllegalArgumentException("player cannot be null");
        return UserIdent.get(player.getUniqueId(), player.getName());
    }

    private static boolean containsGroup(SortedSet<GroupEntry> groups, String expected) {
        if (groups == null) return false;
        for (GroupEntry group : groups) {
            if (group.getGroup()
                .equalsIgnoreCase(expected)) return true;
        }
        return false;
    }
}
