package com.ntnh.herald.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IpAuthStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void evictsLeastRecentlyUsedAndPersistsIpv4AndIpv6() throws Exception {
        Path file = temporaryDirectory.resolve("trusted.tsv");
        UUID uuid = UUID.randomUUID();
        IpAddress first = IpAddress.parse("192.0.2.1");
        IpAddress second = IpAddress.parse("2001:db8::2");
        IpAddress third = IpAddress.parse("203.0.113.3");
        IpAuthStore store = new IpAuthStore(file);

        store.authorize(uuid, first, 10L, 2);
        store.authorize(uuid, second, 20L, 2);
        store.touch(uuid, first, 30L);
        IpAuthStore.Authorization authorization = store.authorize(uuid, third, 40L, 2);

        assertEquals(second, authorization.getEvictedAddress());
        assertTrue(store.isTrusted(uuid, first));
        assertFalse(store.isTrusted(uuid, second));
        assertTrue(store.isTrusted(uuid, third));

        IpAuthStore reloaded = new IpAuthStore(file);
        assertTrue(reloaded.isTrusted(uuid, first));
        assertTrue(reloaded.isTrusted(uuid, third));
        assertEquals(1, reloaded.pruneToLimit(1));
        assertFalse(reloaded.isTrusted(uuid, first));
        assertTrue(reloaded.isTrusted(uuid, third));
    }

    @Test
    void zeroLimitIsUnlimitedAndResetRemovesCollection() throws Exception {
        IpAuthStore store = new IpAuthStore(temporaryDirectory.resolve("unlimited.tsv"));
        UUID uuid = UUID.randomUUID();
        for (int i = 1; i <= 4; i++) store.authorize(uuid, IpAddress.parse("198.51.100." + i), i, 0);

        List<TrustedIp> trustedIps = store.getTrustedIps(uuid);
        assertEquals(4, trustedIps.size());
        store.reset(uuid);
        assertFalse(store.hasTrustedIps(uuid));
    }
}
