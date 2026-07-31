package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

public interface BukkitWorker {

    Plugin getOwner();

    Thread getThread();

    int getTaskId();
}
