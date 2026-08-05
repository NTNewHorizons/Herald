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

package github.scarsz.discordsrv.objects.threads;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;

public class ChannelTopicUpdater extends Thread {

    public ChannelTopicUpdater() {
        setName("DiscordSRV - Channel Topic Updater");
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            boolean updated = runUpdateCycle();
            int delay = updated ? getUpdateRate() : 1;
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(delay));
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Channel Topic Updater thread: sleep interrupted");
                interrupt();
                return;
            }
        }
    }

    private boolean runUpdateCycle() {
        if (DiscordUtil.getJda() == null) {
            DiscordSRV.debug("Skipping channel topic update cycle, JDA was null; retrying in one minute");
            return false;
        }

        boolean successful = true;
        try {
            String chatTopic = PlaceholderUtil
                .replaceChannelUpdaterPlaceholders(LangUtil.Message.CHAT_CHANNEL_TOPIC.toString());
            if (StringUtils.isNotBlank(chatTopic)) DiscordUtil.setTextChannelTopic(
                DiscordSRV.getPlugin()
                    .getMainTextChannel(),
                chatTopic);
        } catch (RuntimeException exception) {
            successful = false;
            DiscordSRV.error("Chat channel topic update failed; retrying next cycle", exception);
        }

        try {
            String consoleTopic = PlaceholderUtil
                .replaceChannelUpdaterPlaceholders(LangUtil.Message.CONSOLE_CHANNEL_TOPIC.toString());
            if (StringUtils.isNotBlank(consoleTopic)) DiscordUtil.setTextChannelTopic(
                DiscordSRV.getPlugin()
                    .getConsoleChannel(),
                consoleTopic);
        } catch (RuntimeException exception) {
            successful = false;
            DiscordSRV.error("Console channel topic update failed; retrying next cycle", exception);
        }
        return successful;
    }

    private int getUpdateRate() {
        try {
            int rate = DiscordSRV.config()
                .getInt("ChannelTopicUpdaterRateInMinutes");
            return Math.max(rate, 10);
        } catch (RuntimeException exception) {
            DiscordSRV.error("Failed to read channel topic updater rate; using ten minutes", exception);
            return 10;
        }
    }

}
