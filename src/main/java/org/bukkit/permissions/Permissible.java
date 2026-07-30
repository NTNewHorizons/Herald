package org.bukkit.permissions;

import java.util.Set;
import org.bukkit.plugin.Plugin;

public interface Permissible {
    boolean isPermissionSet(String name);
    boolean hasPermission(String name);
    PermissionAttachment addAttachment(Plugin plugin, String name, boolean value);
    Set<PermissionAttachmentInfo> getEffectivePermissions();
}
