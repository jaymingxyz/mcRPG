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
 * What players see about specialization: /mcstats tags, XP bar tags, the login reminder text,
 * placeholders, and /skillreset clearing a slot.
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
    void statsTagShouldShowRoleAndRate() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.SMELTING);

        assertThat(plain(SpecializationDisplay.statsTag(profile, PrimarySkillType.MINING)))
                .isEqualTo("(Primary, 1.25x XP)");
        assertThat(plain(SpecializationDisplay.statsTag(profile, PrimarySkillType.SMELTING)))
                .isEqualTo("(Secondary, 1x XP)");
        assertThat(plain(SpecializationDisplay.statsTag(profile, PrimarySkillType.FISHING)))
                .isEqualTo("(0.35x XP)");
    }

    @Test
    void xpBarTitleShouldOnlyTagChosenSkills() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);

        assertThat(SpecializationDisplay.xpBarTitle("Mining Lv.5", profile,
                PrimarySkillType.MINING)).isEqualTo("Mining Lv.5 (Primary)");
        assertThat(SpecializationDisplay.xpBarTitle("Fishing Lv.5", profile,
                PrimarySkillType.FISHING)).isEqualTo("Fishing Lv.5");
    }

    @Test
    void reminderShouldNameTheEmptySlots() {
        assertThat(plain(LoginReminder.reminderText(profile)))
                .isEqualTo("You haven't chosen your Primary and Secondary Specializations yet.");

        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        assertThat(plain(LoginReminder.reminderText(profile)))
                .isEqualTo("You haven't chosen a Secondary Specialization yet.");

        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.SWORDS);
        assertThat(LoginReminder.reminderText(profile)).isNull();
    }

    @Test
    void placeholdersShouldShowTheChosenSkills() {
        when(UserManager.getPlayer(player)).thenReturn(mmoPlayer);
        final SpecializationPlaceholder primary =
                new SpecializationPlaceholder(SpecializationSlot.PRIMARY);
        final SpecializationPlaceholder secondary =
                new SpecializationPlaceholder(SpecializationSlot.SECONDARY);
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);

        assertThat(primary.getName()).isEqualTo("primary_skill");
        assertThat(secondary.getName()).isEqualTo("secondary_skill");
        assertThat(primary.process(player, "")).isEqualTo("Mining");
        assertThat(secondary.process(player, "")).isEmpty();
    }

    @Test
    void skillResetShouldAlsoEmptyTheSlotHoldingThatSkill() {
        profile.modifySkill(PrimarySkillType.MINING, 30);
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.SWORDS);

        SkillresetTestAccess.reset(player, profile, PrimarySkillType.MINING);

        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isZero();
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.SWORDS);
    }
}
