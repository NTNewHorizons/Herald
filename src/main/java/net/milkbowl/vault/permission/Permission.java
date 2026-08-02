package net.milkbowl.vault.permission;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Forge-native stand-in for the Vault permission API. Vault does not exist on Forge 1.7.10, so the only group-like
 * distinction available is operator status. This provider is what powers DiscordSRV's group-role synchronization
 * ({@code op} / {@code default} groups) and the {@code %primarygroup%} placeholder.
 */
public class Permission {

    public String getName() {
        return "Herald";
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean hasSuperPermsCompat() {
        return false;
    }

    public boolean hasGroupSupport() {
        return true;
    }

    public String getPrimaryGroup(Player player) {
        return groupFor(player);
    }

    public String getPrimaryGroup(String world, OfflinePlayer player) {
        return groupFor(player);
    }

    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        return new String[] { groupFor(player) };
    }

    public String[] getGroups() {
        return new String[] { "default", "op" };
    }

    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return groupFor(player).equals(group);
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

    private static String groupFor(OfflinePlayer player) {
        if (player == null) return "default";
        Player online = player.getPlayer();
        return (online != null ? online.isOp() : player.isOp()) ? "op" : "default";
    }
}
