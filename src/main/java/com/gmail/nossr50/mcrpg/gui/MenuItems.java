package com.gmail.nossr50.mcrpg.gui;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/** Builds the display items used in mcRPG's menus. */
final class MenuItems {
    private MenuItems() {
    }

    static @NotNull ItemStack item(@NotNull Material material, @NotNull String name,
            @NotNull List<String> lore, boolean glowing) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            if (glowing) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    static @NotNull ItemStack item(@NotNull Material material, @NotNull String name) {
        return item(material, name, List.of(), false);
    }

    static @NotNull ItemStack filler() {
        return item(Material.GRAY_STAINED_GLASS_PANE, " ");
    }
}
