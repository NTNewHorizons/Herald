package org.bukkit.craftbukkit;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

import java.util.HashMap;
import java.util.Map;

public class CraftServicesManager implements ServicesManager {

    private final Map<Class<?>, RegisteredServiceProvider<?>> providers = new HashMap<>();

    @Override
    public <T> void register(Class<T> service, T provider, Plugin plugin, ServicePriority priority) {
        providers.put(service, new RegisteredServiceProvider<>(service, provider, priority, plugin));
    }

    @Override
    public <T> T load(Class<T> service) {
        RegisteredServiceProvider<T> registration = getRegistration(service);
        return registration != null ? registration.getProvider() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RegisteredServiceProvider<T> getRegistration(Class<T> service) {
        return (RegisteredServiceProvider<T>) providers.get(service);
    }
}
