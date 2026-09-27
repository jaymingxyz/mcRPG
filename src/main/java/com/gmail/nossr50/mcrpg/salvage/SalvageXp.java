package com.gmail.nossr50.mcrpg.salvage;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.skills.MaterialType;
import com.gmail.nossr50.skills.salvage.salvageables.Salvageable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * mcRPG's Salvage XP (mcMMO gives none):
 * <pre>
 * Salvage XP = wear XP + found enchantment XP
 * wear XP    = (player wear / max durability) x item XpMultiplier x Wear_Base
 *              x material multiplier (the same one Repair uses)
 * found XP   = Found_Enchantment_Per_Level x levels the item had when found
 * </pre>
 * Player wear only counts durability the item is still missing, so repairing it or using
 * Mending lowers what it can give. With the defaults, wear pays the same XP that Repair pays
 * for restoring the same durability.
 */
public final class SalvageXp {
    private SalvageXp() {
    }

    /** Salvage XP for this item, before the player's specialization multiplier. */
    public static float forItem(@NotNull ItemStack item, @NotNull Salvageable salvageable) {
        final ItemMeta meta = item.getItemMeta();
        final int currentDamage = meta instanceof Damageable damageable
                ? damageable.getDamage() : 0;
        final ExperienceConfig config = ExperienceConfig.getInstance();

        return (float) calculate(SalvageTracking.getWear(item), currentDamage,
                salvageable.getMaximumDurability(), salvageable.getXpMultiplier(),
                materialMultiplier(salvageable.getSalvageMaterialType()),
                SalvageTracking.getFoundEnchantmentLevels(item),
                SalvageTracking.totalLevels(item.getEnchantments()),
                config.getSalvageWearXpBase(), config.getSalvageFoundEnchantmentXpPerLevel());
    }

    /**
     * The Salvage XP formula.
     *
     * @param trackedWear durability lost while a player used or wore the item
     * @param currentDamage durability the item is missing right now
     * @param maxDurability the item's max durability
     * @param itemXpMultiplier the item's XpMultiplier from salvage.vanilla.yml
     * @param materialMultiplier the material multiplier Repair uses (e.g. Iron 2.5)
     * @param foundLevels enchantment levels the item had when found
     * @param currentLevels enchantment levels it has now
     * @param wearBase Experience_Values.Salvage.Wear_Base
     * @param perFoundLevel Experience_Values.Salvage.Found_Enchantment_Per_Level
     */
    static double calculate(int trackedWear, int currentDamage, int maxDurability,
            double itemXpMultiplier, double materialMultiplier, int foundLevels,
            int currentLevels, double wearBase, double perFoundLevel) {
        double wearXp = 0D;
        if (maxDurability > 0) {
            final int wear = Math.max(0, Math.min(trackedWear, currentDamage));
            wearXp = (double) wear / maxDurability * itemXpMultiplier * wearBase
                    * materialMultiplier;
        }
        // Enchantments removed since (e.g. at a grindstone) no longer count
        final int levels = Math.max(0, Math.min(foundLevels, currentLevels));
        return wearXp + levels * perFoundLevel;
    }

    private static double materialMultiplier(@NotNull MaterialType materialType) {
        final double multiplier = ExperienceConfig.getInstance().getRepairXP(materialType);
        return multiplier > 0D ? multiplier
                : ExperienceConfig.getInstance().getRepairXP(MaterialType.OTHER);
    }
}
