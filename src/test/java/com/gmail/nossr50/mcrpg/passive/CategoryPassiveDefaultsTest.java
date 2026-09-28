package com.gmail.nossr50.mcrpg.passive;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** The shipped category passive settings, and the fallbacks for a config.yml without them. */
class CategoryPassiveDefaultsTest {
    @Test
    void configYmlShouldShipThePassivesWithTheSameValuesAsTheFallbacks() throws Exception {
        final YamlConfiguration config;
        try (InputStream in = CategoryPassiveDefaultsTest.class.getClassLoader()
                .getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        assertThat(config.getBoolean("Specialization.Passives.Enabled")).isTrue();
        assertThat(config.getInt("Specialization.Passives.Unlock_Level_Sum")).isEqualTo(25);
        for (CategoryPassive passive : CategoryPassive.values()) {
            final String path = "Specialization.Passives." + passive.configKey() + ".";
            assertThat(config.getDouble(path + "Damage_Reduction_Per_Piece"))
                    .as("%s damage", passive).isEqualTo(passive.defaultDamageReduction());
            assertThat(config.getDouble(path + "Mining_Speed_Bonus"))
                    .as("%s speed", passive).isEqualTo(passive.defaultMiningSpeedBonus());
            assertThat(config.getDouble(path + "Durability_Loss_Reduction"))
                    .as("%s durability", passive)
                    .isEqualTo(passive.defaultDurabilityLossReduction());
        }
    }
}
