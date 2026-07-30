package org.bukkit.craftbukkit;

import java.io.File;
import net.minecraft.world.WorldServer;
import org.bukkit.GameRule;
import org.bukkit.World;

public class CraftWorld implements World {
    private final WorldServer world;

    public CraftWorld(WorldServer world) {
        this.world = world;
    }

    @Override
    public String getName() {
        return world.getWorldInfo().getWorldName();
    }

    @Override
    public File getWorldFolder() {
        return null;
    }

    @Override
    public String getGameRuleValue(String rule) {
        return "";
    }

    @Override
    public <T> T getGameRuleValue(GameRule<T> rule) {
        return null;
    }
}
