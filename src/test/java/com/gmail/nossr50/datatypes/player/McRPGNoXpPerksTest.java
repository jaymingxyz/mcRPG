package com.gmail.nossr50.datatypes.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.placeholders.PapiExpansion;
import com.gmail.nossr50.util.Permissions;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * mcRPG has no XP perks. Even if a permissions plugin grants one of mcMMO's old
 * {@code mcrpg.perks.xp.*} nodes by hand, it must not change how much XP a player earns or the
 * XP rate a placeholder reports.
 */
class McRPGNoXpPerksTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(McRPGNoXpPerksTest.class.getName());
    private static final PrimarySkillType SKILL = PrimarySkillType.MINING;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);

        // Neutral multipliers and no caps, so any change to the XP could only come from a perk
        when(generalConfig.getPowerLevelCap()).thenReturn(Integer.MAX_VALUE);
        when(generalConfig.getLevelCap(SKILL)).thenReturn(10000);
        when(ExperienceConfig.getInstance().getFormulaSkillModifier(SKILL)).thenReturn(1.0);
        when(ExperienceConfig.getInstance().getExperienceGainsMultiplier(SKILL)).thenReturn(1.0);
        when(ExperienceConfig.getInstance().getCustomXpPerkBoost()).thenReturn(1.25);

        grantEveryXpPerk();
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    private void grantEveryXpPerk() {
        when(Permissions.customXpBoost(player, SKILL)).thenReturn(true);
        when(Permissions.quadrupleXp(player, SKILL)).thenReturn(true);
        when(Permissions.tripleXp(player, SKILL)).thenReturn(true);
        when(Permissions.doubleAndOneHalfXp(player, SKILL)).thenReturn(true);
        when(Permissions.doubleXp(player, SKILL)).thenReturn(true);
        when(Permissions.oneAndOneHalfXp(player, SKILL)).thenReturn(true);
        when(Permissions.oneAndOneTenthXp(player, SKILL)).thenReturn(true);
    }

    @Test
    void modifyXpGainShouldIgnoreXpPerkPermissions() {
        // When - XP is earned by a player holding every XP perk node
        final float modifiedXp = mmoPlayer.modifyXpGain(SKILL, 10F);

        // Then - the XP is unchanged
        assertThat(modifiedXp).isEqualTo(10F);
    }

    @Test
    void skillXpRatePlaceholderShouldIgnoreXpPerkPermissions() {
        // Given - the PlaceholderAPI expansion, with the settings its constructor reads
        when(generalConfig.getPapiLeaderboardMaxTrackedRank()).thenReturn(100);
        when(generalConfig.getLeaderboardRefreshIntervalSecondsFlatFile()).thenReturn(600);
        final PapiExpansion expansion = new PapiExpansion();

        // When - %mcmmo_xprate_mining% is resolved for a player holding every XP perk node
        final String rate = expansion.getSkillXpRate(SKILL, player);

        // Then - it reports no personal XP modifier
        assertThat(rate).isEqualTo("1.0");
    }
}
