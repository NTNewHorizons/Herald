package org.bukkit.craftbukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;

public class CraftPluginManager implements PluginManager {

    private final Map<Plugin, List<Listener>> listenersByPlugin = new HashMap<>();

    @Override
    public void registerEvents(Listener listener, Plugin plugin) {
        listenersByPlugin.computeIfAbsent(plugin, k -> new ArrayList<>())
            .add(listener);
    }

    @Override
    public Plugin getPlugin(String name) {
        return null;
    }

    @Override
    public Plugin[] getPlugins() {
        return new Plugin[0];
    }

    @Override
    public boolean isPluginEnabled(String name) {
        return false;
    }

    @Override
    public boolean isPluginEnabled(Plugin plugin) {
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (plugin != null && plugin.isEnabled()) {
            if (plugin instanceof org.bukkit.plugin.java.JavaPlugin) {
                ((org.bukkit.plugin.java.JavaPlugin) plugin).setEnabled(false);
            }
        }
    }

    @Override
    public void callEvent(Event event) {
        for (List<Listener> listeners : listenersByPlugin.values()) {
            for (Listener listener : listeners) {
                for (Method method : listener.getClass()
                    .getMethods()) {
                    EventHandler handler = method.getAnnotation(EventHandler.class);
                    if (handler == null) continue;
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 1 && params[0].isAssignableFrom(event.getClass())) {
                        try {
                            method.invoke(listener, event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void registerInterface(Class<? extends PluginLoader> loader) {}

    @Override
    public boolean useTimings() {
        return false;
    }

    @Override
    public void clearPlugins() {
        listenersByPlugin.clear();
    }

    @Override
    public Set<RegisteredListener> getRegisteredListeners(Plugin plugin) {
        return new HashSet<>();
    }

    @Override
    public Plugin getPlugin(Class<?> clazz) {
        return null;
    }

    @Override
    public void registerEvent(Class<? extends Event> eventClass, Listener listener, EventPriority priority,
        EventExecutor executor, Plugin plugin, boolean ignoreCancelled) {
        listenersByPlugin.computeIfAbsent(plugin, k -> new ArrayList<>())
            .add(listener);
    }
}
