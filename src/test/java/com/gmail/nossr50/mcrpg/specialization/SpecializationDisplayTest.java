package com.gmail.nossr50.mcrpg.specialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.commands.experience.SkillresetTestAccess;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.placeholders.SpecializationPlaceholder;
import com.gmail.nossr50.util.player.UserManager;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What players see about Specializations: /rpgstats tags, XP bar tags, the login reminder
 * text, placeholders, and /rpgskillreset clearing a Specialization.
 */
class SpecializationDisplayTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(SpecializationDisplayTest.class.getName());

    private PlayerProfile profile;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        profile = mmoPlayer.getProfile();
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    private static String plain(String text) {
        return ChatColor.stripColor(text);
    }

    @Test
    void numbersShouldBeShownWithoutTrailingZeros() {
        assertThat(SpecializationDisplay.formatNumber(1.25)).isEqualTo("1.25");
        assertThat(SpecializationDisplay.formatNumber(1.0)).isEqualTo("1");
        assertThat(SpecializationDisplay.formatNumber(0.25)).isEqualTo("0.25");
        assertThat(SpecializationDisplay.formatNumber(10)).isEqualTo("10");
    }

    @Test
    void statsTagShouldShowTheRoleAndRateOfTheSkillsCategory() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.BOTANY);

        assertThat(plain(SpecializationDisplay.statsTag(profile, PrimarySkillType.SMELTING)))
                .isEqualTo("(Primary, 1.25x XP)");
        assertThat(plain(SpecializationDisplay.statsTag(profile, PrimarySkillType.ALCHEMY)))
                .isEqualTo("(Secondary, 1x XP)");
        assertThat(plain(SpecializationDisplay.statsTag(profile, PrimarySkillType.FISHING)))
                .isEqualTo("(0.35x XP)");
    }

    @Test
    void categoriesShouldBeShownByNameWithTheirSkills() {
        assertThat(SpecializationDisplay.categoryName(SkillCategory.METALLURGY))
                .isEqualTo("Metallurgy");
        assertThat(SpecializationDisplay.skillList(SkillCategory.BLACKSMITHING))
                .isEqualTo("Repair, Salvage");

        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        assertThat(plain(SpecializationDisplay.summary(profile)))
                .isEqualTo("Specializations: Primary: Metallurgy | Secondary: none");
    }

    @Test
    void xpBarTitleShouldOnlyTagSkillsInAChosenCategory() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);

        assertThat(SpecializationDisplay.xpBarTitle("Excavation Lv.5", profile,
                PrimarySkillType.EXCAVATION)).isEqualTo("Excavation Lv.5 (Primary)");
        assertThat(SpecializationDisplay.xpBarTitle("Fishing Lv.5", profile,
                PrimarySkillType.FISHING)).isEqualTo("Fishing Lv.5");
    }

    @Test
    void reminderShouldNameTheMissingSpecializations() {
        assertThat(plain(LoginReminder.reminderText(profile)))
                .isEqualTo("You haven't chosen your Primary and Secondary Specializations yet.");

        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        assertThat(plain(LoginReminder.reminderText(profile)))
                .isEqualTo("You haven't chosen a Secondary Specialization yet.");

        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.MELEE);
        assertThat(LoginReminder.reminderText(profile)).isNull();
    }

    @Test
    void placeholdersShouldShowTheChosenCategories() {
        when(UserManager.getPlayer(player)).thenReturn(mmoPlayer);
        final SpecializationPlaceholder primary =
                new SpecializationPlaceholder(SpecializationSlot.PRIMARY);
        final SpecializationPlaceholder secondary =
                new SpecializationPlaceholder(SpecializationSlot.SECONDARY);
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);

        assertThat(primary.getName()).isEqualTo("primary_specialization");
        assertThat(secondary.getName()).isEqualTo("secondary_specialization");
        assertThat(primary.process(player, "")).isEqualTo("Metallurgy");
        assertThat(secondary.process(player, "")).isEmpty();
    }

    @Test
    void skillResetShouldAlsoClearTheSpecializationItsCategoryIsIn() {
        profile.modifySkill(PrimarySkillType.MINING, 30);
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.MELEE);

        SkillresetTestAccess.reset(player, profile, PrimarySkillType.MINING);

        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isZero();
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(SkillCategory.MELEE);
    }
}
