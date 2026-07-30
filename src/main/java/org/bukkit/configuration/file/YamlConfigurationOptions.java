package org.bukkit.configuration.file;

public class YamlConfigurationOptions {
    private final YamlConfiguration configuration;
    private int indent = 2;

    public YamlConfigurationOptions(YamlConfiguration configuration) {
        this.configuration = configuration;
    }

    public YamlConfiguration configuration() {
        return configuration;
    }

    public int indent() {
        return indent;
    }

    public YamlConfigurationOptions indent(int value) {
        this.indent = value;
        return this;
    }
}
