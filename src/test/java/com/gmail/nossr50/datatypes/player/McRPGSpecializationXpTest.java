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
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Every XP gain is multiplied by the skill's specialization role (1.25x Primary, 1.0x
 * Secondary, 0.35x otherwise), and profile saves carry the slots.
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
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.SMELTING);

        assertThat(mmoPlayer.modifyXpGain(PrimarySkillType.MINING, 100F)).isEqualTo(125F);
        assertThat(mmoPlayer.modifyXpGain(PrimarySkillType.SMELTING, 100F)).isEqualTo(100F);
        assertThat(mmoPlayer.modifyXpGain(PrimarySkillType.FISHING, 100F)).isEqualTo(35F);
    }

    @Test
    void playersWithoutChosenSkillsShouldEarnTheUnselectedRateEverywhere() {
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            assertThat(mmoPlayer.modifyXpGain(skill, 100F)).as("%s", skill).isEqualTo(35F);
        }
    }

    @Test
    void savingShouldWriteTheSlotsToTheDatabase() {
        // Given - a loaded profile with both slots filled
        final DatabaseManager database = mock(DatabaseManager.class);
        when(database.saveUser(any(PlayerProfile.class))).thenReturn(true);
        mockedMcMMO.when(mcMMO::getDatabaseManager).thenReturn(database);
        final PlayerProfile profile = new PlayerProfile("saver", UUID.randomUUID(), true, 0);
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.ARCHERY);
        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.TAMING);

        // When - it is saved (save writes a copy of the profile)
        profile.save(true);

        // Then - the copy handed to the database has the slots
        final ArgumentCaptor<PlayerProfile> saved = ArgumentCaptor.forClass(PlayerProfile.class);
        org.mockito.Mockito.verify(database).saveUser(saved.capture());
        assertThat(saved.getValue().getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.ARCHERY);
        assertThat(saved.getValue().getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.TAMING);
    }
}
