package org.bukkit.entity;

public enum EntityType {
    PLAYER("PLAYER", Player.class, 0),
    WITHER("WITHER", Wither.class, 0);

    private final String name;
    private final Class<? extends Entity> clazz;
    private final int typeId;

    EntityType(String name, Class<? extends Entity> clazz, int typeId) {
        this.name = name;
        this.clazz = clazz;
        this.typeId = typeId;
    }

    public String getName() {
        return name;
    }

    public Class<? extends Entity> getEntityClass() {
        return clazz;
    }

    public int getTypeId() {
        return typeId;
    }
}
