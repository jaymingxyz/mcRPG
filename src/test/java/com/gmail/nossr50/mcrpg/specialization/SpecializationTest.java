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
 * mcRPG's Specialization rules. Each Specialization is a whole skill category: every skill in
 * it earns the Specialization's XP rate, and abandoning it cuts every one of its skills to 10%
 * of its total XP. XP numbers use the default LINEAR curve in Standard mode: 11,300 +
 * 2,000 x level per level.
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

    private Specialization.SkillAbandon abandonOf(Specialization.AbandonPreview preview,
            PrimarySkillType skill) {
        return preview.skills().stream().filter(each -> each.skill() == skill).findFirst()
                .orElseThrow();
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
    void everySkillInAChosenCategoryShouldEarnItsSpecializationRate() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        Specialization.choose(profile, SpecializationSlot.SECONDARY, SkillCategory.BOTANY);

        for (PrimarySkillType skill : SkillCategory.METALLURGY.skills()) {
            assertThat(Specialization.xpMultiplier(profile, skill)).as("%s", skill)
                    .isEqualTo(1.25);
        }
        for (PrimarySkillType skill : SkillCategory.BOTANY.skills()) {
            assertThat(Specialization.xpMultiplier(profile, skill)).as("%s", skill)
                    .isEqualTo(1.0);
        }
        assertThat(Specialization.xpMultiplier(profile, PrimarySkillType.FISHING))
                .isEqualTo(0.35);
        assertThat(Specialization.hasEmptySlot(profile)).isFalse();
    }

    // --- choosing ---

    @Test
    void choosingShouldKeepTheCategorysProgress() {
        profile.modifySkill(PrimarySkillType.FISHING, 12);
        profile.setSkillXpLevel(PrimarySkillType.FISHING, 500F);

        final Specialization.ChooseResult result = Specialization.choose(profile,
                SpecializationSlot.SECONDARY, SkillCategory.SURVIVALISM);

        assertThat(result).isEqualTo(Specialization.ChooseResult.SUCCESS);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(SkillCategory.SURVIVALISM);
        assertThat(profile.getSkillLevel(PrimarySkillType.FISHING)).isEqualTo(12);
        assertThat(profile.getSkillXpLevelRaw(PrimarySkillType.FISHING)).isEqualTo(500F);
    }

    @Test
    void choosingShouldNotReplaceAChosenSpecialization() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);

        final Specialization.ChooseResult result = Specialization.choose(profile,
                SpecializationSlot.PRIMARY, SkillCategory.MELEE);

        assertThat(result).isEqualTo(Specialization.ChooseResult.SLOT_FILLED);
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.METALLURGY);
    }

    @Test
    void choosingShouldNotMakeOneCategoryBothSpecializations() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);

        final Specialization.ChooseResult result = Specialization.choose(profile,
                SpecializationSlot.SECONDARY, SkillCategory.METALLURGY);

        assertThat(result).isEqualTo(Specialization.ChooseResult.CATEGORY_IN_OTHER_SLOT);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
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
    void abandoningShouldKeepTenPercentOfEachSkillsTotalXp(int level, float xp,
            int expectedLevel, float expectedXp) {
        profile.modifySkill(PrimarySkillType.WOODCUTTING, level);
        profile.setSkillXpLevel(PrimarySkillType.WOODCUTTING, xp);
        Specialization.choose(profile, SpecializationSlot.SECONDARY, SkillCategory.BOTANY);

        final Specialization.AbandonPreview preview = Specialization.previewAbandon(profile,
                SkillCategory.BOTANY);

        assertThat(preview).isNotNull();
        assertThat(preview.slot()).isEqualTo(SpecializationSlot.SECONDARY);
        final Specialization.SkillAbandon woodcutting =
                abandonOf(preview, PrimarySkillType.WOODCUTTING);
        assertThat(woodcutting.oldLevel()).isEqualTo(level);
        assertThat(woodcutting.newLevel()).isEqualTo(expectedLevel);
        assertThat(woodcutting.newXp()).isEqualTo(expectedXp);
    }

    @Test
    void abandoningShouldCutEverySkillInTheCategoryAndNothingElse() {
        profile.modifySkill(PrimarySkillType.MINING, 40);
        profile.modifySkill(PrimarySkillType.SMELTING, 10);
        profile.modifySkill(PrimarySkillType.EXCAVATION, 6);
        profile.modifySkill(PrimarySkillType.WOODCUTTING, 40);
        Specialization.choose(profile, SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        Specialization.choose(profile, SpecializationSlot.SECONDARY, SkillCategory.BOTANY);

        Specialization.applyAbandon(profile,
                Specialization.previewAbandon(profile, SkillCategory.METALLURGY));

        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isEqualTo(9);
        assertThat(profile.getSkillXpLevelRaw(PrimarySkillType.MINING)).isEqualTo(27_500F);
        assertThat(profile.getSkillLevel(PrimarySkillType.SMELTING)).isEqualTo(1);
        assertThat(profile.getSkillLevel(PrimarySkillType.EXCAVATION)).isZero();
        // Only the abandoned category changes
        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isEqualTo(40);
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(SkillCategory.BOTANY);
    }

    @Test
    void previewShouldListTheCategorysSkillsWithoutChangingAnything() {
        profile.modifySkill(PrimarySkillType.MINING, 40);
        Specialization.choose(profile, SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);

        final Specialization.AbandonPreview preview =
                Specialization.previewAbandon(profile, SkillCategory.METALLURGY);

        assertThat(preview.skills()).extracting(Specialization.SkillAbandon::skill)
                .containsExactly(PrimarySkillType.MINING, PrimarySkillType.SMELTING,
                        PrimarySkillType.EXCAVATION);
        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isEqualTo(40);
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.METALLURGY);
    }

    @Test
    void previewShouldLeaveOutSkillsMissingFromThisMinecraftVersion() {
        // A server older than 1.21.11 has no Spears
        when(minecraftGameVersion.isAtLeast(1, 21, 11)).thenReturn(false);
        Specialization.choose(profile, SpecializationSlot.PRIMARY, SkillCategory.MELEE);

        final Specialization.AbandonPreview preview =
                Specialization.previewAbandon(profile, SkillCategory.MELEE);

        assertThat(preview.skills()).extracting(Specialization.SkillAbandon::skill)
                .containsExactly(PrimarySkillType.SWORDS, PrimarySkillType.AXES,
                        PrimarySkillType.MACES);
    }

    @Test
    void abandoningThePrimaryShouldNotMoveTheSecondaryUp() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        Specialization.choose(profile, SpecializationSlot.SECONDARY, SkillCategory.MELEE);

        Specialization.applyAbandon(profile,
                Specialization.previewAbandon(profile, SkillCategory.METALLURGY));

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(SkillCategory.MELEE);
    }

    @Test
    void onlyChosenCategoriesCanBeAbandoned() {
        assertThat(Specialization.previewAbandon(profile, SkillCategory.METALLURGY)).isNull();
    }

    @Test
    void resettingASkillShouldClearTheSpecializationItsCategoryIsIn() {
        Specialization.choose(profile, SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        Specialization.choose(profile, SpecializationSlot.SECONDARY, SkillCategory.MELEE);

        Specialization.clearSlotHolding(profile, PrimarySkillType.AXES);
        Specialization.clearSlotHolding(profile, PrimarySkillType.FISHING); // not chosen

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.METALLURGY);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }

    @Test
    void loadingTheSameCategoryTwiceShouldKeepOnlyThePrimary() {
        // Older builds stored single skills; Mining and Smelting both read as Metallurgy
        profile.loadSpecialization(SkillCategory.fromStoredName("MINING"),
                SkillCategory.fromStoredName("SMELTING"));

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.METALLURGY);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }
}
