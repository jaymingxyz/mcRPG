package com.gmail.nossr50.mcrpg.gui;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
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
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The Specialization menu opened by /chooseskill and the login reminder. Each category gets
 * one row: its label, then its skills. The right-hand side shows the player's Specializations
 * and the buttons.
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
 * Left-click a skill to make it the Primary Specialization, right-click for Secondary. The
 * menu never abandons one; that is only done with /abandonskill.
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

        for (Map.Entry<Integer, SkillCategory> label : categoryLabels().entrySet()) {
            inventory.setItem(label.getKey(), MenuItems.item(
                    GuiConfig.getInstance().getCategoryIcon(label.getValue()),
                    ChatColor.GOLD + LocaleLoader.getString(label.getValue().nameKey())));
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
        final PrimarySkillType skill = profile.getSpecialization(slot);
        final String name = LocaleLoader.getString("mcRPG.Menu.SlotIndicator.Name",
                slotName(slot));
        if (skill == null) {
            return MenuItems.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, name,
                    List.of(LocaleLoader.getString("mcRPG.Menu.SlotIndicator.Empty")), false);
        }
        return MenuItems.item(GuiConfig.getInstance().getIcon(skill), name,
                List.of(LocaleLoader.getString("mcRPG.Menu.SlotIndicator.Chosen",
                        skillName(skill))), true);
    }

    private static @NotNull ItemStack skillIcon(@NotNull Player player,
            @NotNull PlayerProfile profile, @NotNull PrimarySkillType skill) {
        final SpecializationRole role = Specialization.roleOf(profile, skill);
        final List<String> lore = new ArrayList<>();
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Level", profile.getSkillLevel(skill)));
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Category",
                LocaleLoader.getString(SkillCategory.of(skill).nameKey())));
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Rate",
                SpecializationDisplay.multiplierText(role)));
        lore.add("");

        switch (role) {
            case PRIMARY -> lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.IsPrimary"));
            case SECONDARY -> lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.IsSecondary"));
            case UNSELECTED -> {
                if (!SpecializationActions.canUseSkill(player, skill)) {
                    lore.add(LocaleLoader.getString("mcRPG.Choose.NoPermission",
                            skillName(skill)));
                } else {
                    lore.add(LocaleLoader.getString(
                            profile.getSpecialization(SpecializationSlot.PRIMARY) == null
                                    ? "mcRPG.Menu.Skill.LeftClick"
                                    : "mcRPG.Menu.Skill.PrimaryFilled"));
                    lore.add(LocaleLoader.getString(
                            profile.getSpecialization(SpecializationSlot.SECONDARY) == null
                                    ? "mcRPG.Menu.Skill.RightClick"
                                    : "mcRPG.Menu.Skill.SecondaryFilled"));
                }
            }
        }

        return MenuItems.item(GuiConfig.getInstance().getIcon(skill),
                ChatColor.YELLOW + skillName(skill), lore, role != SpecializationRole.UNSELECTED);
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

        final PrimarySkillType skill = skillSlots.get(rawSlot);
        final SpecializationSlot targetSlot = event.isLeftClick() ? SpecializationSlot.PRIMARY
                : event.isRightClick() ? SpecializationSlot.SECONDARY : null;
        if (skill == null || targetSlot == null) {
            return;
        }

        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) {
            player.closeInventory();
            player.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
            return;
        }
        final PlayerProfile profile = mmoPlayer.getProfile();

        final boolean allowed = SpecializationActions.canUseSkill(player, skill)
                && Specialization.checkChoose(profile, targetSlot, skill)
                == Specialization.ChooseResult.SUCCESS;
        if (!allowed) {
            // Sends the reason without changing anything
            SpecializationActions.tryChoose(player, profile, targetSlot, skill);
            return;
        }

        if (mcMMO.p.getGeneralConfig().getSpecializationGuiConfirmation()) {
            ConfirmChoiceMenu.open(player, skill, targetSlot);
        } else {
            SpecializationActions.tryChoose(player, profile, targetSlot, skill);
            render(player, profile);
        }
    }
}
