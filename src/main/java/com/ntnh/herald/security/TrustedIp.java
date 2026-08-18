package com.ntnh.herald.security;

public final class TrustedIp {

    private final IpAddress address;
    private final long firstVerifiedAt;
    private long lastSeenAt;

    TrustedIp(IpAddress address, long firstVerifiedAt, long lastSeenAt) {
        this.address = address;
        this.firstVerifiedAt = firstVerifiedAt;
        this.lastSeenAt = lastSeenAt;
    }

    public IpAddress getAddress() {
        return address;
    }

    public long getFirstVerifiedAt() {
        return firstVerifiedAt;
    }

    public long getLastSeenAt() {
        return lastSeenAt;
    }

    void setLastSeenAt(long lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    TrustedIp copy() {
        return new TrustedIp(address, firstVerifiedAt, lastSeenAt);
    }
}
