package com.ntnh.herald.auth;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.authlib.GameProfile;
import com.ntnh.herald.HeraldDiscordSRV;

/** Runs potentially blocking DiscordSRV login checks away from Minecraft's server thread. */
public final class PreAdmissionLoginHandler implements AutoCloseable {

    static final int MAX_AUTHENTICATION_WORKERS = 8;
    static final int AUTHENTICATION_QUEUE_CAPACITY = 64;
    static final String BUSY_MESSAGE = "Herald authentication is busy. Please try again shortly.";

    private final AuthenticationThreadPool executor;
    private final LoginAuthenticator authenticator;

    public PreAdmissionLoginHandler() {
        this(
            MAX_AUTHENTICATION_WORKERS,
            AUTHENTICATION_QUEUE_CAPACITY,
            (username, uuid, address, socketAddress) -> HeraldDiscordSRV.getInstance()
                .checkLoginBeforeAdmission(username, uuid, address, socketAddress));
    }

    PreAdmissionLoginHandler(int maximumWorkers, int queueCapacity, LoginAuthenticator authenticator) {
        if (maximumWorkers < 1) throw new IllegalArgumentException("maximumWorkers must be positive");
        if (queueCapacity < 1) throw new IllegalArgumentException("queueCapacity must be positive");
        this.authenticator = authenticator;
        this.executor = new AuthenticationThreadPool(maximumWorkers, queueCapacity);
    }

    public Future<LoginDecision> begin(GameProfile profile, SocketAddress remoteAddress) {
        String username = profile != null ? profile.getName() : null;
        UUID uuid = profile != null ? profile.getId() : null;
        InetSocketAddress socketAddress = remoteAddress instanceof InetSocketAddress ? (InetSocketAddress) remoteAddress
            : null;
        InetAddress address = socketAddress != null ? socketAddress.getAddress() : null;
        try {
            return executor.submit(() -> authenticator.authenticate(username, uuid, address, socketAddress));
        } catch (RejectedExecutionException ignored) {
            return CompletableFuture.completedFuture(LoginDecision.reject(BUSY_MESSAGE));
        }
    }

    @Override
    public void close() {
        List<Runnable> abandoned = executor.shutdownNow();
        for (Runnable task : abandoned) {
            if (task instanceof Future<?>) ((Future<?>) task).cancel(false);
        }
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                com.ntnh.herald.Herald.LOG.warn("Timed out waiting for Herald pre-admission login checks to stop");
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        }
    }

    boolean isTerminated() {
        return executor.isTerminated();
    }

    interface LoginAuthenticator {

        LoginDecision authenticate(String username, UUID uuid, InetAddress address, InetSocketAddress socketAddress);
    }

    private static final class AuthenticationThreadPool extends ThreadPoolExecutor {

        private AuthenticationThreadPool(int maximumWorkers, int queueCapacity) {
            super(
                maximumWorkers,
                maximumWorkers,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new LoginThreadFactory(),
                new AbortPolicy());
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            return new FutureTask<T>(callable) {

                @Override
                protected void done() {
                    if (isCancelled()) AuthenticationThreadPool.this.remove(this);
                }
            };
        }
    }

    private static final class LoginThreadFactory implements ThreadFactory {

        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "Herald Login Authentication - " + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
