package com.gmail.nossr50.mcrpg.salvage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Checks the shipped Salvage defaults that mcRPG depends on.
 */
class McRPGSalvageDefaultsTest {
    private static YamlConfiguration load(String resource) throws Exception {
        try (InputStream in = McRPGSalvageDefaultsTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertThat(in).as("%s on the test classpath", resource).isNotNull();
            return YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    void newPlayersShouldBeAbleToSalvage() throws Exception {
        // Salvage XP only comes from salvaging, so a level-1 unlock could never be reached
        final YamlConfiguration ranks = load("skillranks.yml");

        assertThat(ranks.getInt("Salvage.ScrapCollector.Standard.Rank_1", -1)).isZero();
    }

    @Test
    void playersOfAnyLevelShouldBeAbleToSalvageAnyGear() throws Exception {
        final ConfigurationSection salvageables =
                load("salvage.vanilla.yml").getConfigurationSection("Salvageables");
        assertThat(salvageables).isNotNull();
        assertThat(salvageables.getKeys(false)).contains("DIAMOND_CHESTPLATE",
                "NETHERITE_CHESTPLATE");

        for (String item : salvageables.getKeys(false)) {
            assertThat(salvageables.getInt(item + ".MinimumLevel")).as("%s MinimumLevel", item)
                    .isZero();
        }
    }

    @Test
    void salvageMasteryShouldReachHalfTheLostMaterialsAtLevel100() throws Exception {
        final YamlConfiguration advanced = load("advanced.yml");

        assertThat(advanced.getDouble("Skills.Salvage.SalvageMastery.MaxBonusPercentage"))
                .isEqualTo(50D);
        assertThat(advanced.getInt("Skills.Salvage.SalvageMastery.MaxBonusLevel"))
                .isEqualTo(100);
    }
}
