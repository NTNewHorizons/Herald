package org.bukkit.metadata;

import java.util.List;

public interface Metadatable {
    void setMetadata(String metadataKey, MetadataValue metadataValue);
    List<MetadataValue> getMetadata(String metadataKey);
    boolean hasMetadata(String metadataKey);
    void removeMetadata(String metadataKey, Object plugin);
}
