package com.ntnh.herald.security;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class IpAuthRecord {

    private final UUID uuid;
    private final Map<String, TrustedIp> trustedIps = new LinkedHashMap<>();

    IpAuthRecord(UUID uuid) {
        this.uuid = uuid;
    }

    UUID getUuid() {
        return uuid;
    }

    Map<String, TrustedIp> getTrustedIps() {
        return trustedIps;
    }

    IpAuthRecord copy() {
        IpAuthRecord copy = new IpAuthRecord(uuid);
        for (Map.Entry<String, TrustedIp> entry : trustedIps.entrySet()) {
            copy.trustedIps.put(
                entry.getKey(),
                entry.getValue()
                    .copy());
        }
        return copy;
    }
}
