package com.ntnh.herald.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IpAuthAuditLoggerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void recordsOnlySecurityMetadataWithoutVerificationCodes() throws Exception {
        Path file = temporaryDirectory.resolve("ip-auth-audit.log");
        UUID uuid = UUID.randomUUID();
        try (IpAuthAuditLogger logger = new IpAuthAuditLogger(file, true)) {
            logger.failedIpCheck(1_000L, "Player", uuid, "192.0.2.9", "discord-a", true, true);
            logger.successfulAuthorization(2_000L, "Player", uuid, "192.0.2.9", "discord-a", "192.0.2.8");
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertTrue(
            lines.get(0)
                .contains("event=failed_ip_check"));
        assertTrue(
            lines.get(0)
                .contains("enrollment=\"initial\""));
        assertTrue(
            lines.get(0)
                .contains("challenge=\"created\""));
        assertTrue(
            lines.get(1)
                .contains("event=ip_authorized"));
        assertTrue(
            lines.get(1)
                .contains("evicted=\"true\""));
        assertFalse(
            String.join("\n", lines)
                .contains("code"));
    }
}
