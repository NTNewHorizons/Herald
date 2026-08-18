package com.ntnh.herald.security;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Thread-safe, easy-to-inspect TSV persistence for UUID-scoped trusted addresses. */
public final class IpAuthStore {

    private static final String HEADER = "# Herald trusted IPs v1: uuid<TAB>address<TAB>firstVerifiedAt<TAB>lastSeenAt";

    private final Path file;
    private final Map<UUID, IpAuthRecord> records = new HashMap<>();

    public IpAuthStore(Path file) throws IOException {
        this.file = file;
        load();
    }

    private synchronized void load() throws IOException {
        records.clear();
        if (!Files.exists(file)) return;

        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.trim()
                .isEmpty() || line.startsWith("#")) continue;
            String[] fields = line.split("\\t", -1);
            if (fields.length != 4) throw new IOException("Invalid trusted-IP entry at line " + lineNumber);
            try {
                UUID uuid = UUID.fromString(fields[0]);
                IpAddress address = IpAddress.parse(fields[1]);
                long firstVerifiedAt = Long.parseLong(fields[2]);
                long lastSeenAt = Long.parseLong(fields[3]);
                IpAuthRecord record = records.computeIfAbsent(uuid, IpAuthRecord::new);
                record.getTrustedIps()
                    .put(address.getKey(), new TrustedIp(address, firstVerifiedAt, lastSeenAt));
            } catch (IllegalArgumentException | UnknownHostException e) {
                throw new IOException("Invalid trusted-IP entry at line " + lineNumber, e);
            }
        }
    }

    public synchronized boolean hasTrustedIps(UUID uuid) {
        IpAuthRecord record = records.get(uuid);
        return record != null && !record.getTrustedIps()
            .isEmpty();
    }

    public synchronized boolean isTrusted(UUID uuid, IpAddress address) {
        IpAuthRecord record = records.get(uuid);
        return record != null && record.getTrustedIps()
            .containsKey(address.getKey());
    }

    public synchronized void touch(UUID uuid, IpAddress address, long now) throws IOException {
        IpAuthRecord record = records.get(uuid);
        if (record == null) return;
        TrustedIp trustedIp = record.getTrustedIps()
            .get(address.getKey());
        if (trustedIp == null) return;

        long previous = trustedIp.getLastSeenAt();
        trustedIp.setLastSeenAt(now);
        try {
            saveLocked();
        } catch (IOException e) {
            trustedIp.setLastSeenAt(previous);
            throw e;
        }
    }

    public synchronized Authorization authorize(UUID uuid, IpAddress address, long now, int maxTrustedIps)
        throws IOException {
        IpAuthRecord original = records.get(uuid);
        IpAuthRecord rollback = original != null ? original.copy() : null;
        IpAuthRecord record = records.computeIfAbsent(uuid, IpAuthRecord::new);
        TrustedIp existing = record.getTrustedIps()
            .get(address.getKey());
        if (existing == null) {
            record.getTrustedIps()
                .put(address.getKey(), new TrustedIp(address, now, now));
        } else {
            existing.setLastSeenAt(now);
        }

        TrustedIp evicted = pruneRecord(record, maxTrustedIps, address.getKey());
        try {
            saveLocked();
        } catch (IOException e) {
            if (rollback == null) records.remove(uuid);
            else records.put(uuid, rollback);
            throw e;
        }
        return new Authorization(evicted != null ? evicted.getAddress() : null);
    }

    public synchronized int pruneToLimit(int maxTrustedIps) throws IOException {
        if (maxTrustedIps == 0) return 0;
        Map<UUID, IpAuthRecord> rollback = copyRecords();
        int removed = 0;
        for (IpAuthRecord record : records.values()) {
            while (record.getTrustedIps()
                .size() > maxTrustedIps) {
                if (pruneRecord(record, maxTrustedIps, null) != null) removed++;
            }
        }
        if (removed == 0) return 0;
        try {
            saveLocked();
        } catch (IOException e) {
            records.clear();
            records.putAll(rollback);
            throw e;
        }
        return removed;
    }

    public synchronized List<TrustedIp> getTrustedIps(UUID uuid) {
        IpAuthRecord record = records.get(uuid);
        List<TrustedIp> result = new ArrayList<>();
        if (record != null) {
            for (TrustedIp trustedIp : record.getTrustedIps()
                .values()) result.add(trustedIp.copy());
        }
        result.sort(
            Comparator.comparingLong(TrustedIp::getLastSeenAt)
                .reversed());
        return result;
    }

    public synchronized void reset(UUID uuid) throws IOException {
        IpAuthRecord removed = records.remove(uuid);
        if (removed == null) return;
        try {
            saveLocked();
        } catch (IOException e) {
            records.put(uuid, removed);
            throw e;
        }
    }

    private static TrustedIp pruneRecord(IpAuthRecord record, int maxTrustedIps, String protectedAddressKey) {
        if (maxTrustedIps == 0 || record.getTrustedIps()
            .size() <= maxTrustedIps) return null;
        TrustedIp oldest = null;
        String oldestKey = null;
        for (Map.Entry<String, TrustedIp> entry : record.getTrustedIps()
            .entrySet()) {
            if (entry.getKey()
                .equals(protectedAddressKey)) continue;
            TrustedIp candidate = entry.getValue();
            if (oldest == null || candidate.getLastSeenAt() < oldest.getLastSeenAt()
                || (candidate.getLastSeenAt() == oldest.getLastSeenAt()
                    && candidate.getFirstVerifiedAt() < oldest.getFirstVerifiedAt())) {
                oldest = candidate;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) record.getTrustedIps()
            .remove(oldestKey);
        return oldest;
    }

    private Map<UUID, IpAuthRecord> copyRecords() {
        Map<UUID, IpAuthRecord> copy = new HashMap<>();
        for (Map.Entry<UUID, IpAuthRecord> entry : records.entrySet()) copy.put(
            entry.getKey(),
            entry.getValue()
                .copy());
        return copy;
    }

    private void saveLocked() throws IOException {
        Path parent = file.toAbsolutePath()
            .getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "ip-auth-trusted-ips-", ".tmp");
        boolean moved = false;
        try {
            List<IpAuthRecord> sortedRecords = new ArrayList<>(records.values());
            sortedRecords.sort(
                Comparator.comparing(
                    record -> record.getUuid()
                        .toString()));
            try (BufferedWriter writer = Files
                .newBufferedWriter(temporary, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(HEADER);
                writer.newLine();
                for (IpAuthRecord record : sortedRecords) writeRecord(writer, record);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }

    private static void writeRecord(BufferedWriter writer, IpAuthRecord record) throws IOException {
        Collection<TrustedIp> values = record.getTrustedIps()
            .values();
        List<TrustedIp> trustedIps = new ArrayList<>(values);
        trustedIps.sort(
            Comparator.comparing(
                trustedIp -> trustedIp.getAddress()
                    .getKey()));
        for (TrustedIp trustedIp : trustedIps) {
            writer.write(
                record.getUuid()
                    .toString());
            writer.write('\t');
            writer.write(
                trustedIp.getAddress()
                    .getText());
            writer.write('\t');
            writer.write(Long.toString(trustedIp.getFirstVerifiedAt()));
            writer.write('\t');
            writer.write(Long.toString(trustedIp.getLastSeenAt()));
            writer.newLine();
        }
    }

    public static final class Authorization {

        private final IpAddress evictedAddress;

        Authorization(IpAddress evictedAddress) {
            this.evictedAddress = evictedAddress;
        }

        public IpAddress getEvictedAddress() {
            return evictedAddress;
        }
    }
}
