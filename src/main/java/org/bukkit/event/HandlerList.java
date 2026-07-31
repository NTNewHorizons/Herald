package org.bukkit.event;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

public class HandlerList {

    private final List<RegisteredListener> listeners = new ArrayList<>();

    public void register(RegisteredListener listener) {
        listeners.add(listener);
    }

    public void unregister(RegisteredListener listener) {
        listeners.remove(listener);
    }

    public void unregister(Listener listener) {
        listeners.removeIf(rl -> rl.getListener() == listener);
    }

    public static void unregisterAll(Plugin plugin) {}

    public static void unregisterAll(Listener listener) {}

    public void unregister(Plugin plugin) {}

    public static HandlerList getHandlerList(Class<? extends Event> clazz) {
        try {
            return (HandlerList) clazz.getMethod("getHandlerList")
                .invoke(null);
        } catch (Exception e) {
            return new HandlerList();
        }
    }

    public RegisteredListener[] getRegisteredListeners() {
        return listeners.toArray(new RegisteredListener[0]);
    }

    public static ArrayList<HandlerList> getHandlerLists() {
        return new ArrayList<>();
    }
}
