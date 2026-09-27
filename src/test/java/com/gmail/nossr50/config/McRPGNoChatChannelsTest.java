package com.gmail.nossr50.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.gmail.nossr50.datatypes.chat.ChatChannel;
import org.junit.jupiter.api.Test;

/**
 * mcRPG has no chat channels: no admin chat, party chat or chat spying. mcMMO's chat code checks
 * {@link ChatConfig} before routing any message, so with every check off, player messages
 * always go to normal chat.
 */
class McRPGNoChatChannelsTest {
    @Test
    void chatShouldAlwaysBeDisabled() {
        final ChatConfig chatConfig = ChatConfig.getInstance();

        assertThat(chatConfig.isChatEnabled()).isFalse();
        assertThat(chatConfig.isSpyingAutomatic()).isFalse();
        for (ChatChannel channel : ChatChannel.values()) {
            assertThat(chatConfig.isChatChannelEnabled(channel)).as("%s channel", channel)
                    .isFalse();
            assertThat(chatConfig.isConsoleIncludedInAudience(channel)).as("%s console", channel)
                    .isFalse();
        }
    }

    @Test
    void chatYmlShouldNotBeShipped() {
        assertThat(McRPGNoChatChannelsTest.class.getResource("/chat.yml")).isNull();
    }
}
