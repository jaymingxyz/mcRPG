package com.gmail.nossr50.mcrpg.salvage;

import java.util.List;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Records the item data Salvage XP needs: wear from players using or wearing gear, and
 * enchantments on gear found in loot chests, caught while fishing, or dropped by mobs.
 * Runs at MONITOR, after other plugins (and mcMMO's fishing treasure) have made their changes.
 */
public class SalvageTrackingListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(@NotNull PlayerItemDamageEvent event) {
        final ItemStack item = event.getItem();
        if (SalvageTracking.isSalvageable(item)) {
            SalvageTracking.addWear(item, event.getDamage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLootGenerate(@NotNull LootGenerateEvent event) {
        final List<ItemStack> loot = event.getLoot();
        if (markFoundItems(loot)) {
            event.setLoot(loot);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        // A player's own gear dropping on death isn't "found"
        if (!(event.getEntity() instanceof Player)) {
            markFoundItems(event.getDrops());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(@NotNull PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                || !(event.getCaught() instanceof Item caught)) {
            return;
        }
        final ItemStack stack = caught.getItemStack();
        if (isFoundCandidate(stack)) {
            SalvageTracking.markFound(stack);
            caught.setItemStack(stack);
        }
    }

    /** Marks enchanted salvageable items in the list as found. Returns true if any were. */
    private static boolean markFoundItems(@NotNull List<ItemStack> items) {
        boolean changed = false;
        for (ItemStack item : items) {
            if (isFoundCandidate(item)) {
                SalvageTracking.markFound(item);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isFoundCandidate(ItemStack item) {
        return SalvageTracking.isSalvageable(item) && !item.getEnchantments().isEmpty();
    }
}
