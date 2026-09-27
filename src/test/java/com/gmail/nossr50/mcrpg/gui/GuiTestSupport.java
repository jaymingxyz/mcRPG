package com.gmail.nossr50.mcrpg.gui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.mcMMO;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Lets tests build mcRPG's menus on top of {@code MMOTestEnvironment}: inventories, item meta,
 * and the shipped gui.yml.
 */
public final class GuiTestSupport {
    private GuiTestSupport() {
    }

    /** Call after {@code mockBaseEnvironment}. Returns the inventory every menu will use. */
    public static Inventory prepareMenus() {
        GuiConfig.resetForTests();
        when(mcMMO.p.getResource("gui.yml")).thenAnswer(invocation ->
                GuiTestSupport.class.getClassLoader().getResourceAsStream("gui.yml"));

        final Inventory inventory = mock(Inventory.class);
        when(Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                .thenReturn(inventory);

        final ItemFactory items = mock(ItemFactory.class);
        when(items.getItemMeta(any(Material.class))).thenAnswer(
                invocation -> mock(ItemMeta.class));
        when(Bukkit.getItemFactory()).thenReturn(items);
        return inventory;
    }

    public static void reset() {
        GuiConfig.resetForTests();
    }
}
