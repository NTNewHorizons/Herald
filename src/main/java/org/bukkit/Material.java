package org.bukkit;

public enum Material {

    AIR,
    STONE,
    GRASS,
    DIRT;

    public NamespacedKey getKey() {
        return new NamespacedKey("minecraft", name().toLowerCase());
    }
}
