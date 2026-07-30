package org.bukkit.entity;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.metadata.Metadatable;

public interface Entity extends Metadatable {
    String getName();
    Server getServer();
    World getWorld();
    EntityType getType();
}
