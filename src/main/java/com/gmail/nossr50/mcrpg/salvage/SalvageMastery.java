package com.gmail.nossr50.mcrpg.salvage;

import com.gmail.nossr50.config.AdvancedConfig;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.random.ProbabilityUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Salvage Mastery: a higher Salvage level gives back part of the materials an item's damage
 * costs.
 * <p>
 * mcMMO returns materials in proportion to the durability left, rounded down. Mastery returns
 * a share of the materials that withholds, rising evenly from nothing at level 0 to
 * {@code MaxBonusPercentage} at {@code MaxBonusLevel}. Whole materials are guaranteed and a
 * leftover fraction is a chance at one more. The share stays below 100%, so a damaged item
 * never guarantees the materials for a new one.
 */
public final class SalvageMastery {
    private SalvageMastery() {
    }

    /** Share of the materials lost to damage that a player at this level gets back (0 to 1). */
    public static double recoveredShare(int skillLevel) {
        final AdvancedConfig config = mcMMO.p.getAdvancedConfig();
        return bonusMaterials(1, skillLevel, config.getSalvageMasteryMaxBonus(),
                config.getSalvageMasteryMaxLevel());
    }

    /**
     * Materials Mastery adds to mcMMO's yield. The whole part is guaranteed; the fraction is
     * rolled by {@link #rollFraction}.
     *
     * @param skillLevel the player's Salvage level
     * @param baseYield what mcMMO returns for the item's remaining durability
     * @param maxQuantity what the item returns undamaged
     */
    public static double bonusMaterials(int skillLevel, int baseYield, int maxQuantity) {
        final AdvancedConfig config = mcMMO.p.getAdvancedConfig();
        return bonusMaterials(maxQuantity - baseYield, skillLevel,
                config.getSalvageMasteryMaxBonus(), config.getSalvageMasteryMaxLevel());
    }

    /**
     * Rolls the leftover fraction of a material as a chance at one more. Luck perks apply, as
     * they do to Salvage's other rolls.
     *
     * @return 1 if the roll succeeds, otherwise 0
     */
    public static int rollFraction(@NotNull McMMOPlayer mmoPlayer, double bonusMaterials) {
        final double fraction = bonusMaterials - Math.floor(bonusMaterials);
        return fraction > 0 && ProbabilityUtil.isStaticSkillRNGSuccessful(
                PrimarySkillType.SALVAGE, mmoPlayer, fraction * 100) ? 1 : 0;
    }

    static double bonusMaterials(int lostMaterials, int skillLevel, double maxBonusPercentage,
            int maxBonusLevel) {
        if (lostMaterials <= 0 || skillLevel <= 0 || maxBonusPercentage <= 0
                || maxBonusLevel <= 0) {
            return 0D;
        }
        // Multiplies before dividing so thresholds such as "1 of 5 lost materials at level 40"
        // come out as exactly 1 instead of 0.999...
        return lostMaterials * Math.min(skillLevel, maxBonusLevel) * maxBonusPercentage
                / (maxBonusLevel * 100D);
    }
}
