package com.ntnh.herald.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IpAuthManagerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void repeatedAttemptsReuseChallengeAndRateLimitDms() throws Exception {
        MutableClock clock = new MutableClock(1_000L);
        Map<UUID, String> links = new HashMap<>();
        List<String> dms = new ArrayList<>();
        UUID uuid = UUID.randomUUID();
        links.put(uuid, "discord-a");

        try (IpAuthManager manager = manager(settings(true, 1, true, true), links, dms, clock)) {
            InetAddress address = InetAddress.getByName("192.0.2.10");
            IpAuthManager.LoginResult first = manager.checkLogin("Player", uuid, address);
            IpAuthManager.LoginResult repeated = manager.checkLogin("Player", uuid, address);

            assertFalse(first.isAllowed());
            assertTrue(first.isChallengeCreated());
            assertTrue(first.isDmAttempted());
            assertEquals(first.getCode(), repeated.getCode());
            assertFalse(repeated.isChallengeCreated());
            assertFalse(repeated.isDmAttempted());
            assertEquals(1, dms.size());
            assertTrue(
                dms.get(0)
                    .contains("v" + first.getCode()));

            clock.advance(30_000L);
            IpAuthManager.LoginResult afterCooldown = manager.checkLogin("Player", uuid, address);
            assertEquals(first.getCode(), afterCooldown.getCode());
            assertTrue(afterCooldown.isDmAttempted());
            assertEquals(2, dms.size());
        }
    }

    @Test
    void onlyCurrentlyLinkedDiscordAccountCanConsumeExactIpChallenge() throws Exception {
        MutableClock clock = new MutableClock(10_000L);
        Map<UUID, String> links = new HashMap<>();
        UUID uuid = UUID.randomUUID();
        links.put(uuid, "discord-a");
        IpAuthStore store = store("links.tsv");

        try (IpAuthManager manager = manager(settings(true, 1, true, false), store, links, new ArrayList<>(), clock)) {
            InetAddress challengedAddress = InetAddress.getByName("198.51.100.25");
            InetAddress otherAddress = InetAddress.getByName("198.51.100.26");
            String code = manager.checkLogin("RelinkedPlayer", uuid, challengedAddress)
                .getCode();

            assertNotNull(manager.handleDiscordMessage("v" + code, "discord-b"));
            assertFalse(store.isTrusted(uuid, IpAddress.from(challengedAddress)));

            links.put(uuid, "discord-b");
            assertNotNull(manager.handleDiscordMessage("V" + code, "discord-a"));
            assertFalse(store.isTrusted(uuid, IpAddress.from(challengedAddress)));

            String success = manager.handleDiscordMessage("V" + code, "discord-b");
            assertTrue(success.contains("IP authorized"));
            assertTrue(store.isTrusted(uuid, IpAddress.from(challengedAddress)));
            assertFalse(store.isTrusted(uuid, IpAddress.from(otherAddress)));
            assertTrue(
                manager.handleDiscordMessage("v" + code, "discord-b")
                    .contains("invalid or expired"));
        }
    }

    @Test
    void unlinkAndExpiryPreventAuthorizationAndSyntaxDoesNotFallThrough() throws Exception {
        MutableClock clock = new MutableClock(50_000L);
        Map<UUID, String> links = new HashMap<>();
        UUID uuid = UUID.randomUUID();
        links.put(uuid, "discord-a");

        try (IpAuthManager manager = manager(settings(true, 1, true, false), links, new ArrayList<>(), clock)) {
            IpAuthManager.LoginResult login = manager.checkLogin("Player", uuid, InetAddress.getByName("203.0.113.4"));
            links.remove(uuid);
            assertTrue(
                manager.handleDiscordMessage("v" + login.getCode(), "discord-a")
                    .contains("not linked"));

            links.put(uuid, "discord-a");
            clock.advance(300_000L);
            assertTrue(
                manager.handleDiscordMessage("v" + login.getCode(), "discord-a")
                    .contains("invalid or expired"));
            assertNull(manager.handleDiscordMessage(login.getCode(), "discord-a"));
            assertNull(manager.handleDiscordMessage("verify " + login.getCode(), "discord-a"));
            assertNull(manager.handleDiscordMessage("v12x4", "discord-a"));
            assertTrue(manager.recognizesVerificationMessage(" V0000 "));
        }
    }

    @Test
    void disabledModeAndOptionalInitialEnrollmentAllowLogin() throws Exception {
        UUID disabledUuid = UUID.randomUUID();
        try (IpAuthManager disabled = manager(
            settings(false, 1, true, true),
            new HashMap<>(),
            new ArrayList<>(),
            new MutableClock(1L))) {
            assertTrue(
                disabled.checkLogin("Player", disabledUuid, null)
                    .isAllowed());
        }

        UUID enrolledUuid = UUID.randomUUID();
        IpAuthStore store = store("enrollment.tsv");
        InetAddress firstAddress = InetAddress.getByName("2001:db8::10");
        try (IpAuthManager manager = manager(
            settings(true, 1, false, true),
            store,
            new HashMap<>(),
            new ArrayList<>(),
            new MutableClock(2L))) {
            assertTrue(
                manager.checkLogin("Player", enrolledUuid, firstAddress)
                    .isAllowed());
            assertTrue(store.isTrusted(enrolledUuid, IpAddress.from(firstAddress)));
        }
    }

    @Test
    void trustedAddressesAreUuidScoped() throws Exception {
        IpAuthStore store = store("nat.tsv");
        IpAddress sharedAddress = IpAddress.parse("192.0.2.44");
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        store.authorize(first, sharedAddress, 1L, 1);

        assertTrue(store.isTrusted(first, sharedAddress));
        assertFalse(store.isTrusted(second, sharedAddress));
    }

    private IpAuthManager manager(IpAuthSettings settings, Map<UUID, String> links, List<String> dms,
        MutableClock clock) throws Exception {
        return manager(settings, store(UUID.randomUUID() + ".tsv"), links, dms, clock);
    }

    private IpAuthManager manager(IpAuthSettings settings, IpAuthStore store, Map<UUID, String> links, List<String> dms,
        MutableClock clock) throws Exception {
        return new IpAuthManager(
            settings,
            store,
            new IpAuthAuditLogger(temporaryDirectory.resolve(UUID.randomUUID() + ".log"), false),
            links::get,
            (discordId, message) -> dms.add(discordId + ":" + message),
            clock,
            new SecureRandom());
    }

    private IpAuthStore store(String name) throws Exception {
        return new IpAuthStore(temporaryDirectory.resolve(name));
    }

    private static IpAuthSettings settings(boolean enabled, int maxTrustedIps, boolean requireInitial,
        boolean dmOnNewIp) {
        return new IpAuthSettings(enabled, maxTrustedIps, 4, 300, requireInitial, dmOnNewIp, 30);
    }

    private static final class MutableClock implements LongSupplier {

        private long time;

        private MutableClock(long time) {
            this.time = time;
        }

        @Override
        public long getAsLong() {
            return time;
        }

        void advance(long amount) {
            time += amount;
        }
    }
}
