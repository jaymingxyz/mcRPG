package com.gmail.nossr50.commands.experience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * /rpgaddxp, /rpgaddlevels, /rpgsetlevel and /rpgskillreset also take a category, such as
 * blacksmithing, and apply to each of its skills.
 */
class McRPGCategoryCommandsTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(McRPGCategoryCommandsTest.class.getName());

    private CommandSender sender;
    private Command command;
    private PlayerProfile profile;

    @BeforeEach
    void setUp() {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        sender = mock(CommandSender.class);
        command = mock(Command.class);
        when(UserManager.getOfflinePlayer("testPlayer")).thenReturn(mmoPlayer);
        profile = mmoPlayer.getProfile();
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    private String senderMessage() {
        final ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(message.capture());
        return ChatColor.stripColor(message.getValue());
    }

    @Test
    void addXpToACategoryShouldAwardEachOfItsSkills() {
        when(Permissions.addxpOthers(sender)).thenReturn(true);

        new AddxpCommand().onCommand(sender, command, "rpgaddxp",
                new String[]{"testPlayer", "blacksmithing", "1000"});

        verify(mmoPlayer).applyXpGain(PrimarySkillType.REPAIR, 1000F, XPGainReason.COMMAND,
                XPGainSource.COMMAND);
        verify(mmoPlayer).applyXpGain(PrimarySkillType.SALVAGE, 1000F, XPGainReason.COMMAND,
                XPGainSource.COMMAND);
        verify(mmoPlayer, never()).applyXpGain(eq(PrimarySkillType.MINING), anyFloat(),
                any(), any());
        assertThat(senderMessage()).isEqualTo("Blacksmithing has been modified for testPlayer.");
    }

    @Test
    void addLevelsToACategoryShouldAddAndRemoveLevelsInEachOfItsSkills() {
        when(Permissions.addlevelsOthers(sender)).thenReturn(true);
        profile.modifySkill(PrimarySkillType.TAMING, 10);
        profile.modifySkill(PrimarySkillType.FISHING, 2);

        new AddlevelsCommand().onCommand(sender, command, "rpgaddlevels",
                new String[]{"testPlayer", "survivalism", "5"});
        assertThat(profile.getSkillLevel(PrimarySkillType.TAMING)).isEqualTo(15);
        assertThat(profile.getSkillLevel(PrimarySkillType.ACROBATICS)).isEqualTo(5);
        assertThat(profile.getSkillLevel(PrimarySkillType.FISHING)).isEqualTo(7);
        assertThat(profile.getSkillLevel(PrimarySkillType.UNARMED)).isEqualTo(5);

        // A negative amount removes levels, never going below 0
        new AddlevelsCommand().onCommand(sender, command, "rpgaddlevels",
                new String[]{"testPlayer", "Survivalism", "-6", "-s"});
        assertThat(profile.getSkillLevel(PrimarySkillType.TAMING)).isEqualTo(9);
        assertThat(profile.getSkillLevel(PrimarySkillType.FISHING)).isEqualTo(1);
        assertThat(profile.getSkillLevel(PrimarySkillType.ACROBATICS)).isZero();
        assertThat(profile.getSkillLevel(PrimarySkillType.SWORDS)).isZero();
    }

    @Test
    void setLevelForACategoryShouldOnlySetItsSkills() {
        when(Permissions.mmoeditOthers(sender)).thenReturn(true);
        profile.modifySkill(PrimarySkillType.WOODCUTTING, 3);

        new MmoeditCommand().onCommand(sender, command, "rpgsetlevel",
                new String[]{"testPlayer", "metallurgy", "20"});

        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isEqualTo(20);
        assertThat(profile.getSkillLevel(PrimarySkillType.SMELTING)).isEqualTo(20);
        assertThat(profile.getSkillLevel(PrimarySkillType.EXCAVATION)).isEqualTo(20);
        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isEqualTo(3);
    }

    @Test
    void skillResetForACategoryShouldResetItsSkillsAndClearItsSpecialization() {
        when(Permissions.skillresetOthers(sender)).thenReturn(true);
        profile.modifySkill(PrimarySkillType.WOODCUTTING, 30);
        profile.modifySkill(PrimarySkillType.ALCHEMY, 12);
        profile.modifySkill(PrimarySkillType.MINING, 40);
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.BOTANY);
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.METALLURGY);

        new SkillresetCommand().onCommand(sender, command, "rpgskillreset",
                new String[]{"testPlayer", "botany"});

        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isZero();
        assertThat(profile.getSkillLevel(PrimarySkillType.ALCHEMY)).isZero();
        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isEqualTo(40);
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(SkillCategory.METALLURGY);
        assertThat(senderMessage()).isEqualTo("Botany has been modified for testPlayer.");
    }

    @Test
    void aCategoryOnYourselfShouldNotChangeEverySkill() {
        // Self form: /rpgaddlevels <category> <levels>. A category isn't a skill, so it must
        // not fall through to mcMMO's "every skill" path
        when(player.getName()).thenReturn("testPlayer");
        when(UserManager.getPlayer("testPlayer")).thenReturn(mmoPlayer);
        when(Permissions.addlevels(player)).thenReturn(true);

        new AddlevelsCommand().onCommand(player, command, "rpgaddlevels",
                new String[]{"botany", "3"});

        assertThat(profile.getSkillLevel(PrimarySkillType.HERBALISM)).isEqualTo(3);
        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isZero();
        assertThat(profile.getSkillLevel(PrimarySkillType.SWORDS)).isZero();
    }

    @Test
    void anythingElseShouldStillBeRejected() {
        when(Permissions.addlevelsOthers(sender)).thenReturn(true);

        new AddlevelsCommand().onCommand(sender, command, "rpgaddlevels",
                new String[]{"testPlayer", "gathering", "5"});

        assertThat(senderMessage()).isEqualTo("That is not a valid skillname!");
        assertThat(profile.getSkillLevel(PrimarySkillType.MINING)).isZero();
    }

    @Test
    void tabCompletionShouldOfferCategoriesAfterSkills() {
        assertThat(new AddxpCommand().onTabComplete(sender, command, "rpgaddxp",
                new String[]{"testPlayer", "b"})).contains("botany", "blacksmithing");
        assertThat(new SkillresetCommand().onTabComplete(sender, command, "rpgskillreset",
                new String[]{"testPlayer", "met"})).containsExactly("metallurgy");
        // Self form: after a category comes the amount, so nothing is offered
        assertThat(new AddxpCommand().onTabComplete(sender, command, "rpgaddxp",
                new String[]{"blacksmithing", ""})).isEmpty();
    }
}
