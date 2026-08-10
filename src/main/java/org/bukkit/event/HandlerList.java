package org.bukkit.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

public class HandlerList {

    private static ArrayList<HandlerList> allLists = new ArrayList<>();
    private static final Map<Class<? extends Event>, HandlerList> fallbackLists = new ConcurrentHashMap<>();

    private EnumMap<EventPriority, ArrayList<RegisteredListener>> handlerslots = new EnumMap<>(EventPriority.class);
    private volatile RegisteredListener[] handlers;

    public HandlerList() {
        for (EventPriority priority : EventPriority.values()) {
            handlerslots.put(priority, new ArrayList<>());
        }
        synchronized (HandlerList.class) {
            allLists.add(this);
        }
    }

    public synchronized void register(RegisteredListener listener) {
        handlerslots.get(listener.getPriority())
            .add(listener);
        handlers = null;
    }

    public synchronized void unregister(RegisteredListener listener) {
        for (List<RegisteredListener> listeners : handlerslots.values()) {
            listeners.remove(listener);
        }
        handlers = null;
    }

    public synchronized void unregister(Listener listener) {
        for (List<RegisteredListener> listeners : handlerslots.values()) {
            listeners.removeIf(registered -> registered.getListener() == listener);
        }
        handlers = null;
    }

    public static void unregisterAll(Plugin plugin) {
        for (HandlerList list : getHandlerLists()) {
            list.unregister(plugin);
        }
    }

    public static void unregisterAll(Listener listener) {
        for (HandlerList list : getHandlerLists()) {
            list.unregister(listener);
        }
    }

    public synchronized void unregister(Plugin plugin) {
        for (List<RegisteredListener> listeners : handlerslots.values()) {
            listeners.removeIf(registered -> registered.getPlugin() == plugin);
        }
        handlers = null;
    }

    public static HandlerList getHandlerList(Class<? extends Event> clazz) {
        try {
            return (HandlerList) clazz.getMethod("getHandlerList")
                .invoke(null);
        } catch (Exception e) {
            return fallbackLists.computeIfAbsent(clazz, ignored -> new HandlerList());
        }
    }

    public RegisteredListener[] getRegisteredListeners() {
        RegisteredListener[] current = handlers;
        if (current != null) return current;
        synchronized (this) {
            current = handlers;
            if (current == null) {
                List<RegisteredListener> baked = new ArrayList<>();
                for (EventPriority priority : EventPriority.values()) {
                    baked.addAll(handlerslots.get(priority));
                }
                current = baked.toArray(new RegisteredListener[0]);
                handlers = current;
            }
        }
        return current;
    }

    public static synchronized ArrayList<HandlerList> getHandlerLists() {
        return new ArrayList<>(allLists);
    }
}
