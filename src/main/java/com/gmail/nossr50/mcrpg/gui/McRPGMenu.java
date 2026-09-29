package com.gmail.nossr50.mcrpg.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** One of mcRPG's chest menus. {@link MenuListener} sends it every click made while it's open. */
interface McRPGMenu extends InventoryHolder {
    /** Handles a click anywhere while this menu is open. Every click is cancelled. */
    void handleClick(@NotNull InventoryClickEvent event);
}
