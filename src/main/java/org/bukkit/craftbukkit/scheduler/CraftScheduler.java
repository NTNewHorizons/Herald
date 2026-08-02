package org.bukkit.craftbukkit.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;

/**
 * Scheduler backed by the server tick loop for synchronous tasks and a
 * dedicated executor for asynchronous tasks. Synchronous tasks are executed
 * from {@link #tick()}, which Herald invokes once per server tick on the main
 * thread, matching Bukkit's "run on the main thread" contract.
 */
public class CraftScheduler implements BukkitScheduler {

    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final List<CraftTask> pendingSyncTasks = new CopyOnWriteArrayList<>();
    private final List<CraftTask> allTasks = new CopyOnWriteArrayList<>();
    private final Map<Integer, CraftTask> tasksById = new ConcurrentHashMap<>();
    private final List<CraftWorker> activeWorkers = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService asyncExecutor = java.util.concurrent.Executors
        .newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "Herald - Async Scheduler");
                thread.setDaemon(true);
                return thread;
            }
        });

    private long currentTick;

    public void tick() {
        currentTick++;
        for (CraftTask task : pendingSyncTasks) {
            if (task.isCancelled()) {
                pendingSyncTasks.remove(task);
                continue;
            }
            if (currentTick >= task.getNextExecution()) {
                task.execute();
                if (!task.isCancelled() && task.getPeriod() > 0) {
                    task.setNextExecution(currentTick + task.getPeriod());
                } else {
                    pendingSyncTasks.remove(task);
                    tasksById.remove(task.getTaskId());
                }
            }
        }
    }

    public void shutdown() {
        cancelAllTasks();
        asyncExecutor.shutdownNow();
    }

    private void cancelAllTasks() {
        for (CraftTask task : allTasks) {
            task.cancel();
        }
    }

    @Override
    public int scheduleSyncDelayedTask(Plugin plugin, Runnable task, long delay) {
        CraftTask craftTask = new CraftTask(plugin, task, 0, true);
        craftTask.setNextExecution(currentTick + Math.max(0, delay));
        pendingSyncTasks.add(craftTask);
        register(craftTask);
        return craftTask.getTaskId();
    }

    @Override
    public int scheduleSyncDelayedTask(Plugin plugin, Runnable task) {
        return scheduleSyncDelayedTask(plugin, task, 0);
    }

    @Override
    public int scheduleSyncRepeatingTask(Plugin plugin, Runnable task, long delay, long period) {
        CraftTask craftTask = new CraftTask(plugin, task, period, true);
        craftTask.setNextExecution(currentTick + Math.max(0, delay));
        pendingSyncTasks.add(craftTask);
        register(craftTask);
        return craftTask.getTaskId();
    }

    @Override
    public int scheduleAsyncDelayedTask(Plugin plugin, Runnable task, long delay) {
        CraftTask craftTask = new CraftTask(plugin, task, 0, false);
        long delayMillis = Math.max(0, delay * 50);
        ScheduledFuture<?> future = asyncExecutor.schedule(craftTask::execute, delayMillis, TimeUnit.MILLISECONDS);
        craftTask.setAsyncFuture(future);
        register(craftTask);
        return craftTask.getTaskId();
    }

    @Override
    public int scheduleAsyncRepeatingTask(Plugin plugin, Runnable task, long delay, long period) {
        CraftTask craftTask = new CraftTask(plugin, task, period, false);
        long delayMillis = Math.max(0, delay * 50);
        long periodMillis = Math.max(1, period * 50);
        ScheduledFuture<?> future = asyncExecutor
            .scheduleWithFixedDelay(craftTask::execute, delayMillis, periodMillis, TimeUnit.MILLISECONDS);
        craftTask.setAsyncFuture(future);
        register(craftTask);
        return craftTask.getTaskId();
    }

    @Override
    public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable runnable = () -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        };
        CraftTask craftTask = new CraftTask(plugin, runnable, 0, true);
        craftTask.setNextExecution(currentTick + 1);
        pendingSyncTasks.add(craftTask);
        register(craftTask);
        return future;
    }

    private void register(CraftTask craftTask) {
        allTasks.add(craftTask);
        tasksById.put(craftTask.getTaskId(), craftTask);
    }

    @Override
    public void cancelTask(int taskId) {
        CraftTask task = tasksById.get(taskId);
        if (task != null) {
            task.cancel();
            pendingSyncTasks.remove(task);
            tasksById.remove(taskId);
        }
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        for (CraftTask task : allTasks) {
            if (task.getOwner() == plugin) {
                task.cancel();
                pendingSyncTasks.remove(task);
                tasksById.remove(task.getTaskId());
            }
        }
    }

    @Override
    public void runTask(Plugin plugin, Runnable task) {
        scheduleSyncDelayedTask(plugin, task, 0);
    }

    @Override
    public void runTaskAsynchronously(Plugin plugin, Runnable task) {
        scheduleAsyncDelayedTask(plugin, task, 0);
    }

    @Override
    public void runTaskLater(Plugin plugin, Runnable task, long delay) {
        scheduleSyncDelayedTask(plugin, task, delay);
    }

    @Override
    public void runTaskLaterAsynchronously(Plugin plugin, Runnable task, long delay) {
        scheduleAsyncDelayedTask(plugin, task, delay);
    }

    @Override
    public void runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        scheduleSyncRepeatingTask(plugin, task, delay, period);
    }

    @Override
    public void runTaskTimerAsynchronously(Plugin plugin, Runnable task, long delay, long period) {
        scheduleAsyncRepeatingTask(plugin, task, delay, period);
    }

    @Override
    public List<BukkitWorker> getActiveWorkers() {
        return new ArrayList<BukkitWorker>(activeWorkers);
    }

    @Override
    public List<BukkitTask> getPendingTasks() {
        List<BukkitTask> tasks = new ArrayList<>();
        for (CraftTask task : allTasks) {
            if (!task.isCancelled() && !task.isCurrentlyRunning()) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    @Override
    public boolean isCurrentlyRunning(int taskId) {
        CraftTask task = tasksById.get(taskId);
        return task != null && task.isCurrentlyRunning();
    }

    @Override
    public boolean isQueued(int taskId) {
        CraftTask task = tasksById.get(taskId);
        return task != null && !task.isCancelled() && !task.isCurrentlyRunning();
    }

    private class CraftWorker implements BukkitWorker {

        private final CraftTask task;
        private final Thread thread;

        CraftWorker(CraftTask task, Thread thread) {
            this.task = task;
            this.thread = thread;
        }

        @Override
        public Plugin getOwner() {
            return task.getOwner();
        }

        @Override
        public Thread getThread() {
            return thread;
        }

        @Override
        public int getTaskId() {
            return task.getTaskId();
        }
    }

    private class CraftTask implements BukkitTask {

        private final int taskId;
        private final Plugin plugin;
        private final Runnable task;
        private final long period;
        private final boolean sync;
        private volatile boolean cancelled;
        private volatile boolean running;
        private volatile long nextExecution;
        private volatile ScheduledFuture<?> asyncFuture;

        CraftTask(Plugin plugin, Runnable task, long period, boolean sync) {
            this.taskId = taskIdCounter.incrementAndGet();
            this.plugin = plugin;
            this.task = task;
            this.period = period;
            this.sync = sync;
        }

        @Override
        public int getTaskId() {
            return taskId;
        }

        @Override
        public Plugin getOwner() {
            return plugin;
        }

        @Override
        public boolean isSync() {
            return sync;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isCurrentlyRunning() {
            return running;
        }

        public long getNextExecution() {
            return nextExecution;
        }

        public void setNextExecution(long nextExecution) {
            this.nextExecution = nextExecution;
        }

        public long getPeriod() {
            return period;
        }

        public void setAsyncFuture(ScheduledFuture<?> asyncFuture) {
            this.asyncFuture = asyncFuture;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
            ScheduledFuture<?> future = this.asyncFuture;
            if (future != null) {
                future.cancel(false);
            }
        }

        public void execute() {
            if (cancelled) return;
            running = true;
            CraftWorker worker = null;
            if (!sync) {
                worker = new CraftWorker(this, Thread.currentThread());
                activeWorkers.add(worker);
            }
            try {
                task.run();
            } catch (Throwable t) {
                java.util.logging.Logger.getLogger("Herald")
                    .severe("Task " + taskId + " threw an exception: " + t);
                t.printStackTrace();
            } finally {
                running = false;
                if (worker != null) {
                    activeWorkers.remove(worker);
                }
            }
        }
    }
}
