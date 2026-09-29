package com.gmail.nossr50.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.mcMMO;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The messages mcRPG sends a player when they join. */
class McRPGMotdTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(McRPGMotdTest.class.getName());

    private PluginDescriptionFile pluginYml;

    @BeforeEach
    void setUp() throws Exception {
        mockBaseEnvironment(logger);
        try (InputStream in = getClass().getResourceAsStream("/plugin.yml")) {
            pluginYml = new PluginDescriptionFile(in);
        }
        when(mcMMO.p.getDescription()).thenReturn(pluginYml);
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    @Test
    void joinMessagesShouldNotShowTheVersion() throws InvalidSkillException {
        // Even players allowed to see the version in /mcrpg
        when(Permissions.showversion(player)).thenReturn(true);

        Motd.displayAll(player);

        final List<String> sent = mockingDetails(player).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("sendMessage"))
                .map(invocation -> String.valueOf(invocation.getRawArguments()[0]))
                .toList();
        assertThat(sent).isNotEmpty()
                .noneMatch(line -> line.contains(pluginYml.getVersion()));
    }
}
