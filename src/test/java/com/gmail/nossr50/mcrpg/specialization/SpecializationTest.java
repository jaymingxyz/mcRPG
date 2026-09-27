package com.gmail.nossr50.mcrpg.specialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.experience.FormulaType;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * mcRPG's specialization rules: roles and XP multipliers, choosing a skill, and abandoning one
 * (the skill keeps 10% of its total XP and its level is worked out again). XP numbers use the
 * default LINEAR curve in Standard mode: 11,300 + 2,000 x level per level.
 */
class SpecializationTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(SpecializationTest.class.getName());

    private PlayerProfile profile;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);

        // Default LINEAR curve (Base 1020, Multiplier 20), Standard mode
        when(ExperienceConfig.getInstance().getFormulaType()).thenReturn(FormulaType.LINEAR);
        when(ExperienceConfig.getInstance().getBase(FormulaType.LINEAR)).thenReturn(1020);
        when(ExperienceConfig.getInstance().getMultiplier(FormulaType.LINEAR)).thenReturn(20D);
        mockedMcMMO.when(mcMMO::isRetroModeEnabled).thenReturn(false);

        // Real default multipliers and abandon setting, and no level caps
        when(ExperienceConfig.getInstance()
                .getSpecializationMultiplier(SpecializationRole.PRIMARY)).thenReturn(1.25);
        when(ExperienceConfig.getInstance()
                .getSpecializationMultiplier(SpecializationRole.SECONDARY)).thenReturn(1.0);
        when(ExperienceConfig.getInstance()
                .getSpecializationMultiplier(SpecializationRole.UNSELECTED)).thenReturn(0.35);
        when(generalConfig.getAbandonXpKeptPercent()).thenReturn(10D);
        when(generalConfig.getLevelCap(any(PrimarySkillType.class)))
                .thenReturn(Integer.MAX_VALUE);

        profile = new PlayerProfile("specialist", UUID.randomUUID(), 0);
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    // --- roles and multipliers ---

    @Test
    void everySkillShouldEarnUnselectedRateBeforeAnythingIsChosen() {
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            assertThat(Specialization.roleOf(profile, skill))
                    .isEqualTo(SpecializationRole.UNSELECTED);
            assertThat(Specialization.xpMultiplier(profile, skill)).isEqualTo(0.35);
        }
        assertThat(Specialization.hasEmptySlot(profile)).isTrue();
    }

    @Test
    void chosenSkillsShouldEarnTheirSlotRates() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        Specialization.choose(profile, SpecializationSlot.SECONDARY, PrimarySkillType.SMELTING);

        assertThat(Specialization.xpMultiplier(profile, PrimarySkillType.MINING)).isEqualTo(1.25);
        assertThat(Specialization.xpMultiplier(profile, PrimarySkillType.SMELTING))
                .isEqualTo(1.0);
        assertThat(Specialization.xpMultiplier(profile, PrimarySkillType.FISHING))
                .isEqualTo(0.35);
        assertThat(Specialization.hasEmptySlot(profile)).isFalse();
    }

    // --- choosing ---

    @Test
    void choosingShouldFillAnEmptySlotAndKeepTheSkillsProgress() {
        profile.modifySkill(PrimarySkillType.FISHING, 12);
        profile.setSkillXpLevel(PrimarySkillType.FISHING, 500F);

        final Specialization.ChooseResult result = Specialization.choose(profile,
                SpecializationSlot.SECONDARY, PrimarySkillType.FISHING);

        assertThat(result).isEqualTo(Specialization.ChooseResult.SUCCESS);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.FISHING);
        assertThat(profile.getSkillLevel(PrimarySkillType.FISHING)).isEqualTo(12);
        assertThat(profile.getSkillXpLevelRaw(PrimarySkillType.FISHING)).isEqualTo(500F);
    }

    @Test
    void choosingShouldNotReplaceAFilledSlot() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, PrimarySkillType.MINING);

        final Specialization.ChooseResult result = Specialization.choose(profile,
                SpecializationSlot.PRIMARY, PrimarySkillType.SWORDS);

        assertThat(result).isEqualTo(Specialization.ChooseResult.SLOT_FILLED);
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.MINING);
    }

    @Test
    void choosingShouldNotPutTheSameSkillInBothSlots() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, PrimarySkillType.MINING);

        final Specialization.ChooseResult result = Specialization.choose(profile,
                SpecializationSlot.SECONDARY, PrimarySkillType.MINING);

        assertThat(result).isEqualTo(Specialization.ChooseResult.SKILL_IN_OTHER_SLOT);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }

    @Test
    void choosingShouldRejectSkillsMissingFromThisMinecraftVersion() {
        // A server older than 1.21.11 has no Spears
        when(minecraftGameVersion.isAtLeast(1, 21, 11)).thenReturn(false);

        final Specialization.ChooseResult result = Specialization.choose(profile,
                SpecializationSlot.PRIMARY, PrimarySkillType.SPEARS);

        assertThat(result).isEqualTo(Specialization.ChooseResult.SKILL_UNAVAILABLE);
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
    }

    // --- abandoning ---

    @Test
    void totalXpShouldMatchTheStandardCurve() {
        assertThat(Specialization.totalXp(1, 0F)).isEqualTo(11_300L);
        assertThat(Specialization.totalXp(10, 0F)).isEqualTo(203_000L);
        assertThat(Specialization.totalXp(100, 0F)).isEqualTo(11_030_000L);
    }

    @ParameterizedTest(name = "level {0} with {1} XP -> level {2} with {3} XP")
    @CsvSource({
            // level, xp toward next, expected level, expected leftover xp
            "6, 0, 0, 9780",
            "10, 0, 1, 9000",
            "20, 0, 4, 3400",
            "40, 0, 9, 27500",
            "40, 10000, 9, 28500",
            "64, 0, 17, 11420",
            "100, 0, 28, 30600",
            "1000, 0, 312, 472400"
    })
    void abandoningShouldKeepTenPercentOfTotalXp(int level, float xp, int expectedLevel,
            float expectedXp) {
        profile.modifySkill(PrimarySkillType.WOODCUTTING, level);
        profile.setSkillXpLevel(PrimarySkillType.WOODCUTTING, xp);
        Specialization.choose(profile, SpecializationSlot.SECONDARY,
                PrimarySkillType.WOODCUTTING);

        final Specialization.AbandonPreview preview = Specialization.previewAbandon(profile,
                PrimarySkillType.WOODCUTTING);

        assertThat(preview).isNotNull();
        assertThat(preview.oldLevel()).isEqualTo(level);
        assertThat(preview.newLevel()).isEqualTo(expectedLevel);
        assertThat(preview.newXp()).isEqualTo(expectedXp);
        assertThat(preview.slot()).isEqualTo(SpecializationSlot.SECONDARY);
    }

    @Test
    void previewShouldNotChangeAnything() {
        profile.modifySkill(PrimarySkillType.WOODCUTTING, 40);
        Specialization.choose(profile, SpecializationSlot.SECONDARY,
                PrimarySkillType.WOODCUTTING);

        Specialization.previewAbandon(profile, PrimarySkillType.WOODCUTTING);

        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isEqualTo(40);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.WOODCUTTING);
    }

    @Test
    void applyingAbandonShouldSetTheNewLevelAndEmptyOnlyThatSlot() {
        profile.modifySkill(PrimarySkillType.WOODCUTTING, 40);
        Specialization.choose(profile, SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        Specialization.choose(profile, SpecializationSlot.SECONDARY,
                PrimarySkillType.WOODCUTTING);

        Specialization.applyAbandon(profile,
                Specialization.previewAbandon(profile, PrimarySkillType.WOODCUTTING));

        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isEqualTo(9);
        assertThat(profile.getSkillXpLevelRaw(PrimarySkillType.WOODCUTTING)).isEqualTo(27_500F);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.MINING);
    }

    @Test
    void abandoningThePrimaryShouldNotMoveTheSecondaryUp() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        Specialization.choose(profile, SpecializationSlot.SECONDARY, PrimarySkillType.SWORDS);

        Specialization.applyAbandon(profile,
                Specialization.previewAbandon(profile, PrimarySkillType.MINING));

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.SWORDS);
    }

    @Test
    void onlyChosenSkillsCanBeAbandoned() {
        assertThat(Specialization.previewAbandon(profile, PrimarySkillType.MINING)).isNull();
    }

    @Test
    void clearSlotHoldingShouldEmptyTheSlotWithThatSkill() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        Specialization.choose(profile, SpecializationSlot.SECONDARY, PrimarySkillType.SWORDS);

        Specialization.clearSlotHolding(profile, PrimarySkillType.SWORDS);
        Specialization.clearSlotHolding(profile, PrimarySkillType.FISHING); // not chosen

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.MINING);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }
}
