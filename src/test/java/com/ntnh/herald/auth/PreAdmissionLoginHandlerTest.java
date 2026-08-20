package com.ntnh.herald.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.mojang.authlib.GameProfile;

class PreAdmissionLoginHandlerTest {

    private final List<PreAdmissionLoginHandler> handlers = new ArrayList<>();

    @AfterEach
    void closeHandlers() {
        for (PreAdmissionLoginHandler handler : handlers) handler.close();
    }

    @Test
    void workerConcurrencyNeverExceedsMaximum() throws Exception {
        CountDownLatch workersStarted = new CountDownLatch(2);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        AtomicBoolean daemonThreads = new AtomicBoolean(true);
        PreAdmissionLoginHandler handler = handler(2, 8, (username, uuid, address, socketAddress) -> {
            int current = active.incrementAndGet();
            maximumActive.accumulateAndGet(current, Math::max);
            daemonThreads.compareAndSet(
                true,
                Thread.currentThread()
                    .isDaemon());
            workersStarted.countDown();
            await(releaseWorkers);
            active.decrementAndGet();
            return LoginDecision.allow();
        });

        List<Future<LoginDecision>> decisions = new ArrayList<>();
        for (int i = 0; i < 6; i++) decisions.add(begin(handler, "player-" + i));
        assertTrue(workersStarted.await(5, TimeUnit.SECONDS));
        assertEquals(2, maximumActive.get());
        assertTrue(daemonThreads.get());

        releaseWorkers.countDown();
        for (Future<LoginDecision> decision : decisions) assertTrue(
            decision.get(5, TimeUnit.SECONDS)
                .isAllowed());
        assertEquals(2, maximumActive.get());
    }

    @Test
    void saturatedQueueFailsAdditionalLoginClosed() throws Exception {
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        PreAdmissionLoginHandler handler = handler(1, 1, (username, uuid, address, socketAddress) -> {
            workerStarted.countDown();
            await(releaseWorker);
            return LoginDecision.allow();
        });

        Future<LoginDecision> running = begin(handler, "running");
        assertTrue(workerStarted.await(5, TimeUnit.SECONDS));
        Future<LoginDecision> queued = begin(handler, "queued");
        LoginDecision rejected = begin(handler, "rejected").get(1, TimeUnit.SECONDS);

        assertFalse(rejected.isAllowed());
        assertEquals(PreAdmissionLoginHandler.BUSY_MESSAGE, rejected.getKickMessage());

        releaseWorker.countDown();
        assertTrue(
            running.get(5, TimeUnit.SECONDS)
                .isAllowed());
        assertTrue(
            queued.get(5, TimeUnit.SECONDS)
                .isAllowed());
    }

    @Test
    void cancellationInterruptsRunningWorkAndReleasesQueueCapacity() throws Exception {
        CountDownLatch runningStarted = new CountDownLatch(1);
        CountDownLatch runningInterrupted = new CountDownLatch(1);
        PreAdmissionLoginHandler handler = handler(1, 1, (username, uuid, address, socketAddress) -> {
            if ("running".equals(username)) {
                runningStarted.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException e) {
                    runningInterrupted.countDown();
                    Thread.currentThread()
                        .interrupt();
                }
            }
            return LoginDecision.allow();
        });

        Future<LoginDecision> running = begin(handler, "running");
        assertTrue(runningStarted.await(5, TimeUnit.SECONDS));
        Future<LoginDecision> queued = begin(handler, "queued");
        assertTrue(queued.cancel(true));

        Future<LoginDecision> replacement = begin(handler, "replacement");
        assertFalse(replacement.isDone());
        assertTrue(running.cancel(true));
        assertTrue(runningInterrupted.await(5, TimeUnit.SECONDS));
        assertTrue(
            replacement.get(5, TimeUnit.SECONDS)
                .isAllowed());
    }

    @Test
    void shutdownCancelsQueuedWorkAndTerminatesWorkers() throws Exception {
        CountDownLatch workerStarted = new CountDownLatch(1);
        PreAdmissionLoginHandler handler = handler(1, 2, (username, uuid, address, socketAddress) -> {
            workerStarted.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
            return LoginDecision.allow();
        });

        begin(handler, "running");
        assertTrue(workerStarted.await(5, TimeUnit.SECONDS));
        Future<LoginDecision> queued = begin(handler, "queued");

        handler.close();

        assertTrue(queued.isCancelled());
        assertTrue(handler.isTerminated());
    }

    private PreAdmissionLoginHandler handler(int workers, int queueCapacity,
        PreAdmissionLoginHandler.LoginAuthenticator authenticator) {
        PreAdmissionLoginHandler handler = new PreAdmissionLoginHandler(workers, queueCapacity, authenticator);
        handlers.add(handler);
        return handler;
    }

    private static Future<LoginDecision> begin(PreAdmissionLoginHandler handler, String username) {
        return handler.begin(new GameProfile(UUID.randomUUID(), username), new InetSocketAddress("192.0.2.100", 25565));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
        }
    }
}
