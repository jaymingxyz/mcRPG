package com.gmail.nossr50.datatypes.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.database.DatabaseManager;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Every XP gain is multiplied by the role of the skill's category: 1.25x in the Primary
 * Specialization, 1.0x in the Secondary, 0.35x otherwise. Profile saves carry the
 * Specializations.
 */
class McRPGSpecializationXpTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(McRPGSpecializationXpTest.class.getName());

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        when(generalConfig.getPowerLevelCap()).thenReturn(Integer.MAX_VALUE);
        when(ExperienceConfig.getInstance().getFormulaSkillModifier(any())).thenReturn(1.0);
        when(ExperienceConfig.getInstance().getExperienceGainsMultiplier(any())).thenReturn(1.0);
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    @Test
    void xpShouldBeMultipliedBySpecializationRole() {
        final PlayerProfile profile = mmoPlayer.getProfile();
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.BOTANY);

        assertThat(mmoPlayer.modifyXpGain(PrimarySkillType.MINING, 100F)).isEqualTo(125F);
        assertThat(mmoPlayer.modifyXpGain(PrimarySkillType.SMELTING, 100F)).isEqualTo(125F);
        assertThat(mmoPlayer.modifyXpGain(PrimarySkillType.ALCHEMY, 100F)).isEqualTo(100F);
        assertThat(mmoPlayer.modifyXpGain(PrimarySkillType.FISHING, 100F)).isEqualTo(35F);
    }

    @Test
    void playersWithoutSpecializationsShouldEarnTheUnselectedRateEverywhere() {
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            assertThat(mmoPlayer.modifyXpGain(skill, 100F)).as("%s", skill).isEqualTo(35F);
        }
    }

    @Test
    void savingShouldWriteTheSpecializationsToTheDatabase() {
        // Given - a loaded profile with both Specializations chosen
        final DatabaseManager database = mock(DatabaseManager.class);
        when(database.saveUser(any(PlayerProfile.class))).thenReturn(true);
        mockedMcMMO.when(mcMMO::getDatabaseManager).thenReturn(database);
        final PlayerProfile profile = new PlayerProfile("saver", UUID.randomUUID(), true, 0);
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.RANGED);
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.SURVIVALISM);

        // When - it is saved (save writes a copy of the profile)
        profile.save(true);

        // Then - the copy handed to the database has the Specializations
        final ArgumentCaptor<PlayerProfile> saved = ArgumentCaptor.forClass(PlayerProfile.class);
        org.mockito.Mockito.verify(database).saveUser(saved.capture());
        assertThat(saved.getValue().getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.RANGED);
        assertThat(saved.getValue().getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(SkillCategory.SURVIVALISM);
    }
}
