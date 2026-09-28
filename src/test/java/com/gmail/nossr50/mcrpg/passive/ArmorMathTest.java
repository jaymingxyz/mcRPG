package com.gmail.nossr50.mcrpg.passive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/**
 * Minecraft's armor formula, and how full mastered sets compare to vanilla sets with the
 * default bonuses. Vanilla full sets (armor, toughness): leather 7/0, copper 10/0, chainmail
 * 12/0, iron 15/0, diamond 20/8, netherite 20/12.
 */
class ArmorMathTest {
    /** Hits from half a heart to 50 damage, a range wide enough for any mob or weapon. */
    private static final double[] HITS = {0.5, 1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 25, 30, 40,
            50};

    private static double taken(double damage, double armor, double toughness) {
        return ArmorMath.damageAfterArmor(damage, armor, toughness);
    }

    /** Damage taken through a full mastered set, the way the listener applies it. */
    private static double mastered(double damage, double armor, CategoryPassive passive) {
        final double reachingArmor = ArmorMath.equivalentDamage(damage, armor, 0,
                passive.defaultBonusArmor(), passive.defaultBonusToughness());
        return taken(reachingArmor, armor, 0);
    }

    @Test
    void damageAfterArmorShouldMatchMinecraft() {
        // Iron against a 10 damage hit: 15 - 10 / 2 = 10 effective points, 40% blocked
        assertThat(taken(10, 15, 0)).isCloseTo(6.0, within(1e-9));
        // Diamond: 20 - 10 / 4 = 17.5 effective points, 70% blocked
        assertThat(taken(10, 20, 8)).isCloseTo(3.0, within(1e-9));
        // Huge hits still meet a fifth of the armor: 20% of 15 points is 12% blocked
        assertThat(taken(100, 15, 0)).isCloseTo(88.0, within(1e-9));
        assertThat(taken(10, 0, 0)).isEqualTo(10.0);
    }

    @Test
    void equivalentDamageShouldLeaveWhatTheBoostedArmorWould() {
        for (double hit : HITS) {
            final double reachingArmor = ArmorMath.equivalentDamage(hit, 12, 0, 8, 9);
            assertThat(reachingArmor).isLessThanOrEqualTo(hit);
            assertThat(taken(reachingArmor, 12, 0)).as("hit %s", hit)
                    .isCloseTo(taken(hit, 20, 9), within(1e-9));
        }
        assertThat(ArmorMath.equivalentDamage(10, 12, 0, 0, 0)).isEqualTo(10);
    }

    @Test
    void boostedArmorShouldStopAtMinecraftsCaps() {
        assertThat(ArmorMath.equivalentDamage(10, 28, 18, 8, 9))
                .isEqualTo(ArmorMath.equivalentDamage(10, 28, 18, 2, 2));
    }

    @Test
    void masteredChainmailShouldBeABitBetterThanDiamondButWorseThanNetherite() {
        for (double hit : HITS) {
            final double chainmail = mastered(hit, 12, CategoryPassive.CHAINMAIL_MASTERY);
            assertThat(chainmail).as("hit %s", hit)
                    .isLessThan(taken(hit, 20, 8))
                    .isGreaterThan(taken(hit, 20, 12));
        }
    }

    @Test
    void masteredLeatherShouldBeABitBetterThanIronButWorseThanDiamond() {
        for (double hit : HITS) {
            final double leather = mastered(hit, 7, CategoryPassive.LEATHER_MASTERY);
            assertThat(leather).as("hit %s", hit)
                    .isLessThan(taken(hit, 15, 0))
                    .isGreaterThan(taken(hit, 20, 8));
        }
    }

    @Test
    void masteredCopperShouldMatchIron() {
        for (double hit : HITS) {
            assertThat(mastered(hit, 10, CategoryPassive.COPPER_MASTERY)).as("hit %s", hit)
                    .isCloseTo(taken(hit, 15, 0), within(1e-9));
        }
    }
}
