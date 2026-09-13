package com.ntnh.herald.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

class IpAuthVerificationTest {

    @TempDir
    Path temporaryDirectory;

    private final List<IpAuthManager> managers = new ArrayList<>();

    @AfterEach
    void closeManagers() throws Exception {
        for (IpAuthManager manager : managers) manager.close();
    }

    @Test
    void validVerificationAuthorizesIpRemovesChallengeAndWritesDiagnostics() throws Exception {
        Path auditFile = temporaryDirectory.resolve("audit.log");
        Fixture fixture = fixture(temporaryDirectory.resolve("trusted.tsv"), auditFile, 4821);
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.10");
        fixture.links.put(uuid, "discord-a");

        IpAuthManager.LoginResult login = fixture.manager.checkLogin("Alice", uuid, address);
        assertEquals("4821", login.getCode());
        assertEquals(
            "IP authorized for Alice. Reconnect to the Minecraft server.",
            fixture.manager.handleDiscordMessage("v4821", "discord-a"));

        assertTrue(fixture.isTrusted(uuid, address));
        assertEquals(
            0,
            fixture.manager.getStatus(uuid)
                .getPendingChallenges());
        String audit = new String(Files.readAllBytes(auditFile), StandardCharsets.UTF_8);
        assertTrue(audit.contains("event=verification_received"));
        assertTrue(audit.contains("event=verification_challenge_found"));
        assertTrue(audit.contains("event=ip_authorized"));
        assertTrue(audit.contains("code=\"v4821\""));
    }

    @Test
    void validCommandWithNoChallengeIsRejected() throws Exception {
        Fixture fixture = fixture(temporaryDirectory.resolve("trusted.tsv"), temporaryDirectory.resolve("audit.log"));

        assertEquals(
            "That IP verification challenge is invalid or expired.",
            fixture.manager.handleDiscordMessage("v4821", "discord-a"));
    }

    @Test
    void wrongLinkedDiscordSenderCannotAuthorize() throws Exception {
        Fixture fixture = fixture(
            temporaryDirectory.resolve("trusted.tsv"),
            temporaryDirectory.resolve("audit.log"),
            4821);
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.20");
        fixture.links.put(uuid, "discord-a");
        fixture.manager.checkLogin("Alice", uuid, address);

        assertEquals(
            "That IP verification challenge is invalid or is not linked to your Discord account.",
            fixture.manager.handleDiscordMessage("v4821", "discord-b"));
        assertFalse(fixture.isTrusted(uuid, address));
        assertEquals(
            1,
            fixture.manager.getStatus(uuid)
                .getPendingChallenges());
    }

    @Test
    void expiredChallengeCannotAuthorize() throws Exception {
        Fixture fixture = fixture(
            temporaryDirectory.resolve("trusted.tsv"),
            temporaryDirectory.resolve("audit.log"),
            4821);
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.30");
        fixture.links.put(uuid, "discord-a");
        fixture.manager.checkLogin("Alice", uuid, address);
        fixture.clock.advanceMillis(5_000);

        assertEquals(
            "That IP verification challenge is invalid or expired.",
            fixture.manager.handleDiscordMessage("v4821", "discord-a"));
        assertFalse(fixture.isTrusted(uuid, address));
        assertEquals(
            0,
            fixture.manager.getStatus(uuid)
                .getPendingChallenges());
    }

    @Test
    void persistenceFailureLeavesChallengeAvailableForRetry() throws Exception {
        Path blocker = temporaryDirectory.resolve("not-a-directory");
        Files.write(blocker, new byte[] { 1 });
        Fixture fixture = fixture(blocker.resolve("trusted.tsv"), temporaryDirectory.resolve("audit.log"), 4821);
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.40");
        fixture.links.put(uuid, "discord-a");
        fixture.manager.checkLogin("Alice", uuid, address);

        assertEquals(
            "The IP could not be saved. Please try the same verification code again.",
            fixture.manager.handleDiscordMessage("v4821", "discord-a"));
        assertFalse(fixture.isTrusted(uuid, address));
        assertEquals(
            1,
            fixture.manager.getStatus(uuid)
                .getPendingChallenges());
        String audit = new String(Files.readAllBytes(temporaryDirectory.resolve("audit.log")), StandardCharsets.UTF_8);
        assertTrue(audit.contains("event=verification_persist_failed"));
    }

    @Test
    void malformedInputDoesNotEnterVerificationHandling() throws Exception {
        Fixture fixture = fixture(
            temporaryDirectory.resolve("trusted.tsv"),
            temporaryDirectory.resolve("audit.log"),
            4821);
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.50");
        fixture.links.put(uuid, "discord-a");
        fixture.manager.checkLogin("Alice", uuid, address);

        assertFalse(fixture.manager.recognizesVerificationMessage("v482"));
        assertFalse(fixture.manager.recognizesVerificationMessage("x4821"));
        assertNull(fixture.manager.handleDiscordMessage("v482", "discord-a"));
        assertFalse(fixture.isTrusted(uuid, address));
        assertEquals(
            1,
            fixture.manager.getStatus(uuid)
                .getPendingChallenges());
        String audit = new String(Files.readAllBytes(temporaryDirectory.resolve("audit.log")), StandardCharsets.UTF_8);
        assertFalse(audit.contains("event=verification_received"));
    }

    @Test
    void challengeIsReusedUntilExpiryThenReplaced() throws Exception {
        Fixture fixture = fixture(
            temporaryDirectory.resolve("trusted.tsv"),
            temporaryDirectory.resolve("audit.log"),
            4821,
            5932);
        UUID uuid = UUID.randomUUID();
        InetAddress address = InetAddress.getByName("192.0.2.60");
        fixture.links.put(uuid, "discord-a");

        IpAuthManager.LoginResult created = fixture.manager.checkLogin("Alice", uuid, address);
        IpAuthManager.LoginResult reused = fixture.manager.checkLogin("Alice", uuid, address);
        fixture.clock.advanceMillis(5_000);
        IpAuthManager.LoginResult replaced = fixture.manager.checkLogin("Alice", uuid, address);

        assertTrue(created.isChallengeCreated());
        assertEquals("4821", created.getCode());
        assertFalse(reused.isChallengeCreated());
        assertEquals(created.getCode(), reused.getCode());
        assertTrue(replaced.isChallengeCreated());
        assertEquals("5932", replaced.getCode());
    }

    private Fixture fixture(Path storeFile, Path auditFile, int... codes) throws Exception {
        MutableClock clock = new MutableClock(1_000_000L);
        Map<UUID, String> links = new HashMap<>();
        IpAuthStore store = new IpAuthStore(storeFile);
        IpAuthManager manager = new IpAuthManager(
            new IpAuthSettings(true, 0, 4, 5, true, false, 30),
            store,
            new IpAuthAuditLogger(auditFile, true),
            links::get,
            (discordId, message) -> {},
            clock,
            new SequenceSecureRandom(codes));
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

    private static final class SequenceSecureRandom extends SecureRandom {

        private final int[] values;
        private int index;

        private SequenceSecureRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            if (values.length == 0) return 0;
            return values[Math.min(index++, values.length - 1)] % bound;
        }
    }
}
