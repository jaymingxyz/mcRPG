package com.gmail.nossr50.datatypes.player;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * In mcMMO, XP earned in Salvage or Smelting was split between their parent skills (Repair and
 * Fishing, or Mining and Repair). In mcRPG they are standalone skills, so the XP must go to
 * the skill that earned it.
 */
class McRPGStandaloneSkillXpTest extends MMOTestEnvironment {
    private static final Logger logger =
            Logger.getLogger(McRPGStandaloneSkillXpTest.class.getName());

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        when(generalConfig.getPowerLevelCap()).thenReturn(Integer.MAX_VALUE);
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            when(generalConfig.getLevelCap(skill)).thenReturn(Integer.MAX_VALUE);
            when(ExperienceConfig.getInstance().getExperienceGainsMultiplier(skill))
                    .thenReturn(1.0);
        }
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    @ParameterizedTest
    @EnumSource(value = PrimarySkillType.class, names = {"SALVAGE", "SMELTING"})
    void xpEarnedInSalvageOrSmeltingShouldGoToThatSkillOnly(PrimarySkillType skill) {
        // When - the player earns XP in Salvage or Smelting
        mmoPlayer.beginXpGain(skill, 10F, XPGainReason.PVE, XPGainSource.SELF);

        // Then - the full amount is applied to that skill...
        verify(mmoPlayer).applyXpGain(eq(skill), eq(10F), any(XPGainReason.class),
                any(XPGainSource.class));

        // ...and none of it is split off to mcMMO's former parent skills
        for (PrimarySkillType formerParent : new PrimarySkillType[] {PrimarySkillType.MINING,
                PrimarySkillType.REPAIR, PrimarySkillType.FISHING}) {
            verify(mmoPlayer, never()).applyXpGain(eq(formerParent), anyFloat(),
                    any(XPGainReason.class), any(XPGainSource.class));
        }
    }
}
