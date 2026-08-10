package com.ntnh.herald.permissions;

import org.bukkit.OfflinePlayer;

public interface PermissionBackend {

    boolean isAvailable();

    String getName();

    String getPrimaryGroup(OfflinePlayer player);

    String[] getPlayerGroups(OfflinePlayer player);

    String[] getGroups();

    boolean playerInGroup(OfflinePlayer player, String group);

    boolean playerHas(OfflinePlayer player, String permission);

    boolean playerAddGroup(OfflinePlayer player, String group);

    boolean playerRemoveGroup(OfflinePlayer player, String group);
}
