package com.gmail.nossr50.mcrpg.gui;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.categoryName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.Specialization;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import com.gmail.nossr50.mcrpg.specialization.SpecializationRole;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.RankUtils;
import com.gmail.nossr50.util.text.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * One skill's page in the skills menu: how the player is doing in it, what each of its
 * abilities does and when it unlocks, and the skill's guide (the same text as /&lt;skill&gt; ?).
 *
 * <pre>
 *            column 0     1-7                    8
 * Row 1:                  skill (centred)
 * Row 2:     Abilities    abilities
 * Row 3:                  abilities
 * Row 4:     Guide        guide pages
 * Row 5:                  guide pages
 * Row 6:                  Back, Close
 * </pre>
 * Unlocked abilities are lime dye and locked ones gray dye. Each guide page is a book named
 * after the page's heading.
 */
public final class SkillInfoMenu implements McRPGMenu {
    static final int SIZE = 54;
    static final int HEADER = 4;
    static final int ABILITIES_LABEL = 9;
    static final int GUIDE_LABEL = 27;
    static final int BACK = 48;
    static final int CLOSE = 50;
    /** Rows 2 and 3, columns 1-7. */
    static final List<Integer> ABILITY_SLOTS = rowSlots(1, 2);
    /** Rows 4 and 5, columns 1-7. */
    static final List<Integer> GUIDE_SLOTS = rowSlots(3, 4);
    /** mcMMO's guide command reads at most this many pages per skill. */
    private static final int MAX_GUIDE_PAGES = 10;
    private static final int ABILITY_LORE_WIDTH = 40;
    /** Guide lines are already broken at about 60 characters, so only longer ones wrap. */
    private static final int GUIDE_LORE_WIDTH = 60;

    private final Inventory inventory;
    private final PrimarySkillType skill;

    private SkillInfoMenu(@NotNull Player player, @NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        this.skill = skill;
        inventory = Bukkit.createInventory(this, SIZE,
                LocaleLoader.getString("mcRPG.Skills.Info.Title", skillName(skill)));
        render(player, profile);
    }

    /** Opens a skill's page for a player on their own thread (required on Folia). */
    public static void open(@NotNull Player player, @NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        mcMMO.p.getFoliaLib().getScheduler().runAtEntity(player, task -> {
            if (player.isOnline()) {
                player.openInventory(new SkillInfoMenu(player, profile, skill).getInventory());
            }
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @VisibleForTesting
    @NotNull PrimarySkillType skill() {
        return skill;
    }

    private static @NotNull List<Integer> rowSlots(int firstRow, int lastRow) {
        final List<Integer> slots = new ArrayList<>();
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = 1; column <= 7; column++) {
                slots.add(row * 9 + column);
            }
        }
        return List.copyOf(slots);
    }

    private void render(@NotNull Player player, @NotNull PlayerProfile profile) {
        inventory.clear();
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, MenuItems.filler());
        }

        final List<String> header = new ArrayList<>(skillLore(player, profile, skill));
        header.add("");
        header.add(LocaleLoader.getString("mcRPG.Skills.Info.Command", commandName(skill)));
        inventory.setItem(HEADER, MenuItems.item(GuiConfig.getInstance().getIcon(skill),
                ChatColor.YELLOW + skillName(skill), header,
                Specialization.roleOf(profile, skill) != SpecializationRole.UNSELECTED));

        inventory.setItem(ABILITIES_LABEL, MenuItems.item(Material.NETHER_STAR,
                LocaleLoader.getString("mcRPG.Skills.Abilities.Name"),
                MenuItems.wrap(LocaleLoader.getString("mcRPG.Skills.Abilities.Hint"),
                        ABILITY_LORE_WIDTH), false));
        final List<SubSkillType> abilities = abilities(player, skill);
        final int level = profile.getSkillLevel(skill);
        for (int i = 0; i < abilities.size() && i < ABILITY_SLOTS.size(); i++) {
            inventory.setItem(ABILITY_SLOTS.get(i), abilityIcon(abilities.get(i), level));
        }

        inventory.setItem(GUIDE_LABEL, MenuItems.item(Material.BOOKSHELF,
                LocaleLoader.getString("mcRPG.Skills.Guide.Name"),
                MenuItems.wrap(LocaleLoader.getString("mcRPG.Skills.Guide.Hint",
                        commandName(skill)), ABILITY_LORE_WIDTH), false));
        final List<List<String>> guide = guide(skill);
        for (int i = 0; i < guide.size() && i < GUIDE_SLOTS.size(); i++) {
            final List<String> page = guide.get(i);
            inventory.setItem(GUIDE_SLOTS.get(i), MenuItems.item(Material.BOOK, page.get(0),
                    page.subList(1, page.size()), false));
        }

        inventory.setItem(BACK, MenuItems.item(Material.ARROW,
                LocaleLoader.getString("mcRPG.Skills.Back")));
        inventory.setItem(CLOSE, MenuItems.item(Material.BARRIER,
                LocaleLoader.getString("mcRPG.Menu.Close")));
    }

    /**
     * How the player is doing in a skill: level, XP, category, XP rate and how to earn XP.
     * Shown on the skill's icon in the skills menu and at the top of its page.
     */
    static @NotNull List<String> skillLore(@NotNull Player player,
            @NotNull PlayerProfile profile, @NotNull PrimarySkillType skill) {
        final List<String> lore = new ArrayList<>();
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Level", profile.getSkillLevel(skill)));
        lore.add(LocaleLoader.getString("mcRPG.Skills.Skill.Xp", profile.getSkillXpLevel(skill),
                profile.getXpToLevel(skill)));
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Category",
                categoryName(SkillCategory.of(skill))));
        lore.add(LocaleLoader.getString("mcRPG.Menu.Skill.Rate",
                SpecializationDisplay.multiplierText(Specialization.roleOf(profile, skill))));
        lore.addAll(MenuItems.wrap(LocaleLoader.getString("mcRPG.Skills.Skill.XpGain",
                LocaleLoader.getString("Commands.XPGain."
                        + StringUtils.getCapitalized(skill.toString()))), ABILITY_LORE_WIDTH));
        if (!mcMMO.p.getSkillTools().doesPlayerHaveSkillPermission(player, skill)) {
            lore.add(LocaleLoader.getString("mcRPG.Skills.Skill.NoPermission"));
        }
        return lore;
    }

    /** The skill's command as typed in chat, e.g. "mining" (the same name /mining ? uses). */
    private static @NotNull String commandName(@NotNull PrimarySkillType skill) {
        return mcMMO.p.getSkillTools().getHeaderBannerSkillName(skill)
                .toLowerCase(Locale.ENGLISH);
    }

    /** The skill's abilities the player has permission for, as /&lt;skill&gt; lists them. */
    static @NotNull List<SubSkillType> abilities(@NotNull Player player,
            @NotNull PrimarySkillType skill) {
        return Arrays.stream(SubSkillType.values())
                .filter(ability -> ability.getParentSkill() == skill)
                .filter(ability -> Permissions.isSubSkillEnabled(player, ability))
                .toList();
    }

    private static @NotNull ItemStack abilityIcon(@NotNull SubSkillType ability, int level) {
        final int ranks = ability.getNumRanks();
        final int rank = rank(ability, level);
        final boolean unlocked = ranks == 0 || rank > 0;
        final String name = ChatColor.stripColor(ability.getLocaleName());
        return MenuItems.item(unlocked ? Material.LIME_DYE : Material.GRAY_DYE,
                (unlocked ? ChatColor.GREEN : ChatColor.GRAY) + name,
                abilityLore(ability, level), ranks > 1 && rank == ranks);
    }

    /** What an ability does, then whether it's unlocked, its rank and when the next one comes. */
    static @NotNull List<String> abilityLore(@NotNull SubSkillType ability, int level) {
        final List<String> lore = new ArrayList<>(MenuItems.wrap(
                ChatColor.GRAY + ChatColor.stripColor(ability.getLocaleDescription()),
                ABILITY_LORE_WIDTH));
        lore.add("");
        final int ranks = ability.getNumRanks();
        final int rank = rank(ability, level);
        if (ranks > 0 && rank == 0) {
            lore.add(LocaleLoader.getString("mcRPG.Skills.Ability.Locked",
                    RankUtils.getRankUnlockLevel(ability, 1)));
        } else if (ranks <= 1) {
            // One-rank abilities, and abilities with no ranks at all such as Roll
            lore.add(LocaleLoader.getString("mcRPG.Skills.Ability.Unlocked"));
        } else if (rank == ranks) {
            lore.add(LocaleLoader.getString("mcRPG.Skills.Ability.MaxRank", rank));
        } else {
            lore.add(LocaleLoader.getString("mcRPG.Skills.Ability.Rank", rank, ranks));
            lore.add(LocaleLoader.getString("mcRPG.Skills.Ability.NextRank",
                    RankUtils.getRankUnlockLevel(ability, rank + 1)));
        }
        return lore;
    }

    /** The highest rank of the ability unlocked at this skill level, or 0 if none is. */
    static int rank(@NotNull SubSkillType ability, int level) {
        for (int rank = ability.getNumRanks(); rank >= 1; rank--) {
            if (level >= RankUtils.getRankUnlockLevel(ability, rank)) {
                return rank;
            }
        }
        return 0;
    }

    /**
     * The skill's guide from the locale ({@code Guides.<Skill>.Section.<n>}), one list per
     * page: the page's heading, then its lines wrapped to fit a tooltip.
     */
    static @NotNull List<List<String>> guide(@NotNull PrimarySkillType skill) {
        final String keyPrefix = "Guides." + StringUtils.getCapitalized(skill.toString())
                + ".Section.";
        final List<List<String>> pages = new ArrayList<>();
        for (int section = 0; section < MAX_GUIDE_PAGES; section++) {
            final String text = LocaleLoader.getString(keyPrefix + section);
            if (text.startsWith("!")) {
                break; // no such page
            }
            final List<String> lines = new ArrayList<>(MenuItems.wrap(text, GUIDE_LORE_WIDTH));
            while (lines.size() > 1 && ChatColor.stripColor(lines.get(lines.size() - 1))
                    .isBlank()) {
                lines.remove(lines.size() - 1);
            }
            pages.add(lines);
        }
        return pages;
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
        if (rawSlot != BACK) {
            return;
        }

        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) {
            player.closeInventory();
            player.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
            return;
        }
        SkillsMenu.open(player, mmoPlayer);
    }
}
