package com.ntnh.herald.security;

import java.util.UUID;

final class IpAuthChallenge {

    private final UUID uuid;
    private final IpAddress address;
    private final String username;
    private final String code;
    private final long createdAt;
    private final long expiresAt;
    private long lastDmAttemptAt;

    IpAuthChallenge(UUID uuid, IpAddress address, String username, String code, long createdAt, long expiresAt) {
        this.uuid = uuid;
        this.address = address;
        this.username = username;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    UUID getUuid() {
        return uuid;
    }

    IpAddress getAddress() {
        return address;
    }

    String getUsername() {
        return username;
    }

    String getCode() {
        return code;
    }

    long getCreatedAt() {
        return createdAt;
    }

    boolean isExpired(long now) {
        return now >= expiresAt;
    }

    boolean canAttemptDm(long now, long cooldownMillis) {
        return lastDmAttemptAt == 0 || now - lastDmAttemptAt >= cooldownMillis;
    }

    void markDmAttempt(long now) {
        lastDmAttemptAt = now;
    }
}
