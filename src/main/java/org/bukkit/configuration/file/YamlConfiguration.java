package org.bukkit.configuration.file;

import java.io.File;
import org.bukkit.configuration.Configuration;

public class YamlConfiguration extends Configuration {
    private YamlConfigurationOptions options = new YamlConfigurationOptions(this);

    public static YamlConfiguration loadConfiguration(File file) {
        return new YamlConfiguration();
    }

    public static YamlConfiguration loadConfiguration(String yaml) {
        return new YamlConfiguration();
    }

    public YamlConfigurationOptions options() {
        return options;
    }
}
