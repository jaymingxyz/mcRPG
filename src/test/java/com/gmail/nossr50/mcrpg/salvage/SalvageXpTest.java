package com.gmail.nossr50.mcrpg.salvage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Salvage XP: wear pays what Repair pays for restoring the same durability (Base 1000 x item
 * XpMultiplier x material multiplier), plus 1000 per found enchantment level.
 */
class SalvageXpTest {
    private static final double WEAR_BASE = 1000D;
    private static final double PER_LEVEL = 1000D;

    private static double xp(int trackedWear, int damage, int maxDurability,
            double itemMultiplier, double materialMultiplier, int foundLevels, int levels) {
        return SalvageXp.calculate(trackedWear, damage, maxDurability, itemMultiplier,
                materialMultiplier, foundLevels, levels, WEAR_BASE, PER_LEVEL);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            // item, wear, max durability, item multiplier, material multiplier, expected XP
            // Most wear that still returns one material (the item's max quantity - 1 of it)
            "iron pickaxe (3 materials), 166, 250, 1, 2.5, 1660",
            "diamond pickaxe (3 materials), 1040, 1561, 1, 5.0, 3331",
            "iron sword (2 materials), 125, 250, 0.5, 2.5, 625",
            "iron chestplate (8 materials), 210, 240, 2, 2.5, 4375",
            "diamond chestplate (8 materials), 462, 528, 6, 5.0, 26250",
            "golden sword (2 materials), 16, 32, 4, 0.3, 600"
    })
    void wearShouldPayLikeRepair(String item, int wear, int maxDurability,
            double itemMultiplier, double materialMultiplier, double expected) {
        assertThat(xp(wear, wear, maxDurability, itemMultiplier, materialMultiplier, 0, 0))
                .isCloseTo(expected, within(1D));
    }

    @Test
    void untrackedDamageShouldGiveNoWearXp() {
        // e.g. a sword dropped by a mob, already damaged but never used by a player
        assertThat(xp(0, 20, 32, 4, 0.3, 0, 0)).isZero();
    }

    @Test
    void repairedDurabilityShouldNoLongerCount() {
        // Worn 200 points, then repaired back to 50 points missing: only 50 count
        assertThat(xp(200, 50, 250, 1, 2.5, 0, 0)).isCloseTo(500D, within(0.001));
    }

    @Test
    void foundEnchantmentsShouldPayPerLevel() {
        // Found with Protection III + Unbreaking II, never worn
        assertThat(xp(0, 0, 528, 6, 5.0, 5, 5)).isEqualTo(5000D);
    }

    @Test
    void onlyEnchantmentsTheItemWasFoundWithShouldCount() {
        // Found with 5 levels, 4 more added at an anvil: still 5
        assertThat(xp(0, 0, 528, 6, 5.0, 5, 9)).isEqualTo(5000D);
        // Found with 5 levels, 3 removed at a grindstone: now 2
        assertThat(xp(0, 0, 528, 6, 5.0, 5, 2)).isEqualTo(2000D);
        // Enchanted at a table or bought from a villager: never found
        assertThat(xp(0, 0, 528, 6, 5.0, 0, 5)).isZero();
    }

    @Test
    void itemsWithoutDurabilityShouldNotBreakTheFormula() {
        assertThat(xp(10, 10, 0, 1, 2.5, 0, 0)).isZero();
    }

    // --- item data ---

    /**
     * An enchantment map with these levels. Salvage only reads the levels, and Enchantment
     * itself can't be created without a running server.
     */
    static Map<Enchantment, Integer> enchantmentLevels(int... levels) {
        final List<Integer> values = new ArrayList<>();
        for (int level : levels) {
            values.add(level);
        }
        return levelsMap(values);
    }

    /** A map whose levels change when {@code values} changes. */
    @SuppressWarnings("unchecked")
    static Map<Enchantment, Integer> levelsMap(List<Integer> values) {
        final Map<Enchantment, Integer> map = mock(Map.class);
        when(map.values()).thenAnswer(invocation -> values);
        when(map.isEmpty()).thenAnswer(invocation -> values.isEmpty());
        return map;
    }

    /** An item whose persistent data is backed by a map, like a real item's. */
    private static ItemStack trackedItem(Map<Enchantment, Integer> enchantments) {
        final Map<NamespacedKey, Object> store = new HashMap<>();
        final PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(data.getOrDefault(any(), any(), any())).thenAnswer(invocation ->
                store.getOrDefault(invocation.getArgument(0), invocation.getArgument(2)));
        when(data.has(any(), any())).thenAnswer(invocation ->
                store.containsKey(invocation.getArgument(0)));
        doAnswer(invocation -> store.put(invocation.getArgument(0), invocation.getArgument(2)))
                .when(data).set(any(), any(), any());

        final ItemMeta meta = mock(ItemMeta.class);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        final ItemStack item = mock(ItemStack.class);
        when(item.getItemMeta()).thenReturn(meta);
        when(item.getEnchantments()).thenReturn(enchantments);
        return item;
    }

    @Test
    void wearShouldAddUp() {
        final ItemStack item = trackedItem(enchantmentLevels());

        SalvageTracking.addWear(item, 3);
        SalvageTracking.addWear(item, 4);
        SalvageTracking.addWear(item, 0);

        assertThat(SalvageTracking.getWear(item)).isEqualTo(7);
    }

    @Test
    void foundLevelsShouldBeRecordedOnceWhenFound() {
        final List<Integer> levels = new ArrayList<>(List.of(3, 2));
        final ItemStack item = trackedItem(levelsMap(levels));

        SalvageTracking.markFound(item);
        levels.add(4); // added later at an anvil
        SalvageTracking.markFound(item);

        assertThat(SalvageTracking.getFoundEnchantmentLevels(item)).isEqualTo(5);
    }

    @Test
    void unenchantedItemsShouldNotBeMarkedFound() {
        final ItemStack item = trackedItem(enchantmentLevels());

        SalvageTracking.markFound(item);

        assertThat(SalvageTracking.getFoundEnchantmentLevels(item)).isZero();
    }
}
