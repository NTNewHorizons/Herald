package org.bukkit.plugin.java;

public class PluginClassLoader extends ClassLoader {
    public PluginClassLoader(ClassLoader parent) {
        super(parent);
    }

    public JavaPlugin getPlugin() {
        return null;
    }
}
