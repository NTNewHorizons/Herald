package org.bukkit.configuration.file;

import java.io.File;
import org.bukkit.configuration.Configuration;

public class YamlConfiguration extends Configuration {
    public static YamlConfiguration loadConfiguration(File file) {
        return new YamlConfiguration();
    }

    public static YamlConfiguration loadConfiguration(String yaml) {
        return new YamlConfiguration();
    }
}
