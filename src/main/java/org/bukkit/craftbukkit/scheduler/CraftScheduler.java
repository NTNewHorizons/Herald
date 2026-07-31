package org.bukkit.craftbukkit.scheduler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;

public class CraftScheduler implements BukkitScheduler {

    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final List<CraftTask> tasks = new CopyOnWriteArrayList<>();

    @Override
    public int scheduleSyncDelayedTask(Plugin plugin, Runnable task, long delay) {
        CraftTask craftTask = new CraftTask(task, delay, 0);
        tasks.add(craftTask);
        return craftTask.getTaskId();
    }

    @Override
    public int scheduleSyncDelayedTask(Plugin plugin, Runnable task) {
        return scheduleSyncDelayedTask(plugin, task, 0);
    }

    @Override
    public int scheduleSyncRepeatingTask(Plugin plugin, Runnable task, long delay, long period) {
        CraftTask craftTask = new CraftTask(task, delay, period);
        tasks.add(craftTask);
        return craftTask.getTaskId();
    }

    @Override
    public int scheduleAsyncDelayedTask(Plugin plugin, Runnable task, long delay) {
        return 0;
    }

    @Override
    public int scheduleAsyncRepeatingTask(Plugin plugin, Runnable task, long delay, long period) {
        return 0;
    }

    @Override
    public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task) {
        return null;
    }

    @Override
    public void cancelTask(int taskId) {
        tasks.removeIf(t -> t.getTaskId() == taskId);
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        tasks.removeIf(t -> t.getPlugin() == plugin);
    }

    @Override
    public void runTask(Plugin plugin, Runnable task) {
        new Thread(task).start();
    }

    @Override
    public void runTaskAsynchronously(Plugin plugin, Runnable task) {
        new Thread(task).start();
    }

    @Override
    public void runTaskLater(Plugin plugin, Runnable task, long delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay * 50);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
            task.run();
        }).start();
    }

    @Override
    public void runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay * 50);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
            task.run();
        }).start();
    }

    @Override
    public void runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        new Thread(() -> {
            try {
                Thread.sleep(delay * 50);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
            while (true) {
                task.run();
                try {
                    Thread.sleep(period * 50);
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                    break;
                }
            }
        }).start();
    }

    @Override
    public void runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period) {
        new Thread(() -> {
            try {
                Thread.sleep(delay * 50);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
            while (true) {
                task.run();
                try {
                    Thread.sleep(period * 50);
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                    break;
                }
            }
        }).start();
    }

    @Override
    public List<BukkitWorker> getActiveWorkers() {
        return Collections.emptyList();
    }

    @Override
    public List<BukkitTask> getPendingTasks() {
        return Collections.emptyList();
    }

    @Override
    public boolean isCurrentlyRunning(int taskId) {
        return false;
    }

    @Override
    public boolean isQueued(int taskId) {
        return false;
    }

    private class CraftTask implements BukkitTask {

        private final int taskId;
        private final Runnable task;
        private final long delay;
        private final long period;
        private boolean cancelled;

        CraftTask(Runnable task, long delay, long period) {
            this.taskId = taskIdCounter.incrementAndGet();
            this.task = task;
            this.delay = delay;
            this.period = period;
        }

        @Override
        public int getTaskId() {
            return taskId;
        }

        @Override
        public Plugin getOwner() {
            return null;
        }

        @Override
        public boolean isSync() {
            return false;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }

        public Runnable getTask() {
            return task;
        }

        public long getDelay() {
            return delay;
        }

        public long getPeriod() {
            return period;
        }

        public Plugin getPlugin() {
            return null;
        }
    }
}
