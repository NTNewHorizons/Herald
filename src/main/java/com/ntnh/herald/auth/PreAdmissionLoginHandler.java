package com.ntnh.herald.auth;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.authlib.GameProfile;
import com.ntnh.herald.HeraldDiscordSRV;

/** Runs potentially blocking DiscordSRV login checks away from Minecraft's server thread. */
public final class PreAdmissionLoginHandler implements AutoCloseable {

    private final ExecutorService executor = Executors.newCachedThreadPool(new LoginThreadFactory());

    public Future<LoginDecision> begin(GameProfile profile, SocketAddress remoteAddress) {
        String username = profile != null ? profile.getName() : null;
        java.util.UUID uuid = profile != null ? profile.getId() : null;
        InetSocketAddress socketAddress = remoteAddress instanceof InetSocketAddress ? (InetSocketAddress) remoteAddress
            : null;
        InetAddress address = socketAddress != null ? socketAddress.getAddress() : null;
        return executor.submit(
            () -> HeraldDiscordSRV.getInstance()
                .checkLoginBeforeAdmission(username, uuid, address, socketAddress));
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                com.ntnh.herald.Herald.LOG.warn("Timed out waiting for Herald pre-admission login checks to stop");
            }
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
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
