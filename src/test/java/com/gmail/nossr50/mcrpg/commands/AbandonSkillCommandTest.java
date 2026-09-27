package com.gmail.nossr50.mcrpg.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelDownEvent;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * /abandonskill only warns on the first run. The player must repeat it with "confirm" within
 * the timeout, and only then does the skill keep 10% of its total XP and leave its slot.
 */
class AbandonSkillCommandTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(AbandonSkillCommandTest.class.getName());

    private final Command command = mock(Command.class);
    private long now;
    private PlayerProfile profile;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        AbandonSkillCommand.clearPendingConfirmations();
        now = 1_000_000L;
        AbandonSkillCommand.clock = () -> now;

        // Woodcutting 40 is the player's Secondary skill
        profile = mmoPlayer.getProfile();
        profile.modifySkill(PrimarySkillType.WOODCUTTING, 40);
        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.WOODCUTTING);
    }

    @AfterEach
    void tearDown() {
        AbandonSkillCommand.clock = System::currentTimeMillis;
        AbandonSkillCommand.clearPendingConfirmations();
        cleanUpStaticMocks();
    }

    private void run(String... args) {
        new AbandonSkillCommand().onCommand(player, command, "abandonskill", args);
    }

    private void assertUnchanged() {
        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isEqualTo(40);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.WOODCUTTING);
    }

    private void assertAbandoned() {
        assertThat(profile.getSkillLevel(PrimarySkillType.WOODCUTTING)).isEqualTo(9);
        assertThat(profile.getSkillXpLevelRaw(PrimarySkillType.WOODCUTTING)).isEqualTo(27_500F);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();
    }

    @Test
    void firstRunShouldOnlyWarn() {
        run("woodcutting");

        assertUnchanged();
        verify(player, atLeastOnce()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void confirmingWithinTheTimeoutShouldAbandon() {
        run("woodcutting");
        now += 29_000L;
        run("woodcutting", "confirm");

        assertAbandoned();
    }

    @Test
    void confirmingWithoutAWarningShouldOnlyWarn() {
        run("woodcutting", "confirm");

        assertUnchanged();

        // ...and that warning can then be confirmed
        run("woodcutting", "confirm");
        assertAbandoned();
    }

    @Test
    void confirmingAfterTheTimeoutShouldWarnAgain() {
        run("woodcutting");
        now += 31_000L;
        run("woodcutting", "confirm");

        assertUnchanged();
    }

    @Test
    void confirmationShouldOnlyApplyToTheWarnedSkill() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        run("mining");
        run("woodcutting", "confirm");

        assertUnchanged();
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.MINING);
    }

    @Test
    void skillsNotInASlotCannotBeAbandoned() {
        profile.modifySkill(PrimarySkillType.FISHING, 20);

        run("fishing");
        run("fishing", "confirm");

        assertThat(profile.getSkillLevel(PrimarySkillType.FISHING)).isEqualTo(20);
    }

    @Test
    void anotherPluginCancellingTheLevelLossShouldLeaveEverythingAsItWas() {
        doAnswer(invocation -> {
            final Event event = invocation.getArgument(0);
            if (event instanceof McMMOPlayerLevelDownEvent levelDown) {
                levelDown.setCancelled(true);
            }
            return event;
        }).when(pluginManager).callEvent(any(Event.class));

        run("woodcutting");
        run("woodcutting", "confirm");

        assertUnchanged();
    }

    @Test
    void abandoningShouldFireALevelDownEventForTheLevelsLost() {
        run("woodcutting");
        run("woodcutting", "confirm");

        verify(pluginManager).callEvent(org.mockito.ArgumentMatchers.argThat(event ->
                event instanceof McMMOPlayerLevelDownEvent levelDown
                        && levelDown.getSkill() == PrimarySkillType.WOODCUTTING
                        && levelDown.getLevelsLost() == 31));
    }
}
