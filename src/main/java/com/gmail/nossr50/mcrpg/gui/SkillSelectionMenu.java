package com.gmail.nossr50.mcrpg.gui;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.categoryName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.passive.CategoryPassiveDisplay;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.Specialization;
import com.gmail.nossr50.mcrpg.specialization.SpecializationActions;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import com.gmail.nossr50.mcrpg.specialization.SpecializationRole;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.player.UserManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The Specialization menu opened by /choosespecialization and the login reminder. Each category
 * gets one row: its label, then its skills. The right-hand side shows the player's
 * Specializations and the buttons.
 *
 * <pre>
 *            column 0     1-4          5-8
 * Row 1:     Melee        skills       |
 * Row 2:     Ranged       skills       |   Primary Specialization
 * Row 3:     Metallurgy   skills       |   Secondary Specialization
 * Row 4:     Botany       skills       |
 * Row 5:     Blacksmith.  skills       |   How Specializations work
 * Row 6:     Survivalism  skills       |   Close
 * </pre>
 * A Specialization is a whole category. Left-click a category's label or any of its skills to
 * make that category the Primary Specialization, right-click for Secondary. The menu never
 * abandons one; that is only done with /abandonspecialization.
 */
public final class SkillSelectionMenu implements InventoryHolder {
    static final int SIZE = 54;
    static final int PRIMARY_INDICATOR = 16;
    static final int SECONDARY_INDICATOR = 25;
    static final int INFO = 43;
    static final int CLOSE = 52;
    /** Columns 5-8 hold the Specializations and buttons; the rest of them are filler. */
    private static final int FIRST_PANEL_COLUMN = 5;

    private final Inventory inventory;
    private final Map<Integer, PrimarySkillType> skillSlots = layout();
    private final Map<Integer, SkillCategory> labelSlots = categoryLabels();

    private SkillSelectionMenu(@NotNull Player player, @NotNull PlayerProfile profile) {
        inventory = Bukkit.createInventory(this, SIZE,
                LocaleLoader.getString("mcRPG.Menu.Title"));
        render(player, profile);
    }

    /** Opens the menu for a player on their own thread (required on Folia). */
    public static void open(@NotNull Player player, @NotNull PlayerProfile profile) {
        mcMMO.p.getFoliaLib().getScheduler().runAtEntity(player, task -> {
            if (player.isOnline()) {
                player.openInventory(new SkillSelectionMenu(player, profile).getInventory());
            }
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Which inventory slot shows which skill: each category's skills follow its label on the
     * category's row, leaving out skills that don't exist on this Minecraft version.
     */
    @VisibleForTesting
    static @NotNull Map<Integer, PrimarySkillType> layout() {
        final Map<Integer, PrimarySkillType> slots = new LinkedHashMap<>();
        for (Map.Entry<Integer, SkillCategory> label : categoryLabels().entrySet()) {
            final List<PrimarySkillType> skills = label.getValue().availableSkills();
            for (int i = 0; i < skills.size(); i++) {
                slots.put(label.getKey() + 1 + i, skills.get(i));
            }
        }
        return Collections.unmodifiableMap(slots);
    }

    /** The slot of each category's label: the first column of the category's row. */
    @VisibleForTesting
    static @NotNull Map<Integer, SkillCategory> categoryLabels() {
        final SkillCategory[] categories = SkillCategory.values();
        if (categories.length > SIZE / 9) {
            throw new IllegalStateException("The Specialization menu has one row per category,"
                    + " so it fits at most " + SIZE / 9 + " categories");
        }
        final Map<Integer, SkillCategory> labels = new LinkedHashMap<>();
        for (int row = 0; row < categories.length; row++) {
            if (categories[row].skills().size() > SkillCategory.MAX_SKILLS_PER_CATEGORY) {
                throw new IllegalStateException(categories[row] + " has more skills than fit"
                        + " in its row of the Specialization menu");
            }
            labels.put(row * 9, categories[row]);
        }
        return Collections.unmodifiableMap(labels);
    }

    private void render(@NotNull Player player, @NotNull PlayerProfile profile) {
        inventory.clear();
        for (int row = 0; row < SIZE / 9; row++) {
            for (int column = FIRST_PANEL_COLUMN; column < 9; column++) {
                inventory.setItem(row * 9 + column, MenuItems.filler());
            }
        }

        for (Map.Entry<Integer, SkillCategory> label : labelSlots.entrySet()) {
            inventory.setItem(label.getKey(), categoryLabel(player, profile, label.getValue()));
        }

        inventory.setItem(PRIMARY_INDICATOR, slotIndicator(profile, SpecializationSlot.PRIMARY));
        inventory.setItem(SECONDARY_INDICATOR,
                slotIndicator(profile, SpecializationSlot.SECONDARY));

        for (Map.Entry<Integer, PrimarySkillType> entry : skillSlots.entrySet()) {
            inventory.setItem(entry.getKey(), skillIcon(player, profile, entry.getValue()));
        }

        inventory.setItem(INFO, MenuItems.item(Material.BOOK,
                LocaleLoader.getString("mcRPG.Menu.Info.Name"), infoLore(), false));
        inventory.setItem(CLOSE, MenuItems.item(Material.BARRIER,
                LocaleLoader.getString("mcRPG.Menu.Close")));
    }

    private static @NotNull ItemStack slotIndicator(
            @NotNull PlayerProfile profile, @NotNull SpecializationSlot slot) {
        final SkillCategory category = profile.getSpecialization(slot);
        final String name = LocaleLoader.getString("mcRPG.Menu.SlotIndicator.Name",
                slotName(slot));
        if (category == null) {
            return MenuItems.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, name,
                    List.of(LocaleLoader.getString("mcRPG.Menu.SlotIndicator.Empty")), false);
        }
        return MenuItems.item(GuiConfig.getInstance().getCategoryIcon(category), name,
                List.of(LocaleLoader.getString("mcRPG.Menu.SlotIndicator.Chosen",
                                categoryName(category)),
                        LocaleLoader.getString("mcRPG.Menu.SlotIndicator.Skills",
                                SpecializationDisplay.skillList(category))), true);
    }

    /** A category's label: its skills, the XP rate they earn now, and how to choose it. */
    private static @NotNull ItemStack categoryLabel(@NotNull Player player,
            @NotNull PlayerProfile profile, @NotNull SkillCategory category) {
        final SpecializationSlot slot = Specialization.slotOf(profile, category);
        final SpecializationRole role = slot == null ? SpecializationRole.UNSELECTED
                : slot.role();
        final List<String> lore = new ArrayList<>();
        lore.add(LocaleLoader.getString("mcRPG.Menu.Category.Skills",
                SpecializationDisplay.skillList(category)));
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Rate",
                SpecializationDisplay.multiplierText(role)));
        final List<String> passive = CategoryPassiveDisplay.menuLore(profile, category);
        if (!passive.isEmpty()) {
            lore.add("");
            lore.addAll(passive);
        }
        lore.add("");
        lore.addAll(choiceLore(player, profile, category));
        return MenuItems.item(GuiConfig.getInstance().getCategoryIcon(category),
                ChatColor.GOLD + categoryName(category), lore, slot != null);
    }

    private static @NotNull ItemStack skillIcon(@NotNull Player player,
            @NotNull PlayerProfile profile, @NotNull PrimarySkillType skill) {
        final SpecializationRole role = Specialization.roleOf(profile, skill);
        final SkillCategory category = SkillCategory.of(skill);
        final List<String> lore = new ArrayList<>();
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Level", profile.getSkillLevel(skill)));
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Category", categoryName(category)));
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Rate",
                SpecializationDisplay.multiplierText(role)));
        lore.add("");
        lore.addAll(choiceLore(player, profile, category));

        return MenuItems.item(GuiConfig.getInstance().getIcon(skill),
                ChatColor.YELLOW + skillName(skill), lore, role != SpecializationRole.UNSELECTED);
    }

    /** Whether the category is a Specialization, or how to choose it and what's in the way. */
    private static @NotNull List<String> choiceLore(@NotNull Player player,
            @NotNull PlayerProfile profile, @NotNull SkillCategory category) {
        final String name = categoryName(category);
        final SpecializationSlot slot = Specialization.slotOf(profile, category);
        if (slot == SpecializationSlot.PRIMARY) {
            return List.of(LocaleLoader.getString("mcRPG.Menu.Choice.IsPrimary", name));
        }
        if (slot == SpecializationSlot.SECONDARY) {
            return List.of(LocaleLoader.getString("mcRPG.Menu.Choice.IsSecondary", name));
        }
        if (!SpecializationActions.canUseCategory(player, category)) {
            return List.of(LocaleLoader.getString("mcRPG.Choose.NoPermission", name));
        }
        return List.of(
                profile.getSpecialization(SpecializationSlot.PRIMARY) == null
                        ? LocaleLoader.getString("mcRPG.Menu.Choice.LeftClick", name)
                        : LocaleLoader.getString("mcRPG.Menu.Choice.PrimaryFilled"),
                profile.getSpecialization(SpecializationSlot.SECONDARY) == null
                        ? LocaleLoader.getString("mcRPG.Menu.Choice.RightClick", name)
                        : LocaleLoader.getString("mcRPG.Menu.Choice.SecondaryFilled"));
    }

    /** The category shown at this slot, by its label or one of its skills, or null. */
    @VisibleForTesting
    @Nullable SkillCategory categoryAt(int rawSlot) {
        final SkillCategory labelled = labelSlots.get(rawSlot);
        if (labelled != null) {
            return labelled;
        }
        final PrimarySkillType skill = skillSlots.get(rawSlot);
        return skill == null ? null : SkillCategory.of(skill);
    }

    private static @NotNull List<String> infoLore() {
        final String kept = SpecializationDisplay.formatNumber(
                mcMMO.p.getGeneralConfig().getAbandonXpKeptPercent());
        return List.of(
                LocaleLoader.getString("mcRPG.Menu.Info.Line1",
                        SpecializationDisplay.multiplierText(SpecializationRole.PRIMARY)),
                LocaleLoader.getString("mcRPG.Menu.Info.Line2",
                        SpecializationDisplay.multiplierText(SpecializationRole.SECONDARY)),
                LocaleLoader.getString("mcRPG.Menu.Info.Line3",
                        SpecializationDisplay.multiplierText(SpecializationRole.UNSELECTED)),
                LocaleLoader.getString("mcRPG.Menu.Info.Line4"),
                LocaleLoader.getString("mcRPG.Menu.Info.Line5"),
                LocaleLoader.getString("mcRPG.Menu.Info.Line6", kept));
    }

    /** Handles a click anywhere while this menu is open. Every click is cancelled. */
    void handleClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= SIZE) {
            return; // the player's own inventory
        }
        if (rawSlot == CLOSE) {
            player.closeInventory();
            return;
        }

        final SkillCategory category = categoryAt(rawSlot);
        final SpecializationSlot targetSlot = event.isLeftClick() ? SpecializationSlot.PRIMARY
                : event.isRightClick() ? SpecializationSlot.SECONDARY : null;
        if (category == null || targetSlot == null) {
            return;
        }

        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) {
            player.closeInventory();
            player.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
            return;
        }
        final PlayerProfile profile = mmoPlayer.getProfile();

        final boolean allowed = SpecializationActions.canUseCategory(player, category)
                && Specialization.checkChoose(profile, targetSlot, category)
                == Specialization.ChooseResult.SUCCESS;
        if (!allowed) {
            // Sends the reason without changing anything
            SpecializationActions.tryChoose(player, profile, targetSlot, category);
            return;
        }

        if (mcMMO.p.getGeneralConfig().getSpecializationGuiConfirmation()) {
            ConfirmChoiceMenu.open(player, category, targetSlot);
        } else {
            SpecializationActions.tryChoose(player, profile, targetSlot, category);
            render(player, profile);
        }
    }
}
