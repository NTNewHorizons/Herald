package com.ntnh.herald;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.ntnh.herald.security.IpAuthSettings;

/** Herald-only configuration. DiscordSRV remains authoritative for all DiscordSRV settings. */
public final class HeraldConfig {

    private static final String IP_AUTHENTICATION = "ipAuthentication";
    private static final String IP_AUTHENTICATION_AUDIT = "ipAuthentication.audit";

    public static boolean ipAuthenticationEnabled = true;
    public static int maxTrustedIps = 1;
    public static int codeDigits = 4;
    public static int codeExpirySeconds = 300;
    public static boolean requireInitialVerification = true;
    public static boolean dmOnNewIp = true;
    public static int dmCooldownSeconds = 30;
    public static boolean ipAuthenticationAuditEnabled = true;

    private HeraldConfig() {}

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);
        removeLegacyDiscordSrvCategories(configuration);

        ipAuthenticationEnabled = configuration.getBoolean(
            "enabled",
            IP_AUTHENTICATION,
            ipAuthenticationEnabled,
            "Reject untrusted login IPs until the currently linked Discord account authorizes them.");
        maxTrustedIps = configuration.getInt(
            "maxTrustedIps",
            IP_AUTHENTICATION,
            maxTrustedIps,
            0,
            Integer.MAX_VALUE,
            "Maximum trusted IPs per Minecraft UUID. 0 allows unlimited IPs; least-recently-used IPs are evicted.");
        codeDigits = configuration.getInt(
            "codeDigits",
            IP_AUTHENTICATION,
            codeDigits,
            1,
            9,
            "Number of decimal digits in the short Discord verification code.");
        codeExpirySeconds = configuration.getInt(
            "codeExpirySeconds",
            IP_AUTHENTICATION,
            codeExpirySeconds,
            1,
            Integer.MAX_VALUE,
            "Lifetime of an in-memory IP verification challenge.");
        requireInitialVerification = configuration.getBoolean(
            "requireInitialVerification",
            IP_AUTHENTICATION,
            requireInitialVerification,
            "Require Discord verification for a UUID's first trusted IP. A successful Discord account link "
                + "automatically enrolls the exact IP that initiated linking.");
        dmOnNewIp = configuration.getBoolean(
            "dmOnNewIp",
            IP_AUTHENTICATION,
            dmOnNewIp,
            "Attempt to DM the currently linked Discord account when an untrusted IP is rejected.");
        dmCooldownSeconds = configuration.getInt(
            "dmCooldownSeconds",
            IP_AUTHENTICATION,
            dmCooldownSeconds,
            0,
            Integer.MAX_VALUE,
            "Minimum interval between DMs for repeated attempts by the same Minecraft UUID and IP.");
        ipAuthenticationAuditEnabled = configuration.getBoolean(
            "enabled",
            IP_AUTHENTICATION_AUDIT,
            ipAuthenticationAuditEnabled,
            "Write failed IP checks and successful IP authorizations to logs/ip-auth-audit.log.");

        if (configuration.hasChanged()) configuration.save();
    }

    private static void removeLegacyDiscordSrvCategories(Configuration configuration) {
        for (String category : new String[] { Configuration.CATEGORY_GENERAL, "discord", "broadcasting" }) {
            if (configuration.hasCategory(category)) {
                configuration.removeCategory(configuration.getCategory(category));
            }
        }
    }

    public static IpAuthSettings ipAuthSettings() {
        return new IpAuthSettings(
            ipAuthenticationEnabled,
            maxTrustedIps,
            codeDigits,
            codeExpirySeconds,
            requireInitialVerification,
            dmOnNewIp,
            dmCooldownSeconds);
    }
}
