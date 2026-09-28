package com.gmail.nossr50.mcrpg.passive;

/**
 * Minecraft's armor formula, used to give armor masteries' bonus armor points and toughness
 * without changing the player's attributes. The server works out armor from the player's
 * real attributes, so a hit is shrunk before it reaches the armor until the real armor leaves
 * the same damage the boosted armor would. Everything after armor (Resistance, Protection
 * enchantments, absorption) then works exactly as it would with the boosted armor.
 */
final class ArmorMath {
    /** Minecraft caps the armor and toughness attributes here. */
    static final double MAX_ARMOR = 30D;
    static final double MAX_TOUGHNESS = 20D;

    private static final int SEARCH_STEPS = 60;

    private ArmorMath() {
    }

    /**
     * The damage left after armor, as in Minecraft's {@code CombatRules.getDamageAfterAbsorb}.
     * Armor blocks 4% per effective point, up to 80%; a big hit lowers the effective points,
     * which toughness counters, but never below a fifth of the armor.
     */
    static double damageAfterArmor(double damage, double armor, double toughness) {
        final double toughnessFactor = 2D + toughness / 4D;
        final double effectiveArmor = Math.min(20D,
                Math.max(armor * 0.2D, armor - damage / toughnessFactor));
        return damage * (1D - effectiveArmor / 25D);
    }

    /**
     * The damage that the player's real armor turns into what their armor plus the bonus
     * would leave of {@code damage}. Never more than {@code damage}.
     */
    static double equivalentDamage(double damage, double armor, double toughness,
            double bonusArmor, double bonusToughness) {
        if (damage <= 0 || (bonusArmor <= 0 && bonusToughness <= 0)) {
            return damage;
        }
        final double target = damageAfterArmor(damage,
                Math.min(MAX_ARMOR, armor + bonusArmor),
                Math.min(MAX_TOUGHNESS, toughness + bonusToughness));
        // More damage always leaves more after armor, so a binary search finds the match
        double low = 0D;
        double high = damage;
        for (int step = 0; step < SEARCH_STEPS; step++) {
            final double middle = (low + high) / 2D;
            if (damageAfterArmor(middle, armor, toughness) < target) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) / 2D;
    }
}
