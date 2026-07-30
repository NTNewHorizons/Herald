package org.bukkit.plugin;

import java.io.File;
import java.util.Set;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public interface PluginManager {
    void registerEvents(Listener listener, Plugin plugin);
    Plugin getPlugin(String name);
    Plugin[] getPlugins();
    boolean isPluginEnabled(String name);
    boolean isPluginEnabled(Plugin plugin);
    void disablePlugin(Plugin plugin);
    void callEvent(Event event);
    void registerInterface(Class<? extends PluginLoader> loader);
    boolean useTimings();
    void clearPlugins();
    Set<RegisteredListener> getRegisteredListeners(Plugin plugin);
    Plugin getPlugin(Class<?> clazz);

    void registerEvent(Class<? extends Event> eventClass, Listener listener, EventPriority priority, EventExecutor executor, Plugin plugin, boolean ignoreCancelled);
}
