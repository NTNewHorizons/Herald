package com.ntnh.herald.auth;

import org.apache.logging.log4j.Logger;

/** Explicit lifecycle for the components that enforce authentication before player admission. */
public final class AuthenticationReadiness {

    public enum State {
        LOADING,
        READY,
        UNAVAILABLE,
        FAILED
    }

    private final Logger logger;
    private volatile State state = State.LOADING;
    private volatile String failureReason;
    private volatile String unavailableReason;

    public AuthenticationReadiness(Logger logger) {
        this.logger = logger;
    }

    public State getState() {
        return state;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public synchronized void markReady() {
        if (state == State.FAILED || state == State.READY) return;
        State previous = state;
        state = State.READY;
        unavailableReason = null;
        if (previous == State.UNAVAILABLE) {
            logger.info("Herald authentication stack is available again");
        } else {
            logger.info("Herald authentication stack is ready");
        }
    }

    public synchronized void markUnavailable(String reason) {
        if (state == State.FAILED) return;
        unavailableReason = reason;
        if (state != State.UNAVAILABLE) {
            state = State.UNAVAILABLE;
            logger.warn("Herald authentication stack is temporarily unavailable: " + reason);
        }
    }

    public synchronized void markFailed(String reason) {
        if (state == State.FAILED) return;
        failureReason = reason;
        unavailableReason = null;
        state = State.FAILED;
        logger.error("Herald authentication stack failed: " + reason);
    }

    /** Re-evaluates live dependencies after initialization without allowing recovery from permanent failure. */
    public State refreshAvailability(boolean requiredComponentsAvailable, boolean discordConnected,
        boolean permanentDiscordFailure, String reason) {
        State current = state;
        if (current == State.LOADING || current == State.FAILED) return current;
        if (!requiredComponentsAvailable || permanentDiscordFailure) {
            markFailed(reason);
        } else if (discordConnected) {
            markReady();
        } else {
            markUnavailable(reason);
        }
        return state;
    }
}
