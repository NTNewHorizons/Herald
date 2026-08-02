package org.bukkit.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public abstract class BukkitRunnable implements Runnable, BukkitTask {

    private int taskId = -1;
    private Plugin owner;
    private boolean sync;

    @Override
    public abstract void run();

    @Override
    public synchronized void cancel() {
        if (taskId != -1) {
            Bukkit.getScheduler()
                .cancelTask(taskId);
        }
        taskId = -1;
        owner = null;
    }

    @Override
    public Plugin getOwner() {
        return owner;
    }

    @Override
    public boolean isSync() {
        return sync;
    }

    public synchronized BukkitTask runTask(Plugin plugin) throws IllegalArgumentException {
        if (taskId != -1) throw new IllegalStateException("Already scheduled as " + taskId);
        this.owner = plugin;
        this.sync = true;
        taskId = Bukkit.getScheduler()
            .scheduleSyncDelayedTask(plugin, this, 0);
        return this;
    }

    public synchronized BukkitTask runTaskAsynchronously(Plugin plugin) throws IllegalArgumentException {
        if (taskId != -1) throw new IllegalStateException("Already scheduled as " + taskId);
        this.owner = plugin;
        this.sync = false;
        taskId = Bukkit.getScheduler()
            .scheduleAsyncDelayedTask(plugin, this, 0);
        return this;
    }

    public synchronized BukkitTask runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException {
        if (taskId != -1) throw new IllegalStateException("Already scheduled as " + taskId);
        this.owner = plugin;
        this.sync = true;
        taskId = Bukkit.getScheduler()
            .scheduleSyncDelayedTask(plugin, this, delay);
        return this;
    }

    public synchronized BukkitTask runTaskLaterAsynchronously(Plugin plugin, long delay)
        throws IllegalArgumentException {
        if (taskId != -1) throw new IllegalStateException("Already scheduled as " + taskId);
        this.owner = plugin;
        this.sync = false;
        taskId = Bukkit.getScheduler()
            .scheduleAsyncDelayedTask(plugin, this, delay);
        return this;
    }

    public synchronized BukkitTask runTaskTimer(Plugin plugin, long delay, long period)
        throws IllegalArgumentException {
        if (taskId != -1) throw new IllegalStateException("Already scheduled as " + taskId);
        this.owner = plugin;
        this.sync = true;
        taskId = Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(plugin, this, delay, period);
        return this;
    }

    public synchronized BukkitTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period)
        throws IllegalArgumentException {
        if (taskId != -1) throw new IllegalStateException("Already scheduled as " + taskId);
        this.owner = plugin;
        this.sync = false;
        taskId = Bukkit.getScheduler()
            .scheduleAsyncRepeatingTask(plugin, this, delay, period);
        return this;
    }

    public int getTaskId() {
        return taskId;
    }
}
