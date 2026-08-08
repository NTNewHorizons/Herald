package com.ntnh.herald.permissions;

import org.bukkit.OfflinePlayer;

public final class VanillaPermissionBackend implements PermissionBackend {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getName() {
        return "Herald";
    }

    @Override
    public String getPrimaryGroup(OfflinePlayer player) {
        return groupFor(player);
    }

    @Override
    public String[] getPlayerGroups(OfflinePlayer player) {
        return new String[] { groupFor(player) };
    }

    @Override
    public String[] getGroups() {
        return new String[] { "default", "op" };
    }

    @Override
    public boolean playerInGroup(OfflinePlayer player, String group) {
        return group != null && groupFor(player).equalsIgnoreCase(group);
    }

    @Override
    public boolean playerHas(OfflinePlayer player, String permission) {
        if (permission == null) return false;
        switch (permission) {
            case "discordsrv.player":
            case "discordsrv.chat":
            case "discordsrv.help":
            case "discordsrv.link":
            case "discordsrv.linked":
            case "discordsrv.discord":
            case "discordsrv.nicknamesync":
                return true;
            case "discordsrv.admin":
            case "discordsrv.updatenotification":
            case "discordsrv.bcast":
            case "discordsrv.reload":
            case "discordsrv.debug":
            case "discordsrv.link.others":
            case "discordsrv.linked.others":
            case "discordsrv.unlink":
            case "discordsrv.unlink.others":
            case "discordsrv.resync":
            case "discordsrv.groupsyncwithcommands":
            case "discordsrv.language":
                return player != null && player.isOp();
            default:
                return false;
        }
    }

    @Override
    public boolean playerAddGroup(OfflinePlayer player, String group) {
        if (player == null || group == null) return false;
        if ("op".equalsIgnoreCase(group)) {
            player.setOp(true);
            return player.isOp();
        }
        if ("default".equalsIgnoreCase(group)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean playerRemoveGroup(OfflinePlayer player, String group) {
        if (player == null || group == null) return false;
        if ("op".equalsIgnoreCase(group)) {
            player.setOp(false);
            return !player.isOp();
        }
        return false;
    }

    private static String groupFor(OfflinePlayer player) {
        return player != null && player.isOp() ? "op" : "default";
    }
}
