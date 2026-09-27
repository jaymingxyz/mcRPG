package com.gmail.nossr50.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.config.party.PartyConfig;
import com.gmail.nossr50.mcMMO;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * mcRPG only supports Standard scaling, has no parties and has no XP perks. These tests check
 * that the settings for those features are gone from the shipped config files, and that editing
 * a server's config can't turn them back on.
 */
class McRPGRemovedSettingsTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(McRPGRemovedSettingsTest.class.getName());

    /** JUnit deletes this folder, and any config written into it, after each test. */
    @TempDir
    File dataFolder;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    @Test
    void retroModeShouldStayOffEvenWhenAServerConfigEnablesIt() throws Exception {
        // Given - a server config.yml that still has mcMMO's Retro Mode switch turned on
        Files.writeString(new File(dataFolder, "config.yml").toPath(), """
                General:
                    RetroMode:
                        Enabled: true
                """);
        when(mcMMO.p.getResource("config.yml")).thenAnswer(
                invocation -> shippedResource("config.yml"));

        // When - it is loaded through the real config loader
        final GeneralConfig loadedConfig = new GeneralConfig(dataFolder);

        // Then - Standard scaling is used anyway
        assertThat(loadedConfig.getIsRetroMode()).isFalse();
    }

    @Test
    void partiesShouldAlwaysBeDisabledWithoutCreatingAPartyFile() {
        // When - the party config is created the way the plugin creates it at startup
        final PartyConfig partyConfig = new PartyConfig(dataFolder);

        // Then - parties are off and no party.yml was written
        assertThat(partyConfig.isPartyEnabled()).isFalse();
        assertThat(new File(dataFolder, "party.yml")).doesNotExist();
    }

    @Test
    void partyYmlShouldNotBeShipped() {
        assertThat(McRPGRemovedSettingsTest.class.getResource("/party.yml")).isNull();
    }

    @Test
    void shippedConfigYmlShouldHaveNoRetroModeOrPartySettings() throws Exception {
        final YamlConfiguration configYml = loadShipped("config.yml");

        assertThat(configYml.contains("General.RetroMode")).as("General.RetroMode").isFalse();
        assertThat(configYml.contains("Party")).as("Party section").isFalse();
    }

    @Test
    void shippedExperienceYmlShouldHaveNoXpPerkBoost() throws Exception {
        final YamlConfiguration experienceYml = loadShipped("experience.yml");

        assertThat(experienceYml.contains("Experience_Formula.Custom_XP_Perk"))
                .as("Experience_Formula.Custom_XP_Perk").isFalse();
    }

    private static InputStream shippedResource(String fileName) {
        return McRPGRemovedSettingsTest.class.getClassLoader().getResourceAsStream(fileName);
    }

    private static YamlConfiguration loadShipped(String fileName) throws Exception {
        try (InputStream in = shippedResource(fileName)) {
            assertThat(in).as(fileName + " on the test classpath").isNotNull();
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
