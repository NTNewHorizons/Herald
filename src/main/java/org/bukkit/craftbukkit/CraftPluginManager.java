package org.bukkit.craftbukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;

public class CraftPluginManager implements PluginManager {

    // DiscordSRV registers listeners from its asynchronous initialization thread while Forge dispatches events on
    // the server thread. The registry therefore needs safe publication across threads.
    private final Map<Plugin, List<RegisteredListener>> listenersByPlugin = new ConcurrentHashMap<>();

    @Override
    public void registerEvents(Listener listener, Plugin plugin) {
        for (Method method : listener.getClass()
            .getMethods()) {
            EventHandler handler = method.getAnnotation(EventHandler.class);
            if (handler == null) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1 || !Event.class.isAssignableFrom(parameters[0])) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) parameters[0];
            registerEvent(eventClass, listener, handler.priority(), (registered, event) -> {
                try {
                    method.invoke(registered, event);
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    Throwable cause = exception instanceof InvocationTargetException
                        ? ((InvocationTargetException) exception).getCause()
                        : exception;
                    throw new RuntimeException("Could not dispatch " + event.getEventName(), cause);
                }
            }, plugin, handler.ignoreCancelled());
        }
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
        for (RegisteredListener listener : event.getHandlers()
            .getRegisteredListeners()) {
            if (listener.getPlugin() != null && !listener.getPlugin()
                .isEnabled()) continue;
            try {
                listener.callEvent(event);
            } catch (EventException | RuntimeException exception) {
                exception.printStackTrace();
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
        for (Plugin plugin : listenersByPlugin.keySet()) {
            HandlerList.unregisterAll(plugin);
        }
        listenersByPlugin.clear();
    }

    @Override
    public Set<RegisteredListener> getRegisteredListeners(Plugin plugin) {
        return new HashSet<>(listenersByPlugin.getOrDefault(plugin, java.util.Collections.emptyList()));
    }

    @Override
    public Plugin getPlugin(Class<?> clazz) {
        return null;
    }

    @Override
    public void registerEvent(Class<? extends Event> eventClass, Listener listener, EventPriority priority,
        EventExecutor executor, Plugin plugin, boolean ignoreCancelled) {
        RegisteredListener registered = new RegisteredListener(
            listener,
            executor,
            priority,
            plugin,
            ignoreCancelled);
        listenersByPlugin.computeIfAbsent(plugin, ignored -> new CopyOnWriteArrayList<>())
            .add(registered);
        HandlerList.getHandlerList(eventClass)
            .register(registered);
    }
}
