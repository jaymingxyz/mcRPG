package com.gmail.nossr50.mcrpg.salvage;

import com.gmail.nossr50.mcMMO;
import java.util.Map;
import java.util.Objects;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data mcRPG stores on items for Salvage XP:
 * <ul>
 *     <li>{@code mcrpg:salvage_wear}: durability the item has lost while a player used or wore
 *     it.</li>
 *     <li>{@code mcrpg:found_enchantment_levels}: the item's total enchantment levels when it
 *     was found (loot chest, fishing catch, or mob drop). Items without it have no found
 *     enchantments, which covers enchanting tables, anvils and villager trades.</li>
 * </ul>
 * The keys use a fixed {@code mcrpg} namespace so items keep their data if the plugin is
 * renamed.
 */
public final class SalvageTracking {
    static final NamespacedKey WEAR_KEY =
            Objects.requireNonNull(NamespacedKey.fromString("mcrpg:salvage_wear"));
    static final NamespacedKey FOUND_LEVELS_KEY =
            Objects.requireNonNull(NamespacedKey.fromString("mcrpg:found_enchantment_levels"));

    private SalvageTracking() {
    }

    /** Whether mcMMO can salvage this kind of item. Only these items are tracked. */
    public static boolean isSalvageable(@Nullable ItemStack item) {
        return item != null && mcMMO.getSalvageableManager().isSalvageable(item.getType());
    }

    /** Adds durability lost while a player was using or wearing the item. */
    public static void addWear(@NotNull ItemStack item, int damage) {
        if (damage <= 0) {
            return;
        }
        final ItemMeta meta = item.getItemMeta();
        final PersistentDataContainer data = dataOf(meta);
        if (data == null) {
            return;
        }
        final int current = data.getOrDefault(WEAR_KEY, PersistentDataType.INTEGER, 0);
        data.set(WEAR_KEY, PersistentDataType.INTEGER,
                (int) Math.min(Integer.MAX_VALUE, (long) current + damage));
        item.setItemMeta(meta);
    }

    /** Durability lost while a player used or wore the item (0 if never tracked). */
    public static int getWear(@NotNull ItemStack item) {
        final PersistentDataContainer data = dataOf(item.getItemMeta());
        return data == null ? 0 : data.getOrDefault(WEAR_KEY, PersistentDataType.INTEGER, 0);
    }

    /**
     * Records an enchanted item's enchantment levels as found. Items that already have a
     * record keep it, so enchantments added later at an anvil don't count.
     */
    public static void markFound(@NotNull ItemStack item) {
        final int levels = totalLevels(item.getEnchantments());
        if (levels <= 0) {
            return;
        }
        final ItemMeta meta = item.getItemMeta();
        final PersistentDataContainer data = dataOf(meta);
        if (data == null) {
            return;
        }
        if (data.has(FOUND_LEVELS_KEY, PersistentDataType.INTEGER)) {
            return;
        }
        data.set(FOUND_LEVELS_KEY, PersistentDataType.INTEGER, levels);
        item.setItemMeta(meta);
    }

    /** Enchantment levels the item had when found (0 if it wasn't found). */
    public static int getFoundEnchantmentLevels(@NotNull ItemStack item) {
        final PersistentDataContainer data = dataOf(item.getItemMeta());
        return data == null ? 0
                : data.getOrDefault(FOUND_LEVELS_KEY, PersistentDataType.INTEGER, 0);
    }

    private static @Nullable PersistentDataContainer dataOf(@Nullable ItemMeta meta) {
        return meta == null ? null : meta.getPersistentDataContainer();
    }

    static int totalLevels(@NotNull Map<Enchantment, Integer> enchantments) {
        int total = 0;
        for (int level : enchantments.values()) {
            total += Math.max(0, level);
        }
        return total;
    }
}
