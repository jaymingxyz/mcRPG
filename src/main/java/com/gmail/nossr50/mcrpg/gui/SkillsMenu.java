package com.gmail.nossr50.mcrpg.gui;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.categoryName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.McRPGLinks;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.Specialization;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import com.gmail.nossr50.mcrpg.specialization.SpecializationRole;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.player.UserManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The skills menu opened by /skills: every skill, laid out by category like the
 * Specialization menu, with the player's level, XP and XP rate. Clicking a skill opens its
 * {@link SkillInfoMenu}, which explains how the skill works.
 *
 * <pre>
 *            column 0     1-4          5-8
 * Row 1:     Melee        skills       |
 * Row 2:     Ranged       skills       |   Your skills (Power Level, Specializations)
 * Row 3:     Metallurgy   skills       |   Specializations button
 * Row 4:     Botany       skills       |
 * Row 5:     Blacksmith.  skills       |   How skills work
 * Row 6:     Survivalism  skills       |   Close
 * </pre>
 * The Specializations button opens the Specialization menu, and only shows for players who
 * can use /choosespecialization. Clicking the How skills work book closes the menu and sends
 * a link to the mcRPG wiki.
 */
public final class SkillsMenu implements McRPGMenu {
    static final int SIZE = SkillSelectionMenu.SIZE;
    static final int SUMMARY = 16;
    static final int SPECIALIZATIONS = 25;
    static final int HELP = 43;
    static final int CLOSE = 52;
    /** Columns 5-8 hold the summary and buttons; the rest of them are filler. */
    private static final int FIRST_PANEL_COLUMN = 5;
    private static final int HELP_LORE_WIDTH = 40;
    static final String CHOOSE_SPECIALIZATION_PERMISSION =
            "mcrpg.commands.choosespecialization";

    private final Inventory inventory;
    private final Map<Integer, PrimarySkillType> skillSlots = SkillSelectionMenu.layout();

    private SkillsMenu(@NotNull Player player, @NotNull McMMOPlayer mmoPlayer) {
        inventory = Bukkit.createInventory(this, SIZE,
                LocaleLoader.getString("mcRPG.Skills.Title"));
        render(player, mmoPlayer);
    }

    /** Opens the menu for a player on their own thread (required on Folia). */
    public static void open(@NotNull Player player, @NotNull McMMOPlayer mmoPlayer) {
        mcMMO.p.getFoliaLib().getScheduler().runAtEntity(player, task -> {
            if (player.isOnline()) {
                player.openInventory(new SkillsMenu(player, mmoPlayer).getInventory());
            }
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void render(@NotNull Player player, @NotNull McMMOPlayer mmoPlayer) {
        final PlayerProfile profile = mmoPlayer.getProfile();
        inventory.clear();
        for (int row = 0; row < SIZE / 9; row++) {
            for (int column = FIRST_PANEL_COLUMN; column < 9; column++) {
                inventory.setItem(row * 9 + column, MenuItems.filler());
            }
        }

        for (Map.Entry<Integer, SkillCategory> label
                : SkillSelectionMenu.categoryLabels().entrySet()) {
            final SkillCategory category = label.getValue();
            inventory.setItem(label.getKey(), MenuItems.item(
                    GuiConfig.getInstance().getCategoryIcon(category),
                    ChatColor.GOLD + categoryName(category),
                    SkillSelectionMenu.categoryLore(profile, category),
                    Specialization.slotOf(profile, category) != null));
        }

        for (Map.Entry<Integer, PrimarySkillType> entry : skillSlots.entrySet()) {
            inventory.setItem(entry.getKey(), skillIcon(player, profile, entry.getValue()));
        }

        inventory.setItem(SUMMARY, MenuItems.item(Material.EXPERIENCE_BOTTLE,
                LocaleLoader.getString("mcRPG.Skills.Summary.Name"), List.of(
                        LocaleLoader.getString("mcRPG.Skills.Summary.PowerLevel",
                                mmoPlayer.getPowerLevel()),
                        LocaleLoader.getString("mcRPG.Skills.Summary.Specialization",
                                slotName(SpecializationSlot.PRIMARY),
                                SpecializationDisplay.slotContents(profile,
                                        SpecializationSlot.PRIMARY)),
                        LocaleLoader.getString("mcRPG.Skills.Summary.Specialization",
                                slotName(SpecializationSlot.SECONDARY),
                                SpecializationDisplay.slotContents(profile,
                                        SpecializationSlot.SECONDARY))), false));
        if (player.hasPermission(CHOOSE_SPECIALIZATION_PERMISSION)) {
            inventory.setItem(SPECIALIZATIONS, MenuItems.item(Material.NETHER_STAR,
                    LocaleLoader.getString("mcRPG.Skills.Specializations.Name"),
                    List.of(LocaleLoader.getString("mcRPG.Skills.Specializations.Click")),
                    false));
        }
        inventory.setItem(HELP, MenuItems.item(Material.BOOK,
                LocaleLoader.getString("mcRPG.Skills.Help.Name"), helpLore(), false));
        inventory.setItem(CLOSE, MenuItems.item(Material.BARRIER,
                LocaleLoader.getString("mcRPG.Menu.Close")));
    }

    private static @NotNull ItemStack skillIcon(@NotNull Player player,
            @NotNull PlayerProfile profile, @NotNull PrimarySkillType skill) {
        final List<String> lore = new ArrayList<>(SkillInfoMenu.skillLore(player, profile, skill));
        lore.add("");
        lore.add(LocaleLoader.getString("mcRPG.Skills.Skill.Click", skillName(skill)));
        return MenuItems.item(GuiConfig.getInstance().getIcon(skill),
                ChatColor.YELLOW + skillName(skill), lore,
                Specialization.roleOf(profile, skill) != SpecializationRole.UNSELECTED);
    }

    private static @NotNull List<String> helpLore() {
        final List<String> lore = new ArrayList<>(MenuItems.wrap(LocaleLoader.getString(
                "mcRPG.Skills.Help.Text",
                SpecializationDisplay.multiplierText(SpecializationRole.PRIMARY),
                SpecializationDisplay.multiplierText(SpecializationRole.SECONDARY),
                SpecializationDisplay.multiplierText(SpecializationRole.UNSELECTED)),
                HELP_LORE_WIDTH));
        lore.add("");
        lore.add(LocaleLoader.getString("mcRPG.Skills.Help.Wiki"));
        return lore;
    }

    @Override
    public void handleClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final int rawSlot = event.getRawSlot();
        if (rawSlot == CLOSE) {
            player.closeInventory();
            return;
        }
        if (rawSlot == HELP) {
            // Chat can't be clicked while a menu is open, so close it before sending the link
            player.closeInventory();
            McRPGLinks.send(player, "mcRPG.Wiki.Link", McRPGLinks.WIKI);
            return;
        }
        final PrimarySkillType skill = skillSlots.get(rawSlot);
        final boolean specializations = rawSlot == SPECIALIZATIONS
                && player.hasPermission(CHOOSE_SPECIALIZATION_PERMISSION);
        if (skill == null && !specializations) {
            return;
        }

        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) {
            player.closeInventory();
            player.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
            return;
        }
        if (skill != null) {
            SkillInfoMenu.open(player, mmoPlayer.getProfile(), skill);
        } else {
            SkillSelectionMenu.open(player, mmoPlayer.getProfile());
        }
    }
}
