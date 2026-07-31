package org.bukkit.scheduler;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.bukkit.plugin.Plugin;

public interface BukkitScheduler {

    int scheduleSyncDelayedTask(Plugin plugin, Runnable task, long delay);

    int scheduleSyncDelayedTask(Plugin plugin, Runnable task);

    int scheduleSyncRepeatingTask(Plugin plugin, Runnable task, long delay, long period);

    int scheduleAsyncDelayedTask(Plugin plugin, Runnable task, long delay);

    int scheduleAsyncRepeatingTask(Plugin plugin, Runnable task, long delay, long period);

    <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task);

    void cancelTask(int taskId);

    void cancelTasks(Plugin plugin);

    void runTask(Plugin plugin, Runnable task);

    void runTaskAsynchronously(Plugin plugin, Runnable task);

    void runTaskLater(Plugin plugin, Runnable task, long delay);

    void runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay);

    void runTaskTimer(Plugin plugin, Runnable task, long delay, long period);

    void runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period);

    List<BukkitWorker> getActiveWorkers();

    List<BukkitTask> getPendingTasks();

    boolean isCurrentlyRunning(int taskId);

    boolean isQueued(int taskId);
}
