package com.ntnh.herald.security;

public final class IpAuthSettings {

    private final boolean enabled;
    private final int maxTrustedIps;
    private final int codeDigits;
    private final long codeExpiryMillis;
    private final boolean requireInitialVerification;
    private final boolean dmOnNewIp;
    private final long dmCooldownMillis;

    public IpAuthSettings(boolean enabled, int maxTrustedIps, int codeDigits, int codeExpirySeconds,
        boolean requireInitialVerification, boolean dmOnNewIp, int dmCooldownSeconds) {
        if (maxTrustedIps < 0) throw new IllegalArgumentException("maxTrustedIps cannot be negative");
        if (codeDigits < 1 || codeDigits > 9) throw new IllegalArgumentException("codeDigits must be between 1 and 9");
        if (codeExpirySeconds < 1) throw new IllegalArgumentException("codeExpirySeconds must be positive");
        if (dmCooldownSeconds < 0) throw new IllegalArgumentException("dmCooldownSeconds cannot be negative");
        this.enabled = enabled;
        this.maxTrustedIps = maxTrustedIps;
        this.codeDigits = codeDigits;
        this.codeExpiryMillis = codeExpirySeconds * 1000L;
        this.requireInitialVerification = requireInitialVerification;
        this.dmOnNewIp = dmOnNewIp;
        this.dmCooldownMillis = dmCooldownSeconds * 1000L;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxTrustedIps() {
        return maxTrustedIps;
    }

    public int getCodeDigits() {
        return codeDigits;
    }

    public long getCodeExpiryMillis() {
        return codeExpiryMillis;
    }

    public boolean isRequireInitialVerification() {
        return requireInitialVerification;
    }

    public boolean isDmOnNewIp() {
        return dmOnNewIp;
    }

    public long getDmCooldownMillis() {
        return dmCooldownMillis;
    }
}
