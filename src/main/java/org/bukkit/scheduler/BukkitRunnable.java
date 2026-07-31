package org.bukkit.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public abstract class BukkitRunnable implements Runnable {

    private int taskId = -1;

    @Override
    public abstract void run();

    public synchronized void cancel() {
        if (taskId != -1) {
            Bukkit.getScheduler()
                .cancelTask(taskId);
        }
        taskId = -1;
    }

    public synchronized BukkitTask runTask(Plugin plugin) {
        if (taskId != -1) throw new IllegalStateException("Already running");
        return null;
    }

    public synchronized BukkitTask runTaskAsynchronously(Plugin plugin) {
        if (taskId != -1) throw new IllegalStateException("Already running");
        return null;
    }

    public synchronized BukkitTask runTaskLater(Plugin plugin, long delay) {
        if (taskId != -1) throw new IllegalStateException("Already running");
        return null;
    }

    public synchronized BukkitTask runTaskLaterAsynchronously(Plugin plugin, long delay) {
        if (taskId != -1) throw new IllegalStateException("Already running");
        return null;
    }

    public synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period) {
        if (taskId != -1) throw new IllegalStateException("Already running");
        return null;
    }

    public synchronized BukkitTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period) {
        if (taskId != -1) throw new IllegalStateException("Already running");
        return null;
    }

    public int getTaskId() {
        return taskId;
    }
}
