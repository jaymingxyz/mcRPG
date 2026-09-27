package com.gmail.nossr50.mcrpg.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Routes clicks in mcRPG's menus and stops items being moved in or out of them. Uses only the
 * event's own methods, never InventoryView ones, because InventoryView changed from a class to
 * an interface in Minecraft 1.21 and mcRPG is compiled against 1.20.5.
 */
public class MenuListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof SkillSelectionMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof ConfirmChoiceMenu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        final InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof SkillSelectionMenu || holder instanceof ConfirmChoiceMenu) {
            event.setCancelled(true);
        }
    }
}
