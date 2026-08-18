package com.ntnh.herald.security;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Coordinates login checks, short-lived challenges, current-link validation, persistence, and audit events. */
public final class IpAuthManager implements Closeable {

    private static final Logger LOG = LogManager.getLogger("HeraldIpAuth");

    private final IpAuthSettings settings;
    private final IpAuthStore store;
    private final IpAuthAuditLogger auditLogger;
    private final LinkResolver linkResolver;
    private final DirectMessenger directMessenger;
    private final LongSupplier clock;
    private final SecureRandom secureRandom;
    private final Object challengeLock = new Object();
    private final Map<String, IpAuthChallenge> challenges = new HashMap<>();
    private final int codeBound;

    public IpAuthManager(IpAuthSettings settings, IpAuthStore store, IpAuthAuditLogger auditLogger,
        LinkResolver linkResolver, DirectMessenger directMessenger) throws IOException {
        this(
            settings,
            store,
            auditLogger,
            linkResolver,
            directMessenger,
            System::currentTimeMillis,
            new SecureRandom());
    }

    IpAuthManager(IpAuthSettings settings, IpAuthStore store, IpAuthAuditLogger auditLogger, LinkResolver linkResolver,
        DirectMessenger directMessenger, LongSupplier clock, SecureRandom secureRandom) throws IOException {
        this.settings = settings;
        this.store = store;
        this.auditLogger = auditLogger;
        this.linkResolver = linkResolver;
        this.directMessenger = directMessenger;
        this.clock = clock;
        this.secureRandom = secureRandom;
        int bound = 1;
        for (int i = 0; i < settings.getCodeDigits(); i++) bound *= 10;
        this.codeBound = bound;
        store.pruneToLimit(settings.getMaxTrustedIps());
    }

    public LoginResult checkLogin(String username, UUID uuid, InetAddress inetAddress) {
        if (!settings.isEnabled()) return LoginResult.allowed();
        long now = clock.getAsLong();
        String linkedDiscordId = resolveCurrentDiscordId(uuid);

        if (inetAddress == null) {
            auditLogger.failedIpCheck(
                now,
                username,
                uuid,
                "<unavailable>",
                linkedDiscordId,
                !store.hasTrustedIps(uuid),
                false);
            return LoginResult.rejected(
                "Unable to determine your connection IP.\n\nThe connection was rejected for safety.",
                null,
                false,
                false);
        }

        IpAddress address = IpAddress.from(inetAddress);
        if (store.isTrusted(uuid, address)) {
            try {
                store.touch(uuid, address, now);
            } catch (IOException e) {
                LOG.error("Could not persist last-seen time for trusted IP of " + uuid, e);
            }
            return LoginResult.allowed();
        }

        boolean initialEnrollment = !store.hasTrustedIps(uuid);
        if (initialEnrollment && !settings.isRequireInitialVerification()) {
            try {
                store.authorize(uuid, address, now, settings.getMaxTrustedIps());
                return LoginResult.allowed();
            } catch (IOException e) {
                LOG.error("Could not persist automatically enrolled IP for " + uuid, e);
                return LoginResult.rejected(
                    "Your login IP could not be saved.\n\nThe connection was rejected for safety.",
                    null,
                    false,
                    false);
            }
        }

        ChallengeSelection selection;
        try {
            synchronized (challengeLock) {
                purgeExpiredLocked(now);
                String key = challengeKey(uuid, address);
                IpAuthChallenge challenge = challenges.get(key);
                boolean created = challenge == null;
                if (created) {
                    challenge = new IpAuthChallenge(
                        uuid,
                        address,
                        username,
                        generateUniqueCodeLocked(),
                        now,
                        safeAdd(now, settings.getCodeExpiryMillis()));
                    challenges.put(key, challenge);
                }
                boolean sendDm = settings.isDmOnNewIp() && linkedDiscordId != null
                    && challenge.canAttemptDm(now, settings.getDmCooldownMillis());
                if (sendDm) challenge.markDmAttempt(now);
                selection = new ChallengeSelection(challenge, created, sendDm);
            }
        } catch (IllegalStateException e) {
            LOG.error("Could not allocate a Herald IP verification challenge", e);
            auditLogger
                .failedIpCheck(now, username, uuid, address.getText(), linkedDiscordId, initialEnrollment, false);
            return LoginResult.rejected(
                "No IP verification code is currently available.\n\nThe connection was rejected for safety.",
                null,
                false,
                false);
        }

        auditLogger.failedIpCheck(
            now,
            username,
            uuid,
            address.getText(),
            linkedDiscordId,
            initialEnrollment,
            selection.created);

        if (selection.sendDm) {
            try {
                directMessenger.send(linkedDiscordId, buildDmMessage(username, address, selection.challenge.getCode()));
            } catch (RuntimeException e) {
                LOG.warn("Could not schedule Herald IP-auth DM for Discord ID " + linkedDiscordId, e);
            }
        }

        return LoginResult.rejected(
            buildKickMessage(selection.challenge.getCode(), linkedDiscordId, selection.sendDm),
            selection.challenge.getCode(),
            selection.created,
            selection.sendDm);
    }

    public boolean recognizesVerificationMessage(String content) {
        return isVerificationCommand(content == null ? "" : content.trim());
    }

    /**
     * @return a reply when the content is a recognized v#### command, or {@code null} when normal DiscordSRV account
     *         linking should continue processing it.
     */
    public String handleDiscordMessage(String content, String authorDiscordId) {
        String command = content == null ? "" : content.trim();
        if (!isVerificationCommand(command)) return null;
        if (!settings.isEnabled()) return "Herald IP verification is disabled.";

        long now = clock.getAsLong();
        String submittedCode = command.substring(1);
        IpAuthChallenge challenge;
        synchronized (challengeLock) {
            purgeExpiredLocked(now);
            challenge = findByCodeLocked(submittedCode);
        }
        if (challenge == null) return "That IP verification challenge is invalid or expired.";

        String currentDiscordId = resolveCurrentDiscordId(challenge.getUuid());
        if (currentDiscordId == null || !currentDiscordId.equals(authorDiscordId)) {
            return "That IP verification challenge is invalid or is not linked to your Discord account.";
        }

        IpAuthStore.Authorization authorization;
        synchronized (challengeLock) {
            IpAuthChallenge current = challenges.get(challengeKey(challenge.getUuid(), challenge.getAddress()));
            if (current != challenge || challenge.isExpired(clock.getAsLong())) {
                return "That IP verification challenge is invalid or expired.";
            }

            // Re-check while consuming so unlinking or relinking invalidates the old identity immediately.
            currentDiscordId = resolveCurrentDiscordId(challenge.getUuid());
            if (currentDiscordId == null || !currentDiscordId.equals(authorDiscordId)) {
                return "That IP verification challenge is invalid or is not linked to your Discord account.";
            }
            try {
                authorization = store
                    .authorize(challenge.getUuid(), challenge.getAddress(), now, settings.getMaxTrustedIps());
            } catch (IOException e) {
                LOG.error("Could not persist authorized IP for " + challenge.getUuid(), e);
                return "The IP could not be saved. Please try the same verification code again.";
            }
            challenges.remove(challengeKey(challenge.getUuid(), challenge.getAddress()));
        }

        IpAddress evicted = authorization.getEvictedAddress();
        auditLogger.successfulAuthorization(
            now,
            challenge.getUsername(),
            challenge.getUuid(),
            challenge.getAddress()
                .getText(),
            authorDiscordId,
            evicted != null ? evicted.getText() : null);
        return "IP authorized for " + challenge.getUsername() + ". Reconnect to the Minecraft server.";
    }

    public Status getStatus(UUID uuid) {
        long now = clock.getAsLong();
        int pending = 0;
        synchronized (challengeLock) {
            purgeExpiredLocked(now);
            for (IpAuthChallenge challenge : challenges.values()) if (challenge.getUuid()
                .equals(uuid)) pending++;
        }
        return new Status(store.getTrustedIps(uuid), pending);
    }

    public void reset(UUID uuid) throws IOException {
        store.reset(uuid);
        synchronized (challengeLock) {
            Iterator<Map.Entry<String, IpAuthChallenge>> iterator = challenges.entrySet()
                .iterator();
            while (iterator.hasNext()) {
                if (iterator.next()
                    .getValue()
                    .getUuid()
                    .equals(uuid)) iterator.remove();
            }
        }
    }

    private String resolveCurrentDiscordId(UUID uuid) {
        try {
            return linkResolver.getDiscordId(uuid);
        } catch (RuntimeException e) {
            LOG.error("Could not resolve current Discord link for " + uuid, e);
            return null;
        }
    }

    private boolean isVerificationCommand(String command) {
        if (command.length() != settings.getCodeDigits() + 1) return false;
        char prefix = command.charAt(0);
        if (prefix != 'v' && prefix != 'V') return false;
        for (int i = 1; i < command.length(); i++) {
            char digit = command.charAt(i);
            if (digit < '0' || digit > '9') return false;
        }
        return true;
    }

    private String generateUniqueCodeLocked() {
        int start = secureRandom.nextInt(codeBound);
        for (int offset = 0; offset < codeBound; offset++) {
            int value = (start + offset) % codeBound;
            String code = leftPad(value, settings.getCodeDigits());
            if (findByCodeLocked(code) == null) return code;
        }
        throw new IllegalStateException("All Herald IP verification codes are currently in use");
    }

    private static String leftPad(int value, int digits) {
        String text = Integer.toString(value);
        StringBuilder result = new StringBuilder(digits);
        for (int i = text.length(); i < digits; i++) result.append('0');
        return result.append(text)
            .toString();
    }

    private IpAuthChallenge findByCodeLocked(String code) {
        for (IpAuthChallenge challenge : challenges.values()) if (challenge.getCode()
            .equals(code)) return challenge;
        return null;
    }

    private void purgeExpiredLocked(long now) {
        Iterator<Map.Entry<String, IpAuthChallenge>> iterator = challenges.entrySet()
            .iterator();
        while (iterator.hasNext()) if (iterator.next()
            .getValue()
            .isExpired(now)) iterator.remove();
    }

    private static String challengeKey(UUID uuid, IpAddress address) {
        return uuid + "|" + address.getKey();
    }

    private static long safeAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static String buildDmMessage(String username, IpAddress address, String code) {
        return "\u26a0 New Minecraft login IP\n\nSomeone attempted to join as " + username
            + " from "
            + address.getText()
            + ".\n\nThat IP is not currently authorized.\n\nTo authorize it, reply to this bot with:\n\nv"
            + code
            + "\n\nIf this was not you, do nothing. The connection was rejected.";
    }

    private static String buildKickMessage(String code, String linkedDiscordId, boolean dmAttempted) {
        StringBuilder message = new StringBuilder(
            "New IP address detected.\n\nAuthorize this IP by messaging the linked Discord bot:\n\nv").append(code);
        if (linkedDiscordId == null) {
            message.append("\n\nNo Discord account is currently linked. Link one before authorizing this IP.");
        } else if (dmAttempted) {
            message.append("\n\nA DM was also sent to your linked Discord account.");
        }
        return message.append("\n\nReconnect after authorization.")
            .toString();
    }

    @Override
    public void close() throws IOException {
        synchronized (challengeLock) {
            challenges.clear();
        }
        auditLogger.close();
    }

    public interface LinkResolver {

        String getDiscordId(UUID uuid);
    }

    public interface DirectMessenger {

        void send(String discordId, String message);
    }

    private static final class ChallengeSelection {

        private final IpAuthChallenge challenge;
        private final boolean created;
        private final boolean sendDm;

        private ChallengeSelection(IpAuthChallenge challenge, boolean created, boolean sendDm) {
            this.challenge = challenge;
            this.created = created;
            this.sendDm = sendDm;
        }
    }

    public static final class LoginResult {

        private final boolean allowed;
        private final String kickMessage;
        private final String code;
        private final boolean challengeCreated;
        private final boolean dmAttempted;

        private LoginResult(boolean allowed, String kickMessage, String code, boolean challengeCreated,
            boolean dmAttempted) {
            this.allowed = allowed;
            this.kickMessage = kickMessage;
            this.code = code;
            this.challengeCreated = challengeCreated;
            this.dmAttempted = dmAttempted;
        }

        static LoginResult allowed() {
            return new LoginResult(true, null, null, false, false);
        }

        static LoginResult rejected(String kickMessage, String code, boolean challengeCreated, boolean dmAttempted) {
            return new LoginResult(false, kickMessage, code, challengeCreated, dmAttempted);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getKickMessage() {
            return kickMessage;
        }

        public String getCode() {
            return code;
        }

        public boolean isChallengeCreated() {
            return challengeCreated;
        }

        public boolean isDmAttempted() {
            return dmAttempted;
        }
    }

    public static final class Status {

        private final List<TrustedIp> trustedIps;
        private final int pendingChallenges;

        private Status(List<TrustedIp> trustedIps, int pendingChallenges) {
            this.trustedIps = new ArrayList<>(trustedIps);
            this.pendingChallenges = pendingChallenges;
        }

        public List<TrustedIp> getTrustedIps() {
            return new ArrayList<>(trustedIps);
        }

        public int getPendingChallenges() {
            return pendingChallenges;
        }
    }
}
