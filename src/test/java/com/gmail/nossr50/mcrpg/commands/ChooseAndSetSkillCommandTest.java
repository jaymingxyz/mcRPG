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
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import java.util.logging.Logger;
import com.gmail.nossr50.mcrpg.gui.GuiTestSupport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** /chooseskill fills empty slots; /rpgsetskill lets admins change slots without resets. */
class ChooseAndSetSkillCommandTest extends MMOTestEnvironment {
    private static final Logger logger =
            Logger.getLogger(ChooseAndSetSkillCommandTest.class.getName());

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
        new ChooseSkillCommand().onCommand(player, command, "chooseskill", args);
    }

    // --- /chooseskill ---

    @Test
    void chooseShouldFillAnEmptySlot() {
        choose("primary", "mining");
        choose("secondary", "Smelting");

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.MINING);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.SMELTING);
    }

    @Test
    void chooseShouldNotReplaceAFilledSlot() {
        choose("primary", "mining");
        choose("primary", "swords");

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.MINING);
    }

    @Test
    void chooseShouldRequireTheSkillPermission() {
        when(Permissions.skillEnabled(player, PrimarySkillType.MINING)).thenReturn(false);

        choose("primary", "mining");

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
    }

    @Test
    void chooseShouldIgnoreInvalidSlotsAndSkills() {
        choose("tertiary", "mining");
        choose("primary", "notaskill");

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
    void tabCompletionShouldOfferSlotsThenSkills() {
        final ChooseSkillCommand chooseSkill = new ChooseSkillCommand();

        assertThat(chooseSkill.onTabComplete(player, command, "chooseskill",
                new String[] {"p"})).containsExactly("primary");
        assertThat(chooseSkill.onTabComplete(player, command, "chooseskill",
                new String[] {"primary", "sm"})).containsExactly("smelting");
    }

    // --- /rpgsetskill ---

    private void setSkill(String... args) {
        final CommandSender admin = mock(CommandSender.class);
        when(UserManager.getOfflinePlayer("testPlayer")).thenReturn(mmoPlayer);
        new SetSkillCommand().onCommand(admin, command, "rpgsetskill", args);
    }

    @Test
    void setSkillShouldReplaceAFilledSlotWithoutChangingProgress() {
        profile.modifySkill(PrimarySkillType.MINING, 50);
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);

        setSkill("testPlayer", "primary", "swords");

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.SWORDS);
        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isEqualTo(50);
    }

    @Test
    void setSkillNoneShouldEmptyTheSlot() {
        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.FISHING);

        setSkill("testPlayer", "secondary", "none");

        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }

    @Test
    void setSkillShouldNotPutTheSameSkillInBothSlots() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);

        setSkill("testPlayer", "secondary", "mining");

        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }
}
