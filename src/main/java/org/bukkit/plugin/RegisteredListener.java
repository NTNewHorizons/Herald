package org.bukkit.plugin;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RegisteredListener {

    private final Listener listener;
    private final EventPriority priority;
    private final Plugin plugin;
    private final EventExecutor executor;
    private final boolean ignoreCancelled;

    public RegisteredListener(Listener listener, EventPriority priority, Plugin plugin) {
        this.listener = listener;
        this.priority = priority;
        this.plugin = plugin;
        this.executor = null;
        this.ignoreCancelled = false;
    }

    public RegisteredListener(Listener listener, EventExecutor executor, EventPriority priority, Plugin plugin,
        boolean ignored) {
        this.listener = listener;
        this.executor = executor;
        this.priority = priority;
        this.plugin = plugin;
        this.ignoreCancelled = ignored;
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
        if (ignoreCancelled && event instanceof Cancellable && ((Cancellable) event).isCancelled()) return;
        if (executor != null) {
            executor.execute(listener, event);
        }
    }
}
