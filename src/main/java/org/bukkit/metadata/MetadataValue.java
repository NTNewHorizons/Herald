package org.bukkit.metadata;

import org.bukkit.plugin.Plugin;

public interface MetadataValue {
    boolean asBoolean();
    int asInt();
    double asDouble();
    String asString();
    Object value();
    Plugin getOwningPlugin();
    void invalidate();
}
