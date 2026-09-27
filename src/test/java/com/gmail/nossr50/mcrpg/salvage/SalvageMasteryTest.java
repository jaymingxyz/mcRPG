package com.gmail.nossr50.mcrpg.salvage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.random.ProbabilityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Salvage Mastery: the share of materials lost to damage a player gets back rises evenly to
 * 50% at level 100, whole materials are guaranteed, and a leftover fraction is a chance at one
 * more.
 */
class SalvageMasteryTest {
    private static final double MAX_PERCENT = 50D;
    private static final int MAX_LEVEL = 100;

    private static double bonus(int lostMaterials, int level) {
        return SalvageMastery.bonusMaterials(lostMaterials, level, MAX_PERCENT, MAX_LEVEL);
    }

    @ParameterizedTest(name = "{0} at level {2} -> {3}")
    @CsvSource({
            // item, materials lost to damage, Salvage level, bonus materials
            "anything, 8, 0, 0",
            "broken chestplate, 8, 24, 0.96",
            "broken chestplate, 8, 25, 1",
            "broken chestplate, 8, 100, 4",
            "broken helmet, 5, 40, 1",
            "broken boots, 4, 50, 1",
            "broken pickaxe, 3, 66, 0.99",
            "broken pickaxe, 3, 67, 1.005",
            "broken pickaxe, 3, 100, 1.5",
            "broken sword, 2, 99, 0.99",
            "broken sword, 2, 100, 1",
            "broken shovel, 1, 100, 0.5",
            "broken chestplate past the max level, 8, 500, 4",
            "undamaged item, 0, 100, 0"
    })
    void bonusShouldRiseEvenlyToHalfTheLostMaterials(String item, int lost, int level,
            double expected) {
        assertThat(bonus(lost, level)).isCloseTo(expected, within(1e-9));
    }

    @Test
    void thresholdLevelsShouldGiveExactlyOneWholeMaterial() {
        // A result of 0.999... would floor to nothing and refuse the salvage
        assertThat(bonus(5, 40)).isEqualTo(1D);
        assertThat(bonus(4, 50)).isEqualTo(1D);
        assertThat(bonus(8, 25)).isEqualTo(1D);
        assertThat(bonus(2, 100)).isEqualTo(1D);
    }

    @ParameterizedTest(name = "max {0}%")
    @ValueSource(doubles = {50D, 99.9D})
    void damagedItemsShouldNeverBeFullyRefundedForSure(double maxPercent) {
        for (int lost = 1; lost <= 8; lost++) {
            for (int level = 0; level <= 1000; level++) {
                final double bonus = SalvageMastery.bonusMaterials(lost, level, maxPercent,
                        MAX_LEVEL);
                assertThat((int) bonus).as("guaranteed, %d lost at level %d", lost, level)
                        .isLessThan(lost);
                assertThat(Math.ceil(bonus)).as("best roll, %d lost at level %d", lost, level)
                        .isLessThanOrEqualTo(lost);
            }
        }
    }

    @Test
    void misconfiguredValuesShouldGiveNoBonus() {
        assertThat(SalvageMastery.bonusMaterials(8, 100, 0D, MAX_LEVEL)).isZero();
        assertThat(SalvageMastery.bonusMaterials(8, 100, -10D, MAX_LEVEL)).isZero();
        assertThat(SalvageMastery.bonusMaterials(8, 100, MAX_PERCENT, 0)).isZero();
        assertThat(SalvageMastery.bonusMaterials(-2, 100, MAX_PERCENT, MAX_LEVEL)).isZero();
    }

    @Test
    void leftoverFractionShouldBeRolledAsAChance() {
        final McMMOPlayer mmoPlayer = mock(McMMOPlayer.class);
        try (MockedStatic<ProbabilityUtil> rng = Mockito.mockStatic(ProbabilityUtil.class)) {
            rng.when(() -> ProbabilityUtil.isStaticSkillRNGSuccessful(eq(PrimarySkillType.SALVAGE),
                    eq(mmoPlayer), doubleThat(chance -> Math.abs(chance - 50D) < 1e-9)))
                    .thenReturn(true);

            assertThat(SalvageMastery.rollFraction(mmoPlayer, 1.5)).isOne();
        }
    }

    @Test
    void failedRollShouldAddNothing() {
        final McMMOPlayer mmoPlayer = mock(McMMOPlayer.class);
        try (MockedStatic<ProbabilityUtil> rng = Mockito.mockStatic(ProbabilityUtil.class)) {
            rng.when(() -> ProbabilityUtil.isStaticSkillRNGSuccessful(Mockito.any(),
                    Mockito.<McMMOPlayer>any(), anyDouble())).thenReturn(false);

            assertThat(SalvageMastery.rollFraction(mmoPlayer, 1.5)).isZero();
        }
    }

    @Test
    void wholeBonusesShouldNotRoll() {
        final McMMOPlayer mmoPlayer = mock(McMMOPlayer.class);
        try (MockedStatic<ProbabilityUtil> rng = Mockito.mockStatic(ProbabilityUtil.class)) {
            assertThat(SalvageMastery.rollFraction(mmoPlayer, 2D)).isZero();
            assertThat(SalvageMastery.rollFraction(mmoPlayer, 0D)).isZero();

            rng.verify(() -> ProbabilityUtil.isStaticSkillRNGSuccessful(Mockito.any(),
                    Mockito.<McMMOPlayer>any(), anyDouble()), never());
        }
    }
}
