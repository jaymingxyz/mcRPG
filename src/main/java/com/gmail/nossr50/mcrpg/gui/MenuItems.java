package com.gmail.nossr50.mcrpg.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
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

    /**
     * Splits text into lore lines of at most {@code width} visible characters, breaking at
     * spaces and at line breaks in the text. A line carried over keeps the colors that were in
     * effect where it was split.
     */
    static @NotNull List<String> wrap(@NotNull String text, int width) {
        final List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            final StringBuilder line = new StringBuilder();
            int visible = 0;
            for (String word : paragraph.split(" ")) {
                final int wordLength = ChatColor.stripColor(word).length();
                if (visible > 0 && visible + 1 + wordLength > width) {
                    final String full = line.toString();
                    lines.add(full);
                    line.setLength(0);
                    line.append(ChatColor.getLastColors(full));
                    visible = 0;
                }
                if (visible > 0) {
                    line.append(' ');
                    visible++;
                }
                line.append(word);
                visible += wordLength;
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
