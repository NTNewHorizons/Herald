/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package github.scarsz.discordsrv.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.objects.managers.link.JdbcAccountLinkManager;

public class PlaceholderUtil {

    private PlaceholderUtil() {}

    private static final Pattern DISCORDSRV_PLACEHOLDER_PATTERN = Pattern.compile("%discordsrv_([a-zA-Z0-9_]+)%");
    private static final Pattern SPECIFIC_ROLE_PATTERN = Pattern.compile("role_(\\d+)_(\\w+)");
    private static long lastJdbcIssue = -1;

    public static String replacePlaceholders(String input) {
        return replacePlaceholders(input, null);
    }

    public static String replacePlaceholders(String input, OfflinePlayer player) {
        if (input == null) return null;
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) {
            Player onlinePlayer = player != null ? player.getPlayer() : null;
            input = me.clip.placeholderapi.PlaceholderAPI
                .setPlaceholders(onlinePlayer != null ? onlinePlayer : player, input);
        }
        // Herald runs without PlaceholderAPI; resolve the built-in %discordsrv_*% expansion natively
        input = replaceDiscordSRVPlaceholders(input, player);
        return input;
    }

    private static String replaceDiscordSRVPlaceholders(String input, OfflinePlayer player) {
        if (input == null || !input.contains("%discordsrv_")) return input;
        Matcher matcher = DISCORDSRV_PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String value = resolveDiscordSRVPlaceholder(matcher.group(1), player);
            if (value == null) value = matcher.group(0);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String resolveDiscordSRVPlaceholder(String identifier, OfflinePlayer player) {
        if (!DiscordSRV.isReady) return "...";

        Guild mainGuild = DiscordSRV.getPlugin()
            .getMainGuild();
        if (mainGuild == null) return "";

        Set<Member> onlineMembers = mainGuild.getMemberCache()
            .stream()
            .filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE)
            .collect(Collectors.toSet());
        Set<String> onlineMemberIds = onlineMembers.stream()
            .map(Member::getId)
            .collect(Collectors.toSet());
        AccountLinkManager accountLinkManager = DiscordSRV.getPlugin()
            .getAccountLinkManager();
        java.util.function.Supplier<Set<String>> linkedAccounts = () -> {
            if (accountLinkManager instanceof JdbcAccountLinkManager && Bukkit.isPrimaryThread()) {
                long currentTime = System.currentTimeMillis();
                if (lastJdbcIssue + TimeUnit.SECONDS.toMillis(10) < currentTime) {
                    DiscordSRV.warning(
                        "The %discordsrv_linked_online% placeholder was requested on the main thread while JDBC is enabled, this is unsupported");
                    lastJdbcIssue = currentTime;
                }
                return Collections.emptySet();
            }
            return accountLinkManager.getLinkedAccounts()
                .keySet();
        };

        switch (identifier) {
            case "guild_id":
                return mainGuild.getId();
            case "guild_name":
                return mainGuild.getName();
            case "guild_icon_id":
                return orEmptyString(mainGuild.getIconId());
            case "guild_icon_url":
                return orEmptyString(mainGuild.getIconUrl());
            case "guild_splash_id":
                return orEmptyString(mainGuild.getSplashId());
            case "guild_splash_url":
                return orEmptyString(mainGuild.getSplashUrl());
            case "guild_owner_effective_name":
                return applyOrEmptyString(mainGuild.getOwner(), Member::getEffectiveName);
            case "guild_owner_nickname":
                return applyOrEmptyString(mainGuild.getOwner(), Member::getNickname);
            case "guild_owner_game_name":
                return applyOrEmptyString(
                    mainGuild.getOwner(),
                    member -> member.getActivities()
                        .stream()
                        .findFirst()
                        .map(Activity::getName)
                        .orElse(""));
            case "guild_owner_game_url":
                return applyOrEmptyString(
                    mainGuild.getOwner(),
                    member -> member.getActivities()
                        .stream()
                        .findFirst()
                        .map(Activity::getUrl)
                        .orElse(""));
            case "guild_bot_effective_name":
                return mainGuild.getSelfMember()
                    .getEffectiveName();
            case "guild_bot_nickname":
                return orEmptyString(
                    mainGuild.getSelfMember()
                        .getNickname());
            case "guild_bot_game_name":
                return applyOrEmptyString(
                    mainGuild.getSelfMember(),
                    member -> member.getActivities()
                        .stream()
                        .findFirst()
                        .map(Activity::getName)
                        .orElse(""));
            case "guild_bot_game_url":
                return applyOrEmptyString(
                    mainGuild.getSelfMember(),
                    member -> member.getActivities()
                        .stream()
                        .findFirst()
                        .map(Activity::getUrl)
                        .orElse(""));
            case "guild_members_online":
                return String.valueOf(onlineMembers.size());
            case "guild_members_total":
                return String.valueOf(
                    mainGuild.getMembers()
                        .size());
            case "linked_online":
                return String.valueOf(
                    linkedAccounts.get()
                        .stream()
                        .filter(onlineMemberIds::contains)
                        .count());
            case "linked_total":
                return String.valueOf(accountLinkManager.getLinkedAccountCount());
            default:
                break;
        }

        Matcher roleMatcher = SPECIFIC_ROLE_PATTERN.matcher(identifier);
        if (roleMatcher.matches()) {
            String roleId = roleMatcher.group(1);
            Role role = DiscordUtil.getRole(roleId);
            String subPlaceholder = roleMatcher.group(2);
            if (role == null) return "";
            switch (subPlaceholder) {
                case "name":
                    return role.getName();
                case "color_hex":
                    return getHex(role.getColorRaw());
                case "color_code":
                    return colorCode(role.getColorRaw());
                default:
                    return "";
            }
        }

        if (player == null) return "";

        String userId = Bukkit.isPrimaryThread() ? accountLinkManager.getDiscordIdFromCache(player.getUniqueId())
            : accountLinkManager.getDiscordId(player.getUniqueId());
        switch (identifier) {
            case "user_id":
                return orEmptyString(userId);
            case "user_islinked":
                return getBoolean(userId != null);
            default:
                break;
        }

        User user = DiscordUtil.getUserById(userId);
        if (user == null) return "";

        switch (identifier) {
            case "user_name":
                return user.getName();
            case "user_tag":
                return user.getAsTag();
            default:
                break;
        }

        Member member = mainGuild.getMember(user);
        if (member == null) return "";

        switch (identifier) {
            case "user_effective_name":
                return member.getEffectiveName();
            case "user_nickname":
                return orEmptyString(member.getNickname());
            case "user_online_status":
                return member.getOnlineStatus()
                    .getKey();
            case "user_game_name":
                return member.getActivities()
                    .stream()
                    .findFirst()
                    .map(Activity::getName)
                    .orElse("");
            case "user_game_url":
                return member.getActivities()
                    .stream()
                    .findFirst()
                    .map(Activity::getUrl)
                    .orElse("");
            case "user_boost_status":
                return getBoolean(member.getTimeBoosted() != null);
            default:
                break;
        }

        if (member.getRoles()
            .isEmpty()) return "";

        Role topSelectedRole = DiscordSRV.getPlugin()
            .getTopSelectedRole(member);
        if (topSelectedRole != null) {
            switch (identifier) {
                case "user_top_selected_role_id":
                    return topSelectedRole.getId();
                case "user_top_selected_role_name":
                    return topSelectedRole.getName();
                case "user_top_selected_role_color_hex":
                    return applyOrEmptyString(topSelectedRole.getColorRaw(), PlaceholderUtil::getHex);
                case "user_top_selected_role_color_code":
                    return colorCode(topSelectedRole.getColorRaw());
                default:
                    break;
            }
        }

        Role topRole = DiscordUtil.getTopRole(member);
        if (topRole != null) {
            switch (identifier) {
                case "user_top_role_id":
                    return topRole.getId();
                case "user_top_role_name":
                    return topRole.getName();
                case "user_top_role_color_hex":
                    return applyOrEmptyString(topRole.getColorRaw(), PlaceholderUtil::getHex);
                case "user_top_role_color_code":
                    return colorCode(topRole.getColorRaw());
                default:
                    break;
            }
        }

        return null;
    }

    private static String colorCode(int color) {
        try {
            String legacy = MessageUtil.toLegacy(
                Component.text(0)
                    .color(TextColor.color(color)));
            return legacy.substring(0, legacy.length() - 1);
        } catch (Exception e) {
            return "";
        }
    }

    private static String getHex(int color) {
        return String.format("#%02x%02x%02x", (color & 0xFF0000) >> 16, (color & 0x00FF00) >> 8, (color & 0x0000FF));
    }

    private static <T> String applyOrEmptyString(T input, Function<T, String> function) {
        if (input == null) return "";
        String output = function.apply(input);
        return orEmptyString(output);
    }

    private static String orEmptyString(String input) {
        return StringUtils.isNotBlank(input) ? input : "";
    }

    private static String getBoolean(boolean input) {
        return input ? "true" : "false";
    }

    /**
     * Important when the content may contain role mentions
     */
    public static String replacePlaceholdersToDiscord(String input) {
        return replacePlaceholdersToDiscord(input, null);
    }

    /**
     * Important when the content may contain role mentions
     */
    public static String replacePlaceholdersToDiscord(String input, OfflinePlayer player) {
        boolean placeholderapi = PluginUtil.pluginHookIsEnabled("placeholderapi");

        // PlaceholderAPI has a side effect of replacing chat colors at the end of placeholder conversion
        // that breaks role mentions: <@&role id> because it converts the & to a §
        // So we add a zero width space after the & to prevent it from translating, and remove it after conversion
        if (placeholderapi) input = input.replace("&", "&\u200B");

        input = replacePlaceholders(input, player);

        if (placeholderapi) {
            input = MessageUtil.stripLegacy(input); // PAPI no longer replaces chat colors? strip both legacy codes
            input = input.replace("&\u200B", "&");
        }
        return input;
    }

    /*
     * Placeholders for the channel topic updater & channel updater
     */
    @SuppressWarnings({ "SpellCheckingInspection" })
    public static String replaceChannelUpdaterPlaceholders(String input) {
        if (StringUtils.isBlank(input)) return "";

        // set PAPI placeholders
        input = PlaceholderUtil.replacePlaceholdersToDiscord(input);

        if (input.contains("%time%") || input.contains("%date%")) {
            input = input.replaceAll("%time%|%date%", notNull(TimeUtil.timeStamp()));
        }
        input = replaceIfPresent(
            input,
            "%playercount%",
            () -> Integer.toString(
                PlayerUtil.getOnlinePlayers(true)
                    .size()));
        input = replaceIfPresent(input, "%playermax%", () -> Integer.toString(Bukkit.getMaxPlayers()));
        input = replaceIfPresent(input, "%totalplayers%", () -> Integer.toString(DiscordSRV.getTotalPlayerCount()));
        input = replaceIfPresent(
            input,
            "%uptimemins%",
            () -> Long.toString(
                TimeUnit.MILLISECONDS.toMinutes(
                    System.currentTimeMillis() - DiscordSRV.getPlugin()
                        .getStartTime())));
        input = replaceIfPresent(
            input,
            "%uptimehours%",
            () -> Long.toString(
                TimeUnit.MILLISECONDS.toHours(
                    System.currentTimeMillis() - DiscordSRV.getPlugin()
                        .getStartTime())));
        input = replaceIfPresent(
            input,
            "%uptimedays%",
            () -> Long.toString(
                TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - DiscordSRV.getPlugin()
                        .getStartTime())));
        input = replaceIfPresent(input, "%timestamp%", () -> Long.toString(System.currentTimeMillis() / 1000));
        input = replaceIfPresent(
            input,
            "%starttimestamp%",
            () -> Long.toString(
                TimeUnit.MILLISECONDS.toSeconds(
                    DiscordSRV.getPlugin()
                        .getStartTime())));
        input = replaceIfPresent(
            input,
            "%motd%",
            () -> StringUtils.isNotBlank(Bukkit.getMotd()) ? MessageUtil.strip(Bukkit.getMotd()) : "");
        input = replaceIfPresent(input, "%serverversion%", Bukkit::getBukkitVersion);

        if (containsAny(
            input,
            "%freememory%",
            "%usedmemory%",
            "%totalmemory%",
            "%maxmemory%",
            "%freememorygb%",
            "%usedmemorygb%",
            "%totalmemorygb%",
            "%maxmemorygb%")) {
            final Map<String, String> mem = MemUtil.get();
            input = input.replace("%freememory%", notNull(mem.get("freeMB")))
                .replace("%usedmemory%", notNull(mem.get("usedMB")))
                .replace("%totalmemory%", notNull(mem.get("totalMB")))
                .replace("%maxmemory%", notNull(mem.get("maxMB")))
                .replace("%freememorygb%", notNull(mem.get("freeGB")))
                .replace("%usedmemorygb%", notNull(mem.get("usedGB")))
                .replace("%totalmemorygb%", notNull(mem.get("totalGB")))
                .replace("%maxmemorygb%", notNull(mem.get("maxGB")));
        }
        input = replaceIfPresent(input, "%tps%", Lag::getTPSString);

        return input;
    }

    private static String replaceIfPresent(String input, String placeholder, Supplier<?> value) {
        if (!input.contains(placeholder)) return input;
        return input.replace(placeholder, notNull(value.get()));
    }

    private static boolean containsAny(String input, String... placeholders) {
        for (String placeholder : placeholders) {
            if (input.contains(placeholder)) return true;
        }
        return false;
    }

    public static String notNull(Object object) {
        return object != null ? object.toString() : "";
    }

}
