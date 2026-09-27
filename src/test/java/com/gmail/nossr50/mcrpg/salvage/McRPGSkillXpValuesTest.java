package com.gmail.nossr50.mcrpg.salvage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the shipped experience.yml values for mcRPG's Smelting and Salvage XP, so the rules
 * behind them stay true when values are tuned.
 */
class McRPGSkillXpValuesTest {
    private static final Set<String> RAW_ITEMS =
            Set.of("Raw_Copper", "Raw_Iron", "Raw_Gold", "Ancient_Debris");

    private static YamlConfiguration experience;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream in = McRPGSkillXpValuesTest.class.getClassLoader()
                .getResourceAsStream("experience.yml")) {
            assertThat(in).as("experience.yml on the test classpath").isNotNull();
            experience = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    private static ConfigurationSection section(String path) {
        final ConfigurationSection section = experience.getConfigurationSection(path);
        assertThat(section).as(path).isNotNull();
        return section;
    }

    @Test
    void smeltingOreBlocksShouldGiveHalfTheirMiningXp() {
        final ConfigurationSection smelting = section("Experience_Values.Smelting");
        final ConfigurationSection mining = section("Experience_Values.Mining");

        for (String key : smelting.getKeys(false)) {
            if (RAW_ITEMS.contains(key)) {
                continue;
            }
            assertThat(mining.contains(key)).as("%s is a Mining ore", key).isTrue();
            assertThat(smelting.getInt(key)).as("Smelting XP for %s", key)
                    .isEqualTo(mining.getInt(key) / 2);
        }
    }

    @Test
    void rawItemsShouldGiveAboutHalfTheirOresMiningXp() {
        final ConfigurationSection smelting = section("Experience_Values.Smelting");

        assertThat(smelting.getInt("Raw_Iron")).isEqualTo(450);        // Iron Ore 900
        assertThat(smelting.getInt("Raw_Gold")).isEqualTo(650);        // Gold Ore 1,300
        assertThat(smelting.getInt("Ancient_Debris")).isEqualTo(3900); // Ancient Debris 7,777
        assertThat(smelting.getInt("Raw_Copper")).isEqualTo(70);       // 500 / 3.5 drops / 2
    }

    @Test
    void farmableItemsShouldGiveNoSmeltingXp() {
        final ConfigurationSection smelting = section("Experience_Values.Smelting");

        assertThat(smelting.getKeys(false)).doesNotContain("Cobbled_Deepslate", "Cobblestone",
                "Copper_Nugget", "Sand", "Kelp", "Oak_Log");
    }

    @Test
    void salvageShouldMatchRepairsBase() {
        assertThat(experience.getDouble("Experience_Values.Salvage.Wear_Base"))
                .isEqualTo(experience.getDouble("Experience_Values.Repair.Base"));
        assertThat(experience.getInt("Experience_Values.Salvage.Found_Enchantment_Per_Level"))
                .isEqualTo(1000);
    }
}
