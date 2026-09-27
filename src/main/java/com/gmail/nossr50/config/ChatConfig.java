package com.gmail.nossr50.config;

import com.gmail.nossr50.datatypes.chat.ChatChannel;
import org.jetbrains.annotations.NotNull;

/**
 * mcRPG has no chat channels (admin chat, party chat or chat spying). This class keeps mcMMO's
 * chat checks compiling, and every one of them sees chat as disabled. No chat.yml is created,
 * because there is nothing to configure.
 */
public class ChatConfig {
    private static ChatConfig instance;

    private ChatConfig() {
    }

    public static ChatConfig getInstance() {
        if (instance == null) {
            instance = new ChatConfig();
        }

        return instance;
    }

    public boolean isChatEnabled() {
        return false;
    }

    public boolean isChatChannelEnabled(@NotNull ChatChannel chatChannel) {
        return false;
    }

    /**
     * Whether to use display names for players in target {@link ChatChannel}
     *
     * @param chatChannel target chat channel
     * @return true if display names should be used
     */
    public boolean useDisplayNames(@NotNull ChatChannel chatChannel) {
        return true;
    }

    public boolean isConsoleIncludedInAudience(@NotNull ChatChannel chatChannel) {
        return false;
    }

    public boolean isSpyingAutomatic() {
        return false;
    }
}
