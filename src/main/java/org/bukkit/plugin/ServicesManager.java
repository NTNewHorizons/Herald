package org.bukkit.plugin;

public interface ServicesManager {
    <T> void register(Class<T> service, T provider, Plugin plugin, ServicePriority priority);
    <T> T load(Class<T> service);
    <T> RegisteredServiceProvider<T> getRegistration(Class<T> service);
}
