package org.bukkit.permissions;

import java.util.Set;

public class Permission {
    private final String name;
    private final String description;
    private final PermissionDefault defaultValue;

    public Permission(String name) {
        this(name, "", PermissionDefault.OP);
    }

    public Permission(String name, String description) {
        this(name, description, PermissionDefault.OP);
    }

    public Permission(String name, String description, PermissionDefault defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PermissionDefault getDefault() {
        return defaultValue;
    }

    public Set<Permission> getChildren() {
        return java.util.Collections.emptySet();
    }
}
