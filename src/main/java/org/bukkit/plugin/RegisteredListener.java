package org.bukkit.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RegisteredListener {
    private final Listener listener;
    private final EventPriority priority;
    private final Plugin plugin;
    private final EventExecutor executor;

    public RegisteredListener(Listener listener, EventPriority priority, Plugin plugin) {
        this.listener = listener;
        this.priority = priority;
        this.plugin = plugin;
        this.executor = null;
    }

    public RegisteredListener(Listener listener, EventExecutor executor, EventPriority priority, Plugin plugin, boolean ignored) {
        this.listener = listener;
        this.executor = executor;
        this.priority = priority;
        this.plugin = plugin;
    }

    public Listener getListener() {
        return listener;
    }

    public EventPriority getPriority() {
        return priority;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void callEvent(Event event) throws EventException {
        if (executor != null) {
            executor.execute(listener, event);
        }
    }
}
