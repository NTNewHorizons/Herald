package org.bukkit.plugin;

import java.util.List;
import java.util.Map;
import org.bukkit.permissions.Permission;

public class PluginDescriptionFile {
    private final String name;
    private final String version;
    private final List<Permission> permissions;

    public PluginDescriptionFile(String name, String version) {
        this.name = name;
        this.version = version;
        this.permissions = new java.util.ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public String getFullName() {
        return name + " v" + version;
    }

    public Map<String, Map<String, Object>> getCommands() {
        return java.util.Collections.emptyMap();
    }
}
