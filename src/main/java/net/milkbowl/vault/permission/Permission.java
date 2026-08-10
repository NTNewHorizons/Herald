package net.milkbowl.vault.permission;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.ntnh.herald.permissions.PermissionBackend;
import com.ntnh.herald.permissions.VanillaPermissionBackend;

import cpw.mods.fml.common.Loader;

/**
 * Forge-native stand-in for the Vault permission API. It delegates to ForgeEssentials when its permissions module is
 * available, with an {@code op}/{@code default} fallback for standalone Herald servers.
 */
public class Permission {

    private final PermissionBackend fallback = new VanillaPermissionBackend();
    private volatile PermissionBackend forgeEssentials;

    public String getName() {
        return backend().getName();
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
        return backend().getPrimaryGroup(player);
    }

    public String getPrimaryGroup(String world, OfflinePlayer player) {
        return backend().getPrimaryGroup(player);
    }

    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        return backend().getPlayerGroups(player);
    }

    public String[] getGroups() {
        return backend().getGroups();
    }

    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return backend().playerInGroup(player, group);
    }

    public boolean playerHas(String world, OfflinePlayer player, String permission) {
        return backend().playerHas(player, permission);
    }

    public boolean playerAddGroup(String world, OfflinePlayer player, String group) {
        return backend().playerAddGroup(player, group);
    }

    public boolean playerRemoveGroup(String world, OfflinePlayer player, String group) {
        return backend().playerRemoveGroup(player, group);
    }

    private PermissionBackend backend() {
        PermissionBackend current = forgeEssentials;
        if (current != null && current.isAvailable()) return current;
        if (!Loader.isModLoaded("ForgeEssentials")) return fallback;

        synchronized (this) {
            current = forgeEssentials;
            if (current == null) {
                try {
                    current = (PermissionBackend) Class
                        .forName("com.ntnh.herald.permissions.ForgeEssentialsPermissionBackend")
                        .newInstance();
                    forgeEssentials = current;
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    return fallback;
                }
            }
        }
        return current.isAvailable() ? current : fallback;
    }
}
