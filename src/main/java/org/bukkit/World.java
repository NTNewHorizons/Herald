package org.bukkit;

import java.io.File;

public interface World {
    String getName();
    File getWorldFolder();
    String getGameRuleValue(String rule);
    <T> T getGameRuleValue(GameRule<T> rule);
}
