package com.gmail.nossr50.mcrpg.passive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** What players are told about category passives. */
class CategoryPassiveDisplayTest extends MMOTestEnvironment {
    private static final Logger logger =
            Logger.getLogger(CategoryPassiveDisplayTest.class.getName());

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
        return text == null ? null : ChatColor.stripColor(text);
    }

    @Test
    void effectsShouldShowTheConfiguredNumbers() {
        assertThat(CategoryPassiveDisplay.effect(CategoryPassive.LEATHER_MASTERY))
                .isEqualTo("Each piece of leather armor you wear cuts damage taken by 8%, "
                        + "and leather armor loses 50% less durability");
        assertThat(CategoryPassiveDisplay.effect(CategoryPassive.WOODEN_MASTERY))
                .isEqualTo("Wooden tools mine 50% faster and lose 50% less durability");
    }

    @Test
    void statsShouldShowWhatALockedPassiveNeeds() {
        profile.modifySkill(PrimarySkillType.MINING, 12);
        assertThat(plain(CategoryPassiveDisplay.statsLine(profile, SkillCategory.METALLURGY)))
                .isEqualTo("Passive: Copper Mastery (locked) - needs Metallurgy as a "
                        + "Specialization and 25 combined levels (now 12)");

        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        assertThat(plain(CategoryPassiveDisplay.statsLine(profile, SkillCategory.METALLURGY)))
                .isEqualTo("Passive: Copper Mastery (locked) - needs 25 combined levels "
                        + "(now 12)");

        profile.setSpecialization(SpecializationSlot.PRIMARY, null);
        profile.modifySkill(PrimarySkillType.MINING, 30);
        assertThat(plain(CategoryPassiveDisplay.statsLine(profile, SkillCategory.METALLURGY)))
                .isEqualTo("Passive: Copper Mastery (locked) - needs Metallurgy as a "
                        + "Specialization");
    }

    @Test
    void statsShouldShowAnActivePassiveAndWhatItDoes() {
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.BOTANY);
        profile.modifySkill(PrimarySkillType.ALCHEMY, 25);

        assertThat(plain(CategoryPassiveDisplay.statsLine(profile, SkillCategory.BOTANY)))
                .isEqualTo("Passive: Wooden Mastery - Wooden tools mine 50% faster and lose "
                        + "50% less durability");
    }

    @Test
    void categoriesWithoutAPassiveOrWithPassivesOffShouldShowNothing() {
        assertThat(CategoryPassiveDisplay.statsLine(profile, SkillCategory.MELEE)).isNull();
        assertThat(CategoryPassiveDisplay.menuLore(profile, SkillCategory.RANGED)).isEmpty();

        when(generalConfig.getCategoryPassivesEnabled()).thenReturn(false);
        assertThat(CategoryPassiveDisplay.statsLine(profile, SkillCategory.BOTANY)).isNull();
        assertThat(CategoryPassiveDisplay.menuLore(profile, SkillCategory.BOTANY)).isEmpty();
        assertThat(CategoryPassiveDisplay.chooseMessage(profile, SkillCategory.BOTANY))
                .isNull();
    }

    @Test
    void menuLoreShouldWrapLongLines() {
        final List<String> lore =
                CategoryPassiveDisplay.menuLore(profile, SkillCategory.SURVIVALISM);

        assertThat(plain(lore.get(0))).isEqualTo("Passive: Leather Mastery");
        assertThat(lore).allSatisfy(line -> assertThat(plain(line).length())
                .isLessThanOrEqualTo(40));
        assertThat(String.join(" ", lore.stream().map(ChatColor::stripColor).toList()))
                .isEqualTo("Passive: Leather Mastery Each piece of leather armor you wear "
                        + "cuts damage taken by 8%, and leather armor loses 50% less "
                        + "durability Locked: needs Survivalism as a Specialization and 25 "
                        + "combined levels (now 0)");
    }

    @Test
    void choosingACategoryShouldSayWhetherItsPassiveIsActive() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.BLACKSMITHING);
        profile.modifySkill(PrimarySkillType.REPAIR, 10);
        assertThat(plain(CategoryPassiveDisplay.chooseMessage(profile,
                SkillCategory.BLACKSMITHING)))
                .isEqualTo("Chainmail Mastery unlocks when your Blacksmithing skills reach 25 "
                        + "combined levels (now 10).");

        profile.modifySkill(PrimarySkillType.SALVAGE, 15);
        assertThat(plain(CategoryPassiveDisplay.chooseMessage(profile,
                SkillCategory.BLACKSMITHING))).startsWith("Chainmail Mastery unlocked!");
        assertThat(CategoryPassiveDisplay.chooseMessage(profile, SkillCategory.MELEE)).isNull();
    }

    @Test
    void abandonWarningShouldOnlyMentionAnActivePassive() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.SURVIVALISM);
        assertThat(CategoryPassiveDisplay.abandonWarning(profile, SkillCategory.SURVIVALISM))
                .isNull();

        profile.modifySkill(PrimarySkillType.TAMING, 25);
        assertThat(plain(CategoryPassiveDisplay.abandonWarning(profile,
                SkillCategory.SURVIVALISM))).isEqualTo("You will also lose Leather Mastery.");
    }
}
