package com.ntnh.herald.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IpAuthInitialEnrollmentTest {

    @TempDir
    Path temporaryDirectory;

    private final List<IpAuthManager> managers = new ArrayList<>();

    @AfterEach
    void closeManagers() throws Exception {
        for (IpAuthManager manager : managers) manager.close();
    }

    @Test
    void consumedLinkingCodeEnrollsItsExactIp() throws Exception {
        Fixture fixture = fixture();
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.10");
        fixture.links.put(uuid, "discord-a");

        assertTrue(fixture.manager.rememberInitialLinkAttempt("1234", "Alice", uuid, address));
        assertTrue(fixture.manager.completeInitialLinkEnrollment("1234", uuid, "discord-a"));
        assertTrue(fixture.isTrusted(uuid, address));
    }

    @Test
    void twoCodesForOneUuidRemainBoundToTheirOwnIps() throws Exception {
        Fixture fixture = fixture();
        UUID uuid = UUID.randomUUID();
        InetAddress addressA = InetAddress.getByName("192.0.2.20");
        InetAddress addressB = InetAddress.getByName("192.0.2.21");
        fixture.links.put(uuid, "discord-a");

        assertTrue(fixture.manager.rememberInitialLinkAttempt("1111", "Alice", uuid, addressA));
        assertTrue(fixture.manager.rememberInitialLinkAttempt("2222", "Alice", uuid, addressB));
        assertTrue(fixture.manager.completeInitialLinkEnrollment("2222", uuid, "discord-a"));

        assertFalse(fixture.isTrusted(uuid, addressA));
        assertTrue(fixture.isTrusted(uuid, addressB));
        assertFalse(fixture.manager.completeInitialLinkEnrollment("1111", uuid, "discord-a"));
    }

    @Test
    void expiredEnrollmentCannotAuthorize() throws Exception {
        Fixture fixture = fixture();
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("2001:db8::10");
        fixture.links.put(uuid, "discord-a");

        assertTrue(fixture.manager.rememberInitialLinkAttempt("1234", "Alice", uuid, address));
        fixture.clock.advanceMillis(5_000);

        assertFalse(fixture.manager.completeInitialLinkEnrollment("1234", uuid, "discord-a"));
        assertFalse(fixture.isTrusted(uuid, address));
    }

    @Test
    void wrongDiscordAccountCannotAuthorize() throws Exception {
        Fixture fixture = fixture();
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.30");
        fixture.links.put(uuid, "discord-a");

        assertTrue(fixture.manager.rememberInitialLinkAttempt("1234", "Alice", uuid, address));
        assertFalse(fixture.manager.completeInitialLinkEnrollment("1234", uuid, "discord-b"));
        assertFalse(fixture.isTrusted(uuid, address));
    }

    @Test
    void unlinkingOrRelinkingInvalidatesStaleAuthorization() throws Exception {
        Fixture fixture = fixture();
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.40");
        fixture.links.put(uuid, "discord-a");
        assertTrue(fixture.manager.rememberInitialLinkAttempt("1234", "Alice", uuid, address));

        fixture.links.remove(uuid);
        assertFalse(fixture.manager.completeInitialLinkEnrollment("1234", uuid, "discord-a"));
        fixture.links.put(uuid, "discord-b");
        assertFalse(fixture.manager.completeInitialLinkEnrollment("1234", uuid, "discord-a"));
        assertFalse(fixture.isTrusted(uuid, address));
    }

    @Test
    void existingTrustedRecordCannotBeExpandedByInitialLinkShortcut() throws Exception {
        Fixture fixture = fixture();
        UUID uuid = UUID.randomUUID();
        InetAddress pendingAddress = InetAddress.getByName("192.0.2.50");
        InetAddress trustedAddress = InetAddress.getByName("192.0.2.51");
        fixture.links.put(uuid, "discord-a");
        assertTrue(fixture.manager.rememberInitialLinkAttempt("1234", "Alice", uuid, pendingAddress));

        fixture.store.authorize(uuid, IpAddress.from(trustedAddress), fixture.clock.getAsLong(), 0);

        assertFalse(fixture.manager.completeInitialLinkEnrollment("1234", uuid, "discord-a"));
        assertFalse(fixture.isTrusted(uuid, pendingAddress));
        assertTrue(fixture.isTrusted(uuid, trustedAddress));
    }

    private Fixture fixture() throws Exception {
        MutableClock clock = new MutableClock(1_000_000L);
        Map<UUID, String> links = new HashMap<>();
        IpAuthStore store = new IpAuthStore(temporaryDirectory.resolve(UUID.randomUUID() + "-trusted.tsv"));
        IpAuthManager manager = new IpAuthManager(
            new IpAuthSettings(true, 0, 4, 5, true, false, 30),
            store,
            new IpAuthAuditLogger(temporaryDirectory.resolve(UUID.randomUUID() + "-audit.log"), false),
            links::get,
            (discordId, message) -> {},
            clock,
            new SecureRandom());
        managers.add(manager);
        return new Fixture(manager, store, links, clock);
    }

    private static final class Fixture {

        private final IpAuthManager manager;
        private final IpAuthStore store;
        private final Map<UUID, String> links;
        private final MutableClock clock;

        private Fixture(IpAuthManager manager, IpAuthStore store, Map<UUID, String> links, MutableClock clock) {
            this.manager = manager;
            this.store = store;
            this.links = links;
            this.clock = clock;
        }

        private boolean isTrusted(UUID uuid, InetAddress address) {
            return store.isTrusted(uuid, IpAddress.from(address));
        }
    }

    private static final class MutableClock implements LongSupplier {

        private long now;

        private MutableClock(long now) {
            this.now = now;
        }

        @Override
        public long getAsLong() {
            return now;
        }

        private void advanceMillis(long millis) {
            now += millis;
        }
    }
}
