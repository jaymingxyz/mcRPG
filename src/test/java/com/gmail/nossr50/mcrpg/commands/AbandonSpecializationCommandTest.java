package com.gmail.nossr50.mcrpg.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelDownEvent;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * /abandonspecialization only warns on the first run. The player must repeat it with "confirm"
 * within the timeout, and only then does every skill in the category keep 10% of its total XP
 * and the Specialization clear.
 */
class AbandonSpecializationCommandTest extends MMOTestEnvironment {
    private static final Logger logger =
            Logger.getLogger(AbandonSpecializationCommandTest.class.getName());

    private final Command command = mock(Command.class);
    private long now;
    private PlayerProfile profile;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        AbandonSpecializationCommand.clearPendingConfirmations();
        now = 1_000_000L;
        AbandonSpecializationCommand.clock = () -> now;

        // Botany is the player's Secondary Specialization: Woodcutting 40, Herbalism 10
        profile = mmoPlayer.getProfile();
        profile.modifySkill(PrimarySkillType.WOODCUTTING, 40);
        profile.modifySkill(PrimarySkillType.HERBALISM, 10);
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.BOTANY);
    }

    @AfterEach
    void tearDown() {
        AbandonSpecializationCommand.clock = System::currentTimeMillis;
        AbandonSpecializationCommand.clearPendingConfirmations();
        cleanUpStaticMocks();
    }

    private void run(String... args) {
        new AbandonSpecializationCommand().onCommand(player, command, "abandonspecialization",
                args);
    }

    private void assertUnchanged() {
        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isEqualTo(40);
        assertThat(profile.getSkillLevel(PrimarySkillType.HERBALISM)).isEqualTo(10);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(SkillCategory.BOTANY);
    }

    private void assertAbandoned() {
        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isEqualTo(9);
        assertThat(profile.getSkillXpLevelRaw(PrimarySkillType.WOODCUTTING)).isEqualTo(27_500F);
        assertThat(profile.getSkillLevel(PrimarySkillType.HERBALISM)).isEqualTo(1);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }

    @Test
    void firstRunShouldOnlyWarn() {
        run("botany");

        assertUnchanged();
        verify(player, atLeastOnce()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void confirmingWithinTheTimeoutShouldCutEverySkillInTheCategory() {
        run("botany");
        now += 29_000L;
        run("botany", "confirm");

        assertAbandoned();
    }

    @Test
    void theSpecializationCanBeNamedByPrimaryOrSecondary() {
        run("secondary");
        run("secondary", "confirm");

        assertAbandoned();
    }

    @Test
    void confirmingWithoutAWarningShouldOnlyWarn() {
        run("botany", "confirm");

        assertUnchanged();

        // ...and that warning can then be confirmed
        run("botany", "confirm");
        assertAbandoned();
    }

    @Test
    void confirmingAfterTheTimeoutShouldWarnAgain() {
        run("botany");
        now += 31_000L;
        run("botany", "confirm");

        assertUnchanged();
    }

    @Test
    void confirmationShouldOnlyApplyToTheWarnedCategory() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.METALLURGY);
        run("metallurgy");
        run("botany", "confirm");

        assertUnchanged();
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(SkillCategory.METALLURGY);
    }

    @Test
    void categoriesThatArentSpecializationsCannotBeAbandoned() {
        profile.modifySkill(PrimarySkillType.FISHING, 20);

        run("survivalism");
        run("survivalism", "confirm");
        run("primary");
        run("primary", "confirm");
        run("mining");
        run("mining", "confirm");

        assertThat(profile.getSkillLevel(PrimarySkillType.FISHING)).isEqualTo(20);
        assertUnchanged();
    }

    @Test
    void anotherPluginCancellingAnySkillsLevelLossShouldLeaveEverythingAsItWas() {
        // Only Herbalism's level loss is vetoed, so Woodcutting must not change either
        doAnswer(invocation -> {
            final Event event = invocation.getArgument(0);
            if (event instanceof McMMOPlayerLevelDownEvent levelDown
                    && levelDown.getSkill() == PrimarySkillType.HERBALISM) {
                levelDown.setCancelled(true);
            }
            return event;
        }).when(pluginManager).callEvent(any(Event.class));

        run("botany");
        run("botany", "confirm");

        assertUnchanged();
    }

    @Test
    void abandoningShouldFireALevelDownEventForEachSkillThatLosesLevels() {
        run("botany");
        run("botany", "confirm");

        verify(pluginManager).callEvent(org.mockito.ArgumentMatchers.argThat(event ->
                event instanceof McMMOPlayerLevelDownEvent levelDown
                        && levelDown.getSkill() == PrimarySkillType.WOODCUTTING
                        && levelDown.getLevelsLost() == 31));
        verify(pluginManager).callEvent(org.mockito.ArgumentMatchers.argThat(event ->
                event instanceof McMMOPlayerLevelDownEvent levelDown
                        && levelDown.getSkill() == PrimarySkillType.HERBALISM
                        && levelDown.getLevelsLost() == 9));
        // Alchemy is level 0, so it loses nothing and needs no event
        verify(pluginManager, never()).callEvent(org.mockito.ArgumentMatchers.argThat(event ->
                event instanceof McMMOPlayerLevelDownEvent levelDown
                        && levelDown.getSkill() == PrimarySkillType.ALCHEMY));
    }
}
