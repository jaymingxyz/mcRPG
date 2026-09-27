package com.gmail.nossr50.mcrpg.specialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * The shipped XP multipliers: 1.25x for the Primary Specialization, 1.0x for the Secondary,
 * and 0.35x for every other skill.
 */
class SpecializationDefaultsTest {
    @Test
    void experienceYmlShouldShipTheSpecializationMultipliers() throws Exception {
        final YamlConfiguration experience;
        try (InputStream in = SpecializationDefaultsTest.class.getClassLoader()
                .getResourceAsStream("experience.yml")) {
            assertThat(in).isNotNull();
            experience = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        assertThat(experience.getDouble("Specialization.Multipliers.Primary")).isEqualTo(1.25);
        assertThat(experience.getDouble("Specialization.Multipliers.Secondary")).isEqualTo(1.0);
        assertThat(experience.getDouble("Specialization.Multipliers.Unselected"))
                .isEqualTo(0.35);

        // The fallbacks used when a server's experience.yml lacks a key match the file
        for (SpecializationRole role : SpecializationRole.values()) {
            assertThat(role.defaultMultiplier()).as("%s", role).isEqualTo(
                    experience.getDouble("Specialization.Multipliers." + role.configKey()));
        }
    }
}
