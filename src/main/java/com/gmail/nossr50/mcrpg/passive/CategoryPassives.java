package com.gmail.nossr50.mcrpg.passive;

import com.gmail.nossr50.config.GeneralConfig;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.Specialization;
import java.util.function.DoubleSupplier;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The rules for category passives: when one is unlocked, and how much it improves gear.
 * A passive works only while its category is one of the player's Specializations (Primary or
 * Secondary) and the levels of the category's skills add up to the unlock level (25 by
 * default). Everything here works on a {@link PlayerProfile}; the effects themselves are
 * applied by {@link CategoryPassiveListener}.
 */
public final class CategoryPassives {
    private CategoryPassives() {
    }

    /** Whether a passive works for a player and, if not, what's missing. */
    public enum Status {
        ACTIVE,
        /** Category passives are turned off in config.yml. */
        DISABLED,
        /** The category isn't one of the player's Specializations. */
        NOT_SPECIALIZED,
        /** The category is a Specialization, but its skills' levels don't add up to enough. */
        NEEDS_LEVELS
    }

    /** The levels of the category's skills that exist on this server, added together. */
    public static int categoryLevel(@NotNull PlayerProfile profile,
            @NotNull SkillCategory category) {
        int total = 0;
        for (PrimarySkillType skill : category.availableSkills()) {
            total += profile.getSkillLevel(skill);
        }
        return total;
    }

    /** The combined category level that unlocks a passive. */
    public static int unlockLevel() {
        return mcMMO.p.getGeneralConfig().getCategoryPassiveUnlockLevel();
    }

    public static @NotNull Status status(@NotNull PlayerProfile profile,
            @NotNull CategoryPassive passive) {
        if (!mcMMO.p.getGeneralConfig().getCategoryPassivesEnabled()) {
            return Status.DISABLED;
        }
        if (Specialization.slotOf(profile, passive.category()) == null) {
            return Status.NOT_SPECIALIZED;
        }
        return categoryLevel(profile, passive.category()) >= unlockLevel()
                ? Status.ACTIVE : Status.NEEDS_LEVELS;
    }

    public static boolean isActive(@NotNull PlayerProfile profile,
            @NotNull CategoryPassive passive) {
        return status(profile, passive) == Status.ACTIVE;
    }

    /**
     * The share of damage (0 to 1) the player's active armor masteries remove, from every
     * piece of mastered armor they wear. Mixed sets count each piece toward its own mastery.
     *
     * @param armor the worn armor, as from {@code PlayerInventory#getArmorContents()}
     */
    public static double armorDamageReduction(@NotNull PlayerProfile profile,
            @Nullable ItemStack @NotNull [] armor) {
        final GeneralConfig config = mcMMO.p.getGeneralConfig();
        double percent = 0;
        for (ItemStack piece : armor) {
            final CategoryPassive passive = CategoryPassive.forItem(piece);
            if (passive != null && passive.isArmorMastery() && isActive(profile, passive)) {
                percent += config.getCategoryPassiveDamageReduction(passive);
            }
        }
        return Math.min(1D, percent / 100D);
    }

    /**
     * How much faster the player mines while holding this item (0.5 means 50% faster), or 0
     * if it isn't a tool of an active passive.
     */
    public static double miningSpeedBonus(@NotNull PlayerProfile profile,
            @Nullable ItemStack held) {
        final CategoryPassive passive = CategoryPassive.forItem(held);
        if (passive == null || passive.isArmorMastery() || !isActive(profile, passive)) {
            return 0D;
        }
        return mcMMO.p.getGeneralConfig().getCategoryPassiveMiningSpeedBonus(passive) / 100D;
    }

    /**
     * The durability damage this item takes after the player's passives: each point is
     * ignored with the passive's chance, like Unbreaking.
     *
     * @param random returns a number from 0 (inclusive) to 1 (exclusive) for each point
     */
    public static int durabilityDamage(@NotNull PlayerProfile profile, @NotNull ItemStack item,
            int damage, @NotNull DoubleSupplier random) {
        final CategoryPassive passive = CategoryPassive.forItem(item);
        if (passive == null || damage <= 0 || !isActive(profile, passive)) {
            return damage;
        }
        return keptDamage(damage,
                mcMMO.p.getGeneralConfig().getCategoryPassiveDurabilityLossReduction(passive),
                random);
    }

    static int keptDamage(int damage, double ignoredPercent, @NotNull DoubleSupplier random) {
        if (ignoredPercent <= 0) {
            return damage;
        }
        final double ignoredChance = ignoredPercent / 100D;
        int kept = 0;
        for (int point = 0; point < damage; point++) {
            if (random.getAsDouble() >= ignoredChance) {
                kept++;
            }
        }
        return kept;
    }
}
