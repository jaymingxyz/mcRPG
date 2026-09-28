package com.gmail.nossr50.mcrpg.passive;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.util.MaterialMapStore;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The passive a Specialization category unlocks. Each one improves a kind of gear: three
 * armor materials and wooden tools. A passive only works while its category is one of the
 * player's Specializations and the category's skill levels add up to the unlock level (see
 * {@link CategoryPassives}). Melee and Ranged have none.
 * <p>
 * The defaults here are the fallbacks for a config.yml without the setting, and match the
 * shipped config.yml. Armor masteries default to roughly the protection of iron for a full
 * set: the weaker the armor, the larger its bonus.
 */
public enum CategoryPassive {
    LEATHER_MASTERY(SkillCategory.SURVIVALISM, "Leather_Mastery", 8, 0, 50),
    CHAINMAIL_MASTERY(SkillCategory.BLACKSMITHING, "Chainmail_Mastery", 4, 0, 50),
    COPPER_MASTERY(SkillCategory.METALLURGY, "Copper_Mastery", 6, 0, 50),
    WOODEN_MASTERY(SkillCategory.BOTANY, "Wooden_Mastery", 0, 50, 50);

    private final @NotNull SkillCategory category;
    private final @NotNull String configKey;
    private final double defaultDamageReduction;
    private final double defaultMiningSpeedBonus;
    private final double defaultDurabilityLossReduction;

    CategoryPassive(@NotNull SkillCategory category, @NotNull String configKey,
            double defaultDamageReduction, double defaultMiningSpeedBonus,
            double defaultDurabilityLossReduction) {
        this.category = category;
        this.configKey = configKey;
        this.defaultDamageReduction = defaultDamageReduction;
        this.defaultMiningSpeedBonus = defaultMiningSpeedBonus;
        this.defaultDurabilityLossReduction = defaultDurabilityLossReduction;
    }

    /** The category that unlocks this passive. */
    public @NotNull SkillCategory category() {
        return category;
    }

    /** The key under {@code Specialization.Passives} in config.yml, e.g. Leather_Mastery. */
    public @NotNull String configKey() {
        return configKey;
    }

    /** The locale key for this passive's name, e.g. "Leather Mastery". */
    public @NotNull String nameKey() {
        return "mcRPG.Passive." + configKey + ".Name";
    }

    /** The locale key for what this passive does. */
    public @NotNull String effectKey() {
        return "mcRPG.Passive." + configKey + ".Effect";
    }

    /** True for the armor masteries, false for Wooden Mastery. */
    public boolean isArmorMastery() {
        return this != WOODEN_MASTERY;
    }

    /** Percent less damage for each piece worn. Only armor masteries have a default above 0. */
    public double defaultDamageReduction() {
        return defaultDamageReduction;
    }

    /** Percent faster mining. Only Wooden Mastery has a default above 0. */
    public double defaultMiningSpeedBonus() {
        return defaultMiningSpeedBonus;
    }

    /** Percent chance that each point of durability damage is ignored. */
    public double defaultDurabilityLossReduction() {
        return defaultDurabilityLossReduction;
    }

    /** Whether this passive improves the item: armor of its material, or a wooden tool. */
    public boolean appliesTo(@Nullable ItemStack item) {
        return item != null && appliesTo(item.getType().getKey().getKey());
    }

    /**
     * Whether this passive improves items with this Minecraft ID, e.g. {@code leather_boots}.
     * IDs are used because copper armor doesn't exist on older Minecraft versions.
     */
    public boolean appliesTo(@NotNull String materialId) {
        final MaterialMapStore materials = mcMMO.getMaterialMapStore();
        return switch (this) {
            case LEATHER_MASTERY -> materials.isLeatherArmor(materialId);
            case CHAINMAIL_MASTERY -> materials.isChainmailArmor(materialId);
            case COPPER_MASTERY -> materials.isCopperArmor(materialId);
            case WOODEN_MASTERY -> materials.isWoodTool(materialId);
        };
    }

    /** The passive this category unlocks, or null if it has none. */
    public static @Nullable CategoryPassive of(@NotNull SkillCategory category) {
        for (CategoryPassive passive : values()) {
            if (passive.category == category) {
                return passive;
            }
        }
        return null;
    }

    /** The passive that improves this item, or null if none does. */
    public static @Nullable CategoryPassive forItem(@Nullable ItemStack item) {
        if (item == null) {
            return null;
        }
        final String materialId = item.getType().getKey().getKey();
        for (CategoryPassive passive : values()) {
            if (passive.appliesTo(materialId)) {
                return passive;
            }
        }
        return null;
    }
}
