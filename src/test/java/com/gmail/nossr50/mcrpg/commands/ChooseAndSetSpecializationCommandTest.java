package com.gmail.nossr50.mcrpg.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.gui.GuiTestSupport;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * /choosespecialization chooses a category as a Specialization; /rpgsetspecialization lets
 * admins change Specializations without resetting any skills.
 */
class ChooseAndSetSpecializationCommandTest extends MMOTestEnvironment {
    private static final Logger logger =
            Logger.getLogger(ChooseAndSetSpecializationCommandTest.class.getName());

    private final Command command = mock(Command.class);
    private PlayerProfile profile;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        when(Permissions.skillEnabled(any(), any(PrimarySkillType.class))).thenReturn(true);
        profile = mmoPlayer.getProfile();
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    private void choose(String... args) {
        new ChooseSpecializationCommand().onCommand(player, command, "choosespecialization",
                args);
    }

    // --- /choosespecialization ---

    @Test
    void chooseShouldSetBothSpecializations() {
        choose("primary", "metallurgy");
        choose("secondary", "Botany");

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.METALLURGY);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(SkillCategory.BOTANY);
    }

    @Test
    void chooseShouldNotReplaceAChosenSpecialization() {
        choose("primary", "metallurgy");
        choose("primary", "melee");

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.METALLURGY);
    }

    @Test
    void chooseShouldNeedPermissionForAtLeastOneSkillInTheCategory() {
        // Mining alone is off: Smelting and Excavation still allow Metallurgy
        when(Permissions.skillEnabled(player, PrimarySkillType.MINING)).thenReturn(false);
        choose("primary", "metallurgy");
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.METALLURGY);

        // Both Blacksmithing skills are off
        when(Permissions.skillEnabled(player, PrimarySkillType.REPAIR)).thenReturn(false);
        when(Permissions.skillEnabled(player, PrimarySkillType.SALVAGE)).thenReturn(false);
        choose("secondary", "blacksmithing");
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }

    @Test
    void chooseShouldIgnoreInvalidSlotsAndCategories() {
        choose("tertiary", "metallurgy");
        choose("primary", "mining");
        choose("primary", "gathering");

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }

    @Test
    void chooseWithNoArgumentsShouldOpenTheMenu() {
        final Inventory menu = GuiTestSupport.prepareMenus();
        try {
            choose();

            verify(player).openInventory(menu);
        } finally {
            GuiTestSupport.reset();
        }
    }

    @Test
    void tabCompletionShouldOfferSpecializationsThenCategories() {
        final ChooseSpecializationCommand chooseCommand = new ChooseSpecializationCommand();

        assertThat(chooseCommand.onTabComplete(player, command, "csp",
                new String[] {"p"})).containsExactly("primary");
        assertThat(chooseCommand.onTabComplete(player, command, "csp",
                new String[] {"primary", "b"})).containsExactly("botany", "blacksmithing");
    }

    // --- /rpgsetspecialization ---

    private void setSpecialization(String... args) {
        final CommandSender admin = mock(CommandSender.class);
        when(UserManager.getOfflinePlayer("testPlayer")).thenReturn(mmoPlayer);
        new SetSpecializationCommand().onCommand(admin, command, "rpgsetspecialization", args);
    }

    @Test
    void setShouldReplaceAChosenSpecializationWithoutChangingProgress() {
        profile.modifySkill(PrimarySkillType.MINING, 50);
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);

        setSpecialization("testPlayer", "primary", "melee");

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.MELEE);
        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isEqualTo(50);
    }

    @Test
    void setNoneShouldClearTheSpecialization() {
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.SURVIVALISM);

        setSpecialization("testPlayer", "secondary", "none");

        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }

    @Test
    void setShouldNotMakeOneCategoryBothSpecializations() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);

        setSpecialization("testPlayer", "secondary", "metallurgy");
        setSpecialization("testPlayer", "secondary", "mining");

        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }
}
