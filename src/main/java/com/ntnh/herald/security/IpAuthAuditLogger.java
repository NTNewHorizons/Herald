package com.ntnh.herald.security;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Dedicated security-event audit stream. Normal trusted-IP logins are intentionally omitted. */
public final class IpAuthAuditLogger implements Closeable {

    private static final Logger LOG = LogManager.getLogger("HeraldIpAuthAudit");

    private final boolean enabled;
    private final BufferedWriter writer;

    public IpAuthAuditLogger(Path file, boolean enabled) throws IOException {
        this.enabled = enabled;
        if (enabled) {
            Path parent = file.toAbsolutePath()
                .getParent();
            if (parent != null) Files.createDirectories(parent);
            this.writer = Files
                .newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            this.writer = null;
        }
    }

    public synchronized void failedIpCheck(long timestamp, String username, UUID uuid, String ip, String discordId,
        boolean initialEnrollment, boolean challengeCreated) {
        write(
            timestamp,
            "failed_ip_check",
            field("username", username) + field("uuid", uuid)
                + field("ip", ip)
                + field("discord_id", discordId)
                + field("enrollment", initialEnrollment ? "initial" : "ip_change")
                + field("challenge", challengeCreated ? "created" : "reused"));
    }

    public synchronized void successfulAuthorization(long timestamp, String username, UUID uuid, String ip,
        String discordId, String evictedIp) {
        write(
            timestamp,
            "ip_authorized",
            field("username", username) + field("uuid", uuid)
                + field("ip", ip)
                + field("discord_id", discordId)
                + field("evicted", evictedIp != null ? "true" : "false")
                + field("evicted_ip", evictedIp));
    }

    private void write(long timestamp, String event, String fields) {
        if (!enabled) return;
        try {
            writer.write("timestamp=");
            writer.write(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timestamp)));
            writer.write(" event=");
            writer.write(event);
            writer.write(fields);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            LOG.error("Could not write Herald IP-auth audit event " + event, e);
        }
    }

    private static String field(String name, Object value) {
        String text = value == null ? "<none>" : value.toString();
        text = text.replace("\\", "\\\\")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\"", "\\\"");
        return " " + name + "=\"" + text + "\"";
    }

    @Override
    public synchronized void close() throws IOException {
        if (writer != null) writer.close();
    }
}
