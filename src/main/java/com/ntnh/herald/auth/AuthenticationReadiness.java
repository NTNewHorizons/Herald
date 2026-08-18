package com.ntnh.herald.auth;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Logger;

/** Explicit lifecycle for the components that enforce authentication before player admission. */
public final class AuthenticationReadiness {

    public enum State {
        LOADING,
        READY,
        FAILED
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.LOADING);
    private final Logger logger;
    private volatile String failureReason;

    public AuthenticationReadiness(Logger logger) {
        this.logger = logger;
    }

    public State getState() {
        return state.get();
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void markReady() {
        if (state.compareAndSet(State.LOADING, State.READY)) {
            logger.info("Herald authentication stack is ready");
        }
    }

    public void markFailed(String reason) {
        failureReason = reason;
        State previous = state.getAndSet(State.FAILED);
        if (previous != State.FAILED) {
            logger.error("Herald authentication stack failed: " + reason);
        }
    }
}
