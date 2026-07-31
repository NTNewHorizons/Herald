package net.milkbowl.vault.permission;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Permission {

    public String getName() {
        return "Vault";
    }

    public boolean isEnabled() {
        return false;
    }

    public boolean hasSuperPermsCompat() {
        return false;
    }

    public boolean hasGroupSupport() {
        return false;
    }

    public String getPrimaryGroup(Player player) {
        return "default";
    }

    public String getPrimaryGroup(String world, OfflinePlayer player) {
        return "default";
    }

    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        return new String[0];
    }

    public String[] getGroups() {
        return new String[0];
    }

    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return false;
    }

    public boolean playerHas(String world, OfflinePlayer player, String permission) {
        return false;
    }

    public boolean playerAddGroup(String world, OfflinePlayer player, String group) {
        return false;
    }

    public boolean playerRemoveGroup(String world, OfflinePlayer player, String group) {
        return false;
    }
}
