package com.ntnh.herald.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

class AuthenticationReadinessTest {

    @Test
    void startsLoadingAndSuccessfulInitializationBecomesReady() {
        AuthenticationReadiness readiness = readiness();

        assertEquals(AuthenticationReadiness.State.LOADING, readiness.getState());
        readiness.markReady();
        assertEquals(AuthenticationReadiness.State.READY, readiness.getState());
    }

    @Test
    void permanentInitializationFailureIsSticky() {
        AuthenticationReadiness readiness = readiness();

        readiness.markFailed("invalid configuration");
        readiness.markReady();

        assertEquals(AuthenticationReadiness.State.FAILED, readiness.getState());
        assertEquals("invalid configuration", readiness.getFailureReason());
    }

    @Test
    void temporaryDiscordDisconnectRecoversToReady() {
        AuthenticationReadiness readiness = readiness();
        readiness.markReady();

        assertEquals(
            AuthenticationReadiness.State.UNAVAILABLE,
            readiness.refreshAvailability(true, false, false, "Discord is reconnecting"));
        assertEquals("Discord is reconnecting", readiness.getUnavailableReason());
        assertEquals(
            AuthenticationReadiness.State.READY,
            readiness.refreshAvailability(true, true, false, "Discord is connected"));
    }

    @Test
    void terminalDiscordFailureFailsClosed() {
        AuthenticationReadiness readiness = readiness();
        readiness.markReady();

        assertEquals(
            AuthenticationReadiness.State.FAILED,
            readiness.refreshAvailability(true, false, true, "JDA shut down"));
        readiness.refreshAvailability(true, true, false, "connected again");
        assertEquals(AuthenticationReadiness.State.FAILED, readiness.getState());
    }

    @Test
    void lossOfAccountLinkManagerOrRequiredIpAuthManagerFailsClosed() {
        AuthenticationReadiness accountLinksMissing = readiness();
        accountLinksMissing.markReady();
        assertEquals(
            AuthenticationReadiness.State.FAILED,
            accountLinksMissing.refreshAvailability(false, true, false, "account links missing"));

        AuthenticationReadiness ipAuthMissing = readiness();
        ipAuthMissing.markReady();
        assertEquals(
            AuthenticationReadiness.State.FAILED,
            ipAuthMissing.refreshAvailability(false, true, false, "IP auth missing"));
    }

    private static AuthenticationReadiness readiness() {
        return new AuthenticationReadiness(LogManager.getLogger(AuthenticationReadinessTest.class));
    }
}
