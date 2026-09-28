package com.gmail.nossr50.mcrpg.passive;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.categoryName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.formatNumber;

import com.gmail.nossr50.config.GeneralConfig;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Text shown to players about category passives: in /rpgstats, the Specialization menu, and
 * the messages for choosing, unlocking and abandoning.
 */
public final class CategoryPassiveDisplay {
    /** Menu lore lines are wrapped at about this many characters. */
    private static final int LORE_WIDTH = 40;

    private CategoryPassiveDisplay() {
    }

    public static @NotNull String name(@NotNull CategoryPassive passive) {
        return LocaleLoader.getString(passive.nameKey());
    }

    /** What the passive does with the configured numbers, e.g. "Wooden tools mine 50% faster…" */
    public static @NotNull String effect(@NotNull CategoryPassive passive) {
        final GeneralConfig config = mcMMO.p.getGeneralConfig();
        final String durability =
                formatNumber(config.getCategoryPassiveDurabilityLossReduction(passive));
        if (!passive.isArmorMastery()) {
            return LocaleLoader.getString(passive.effectKey(),
                    formatNumber(config.getCategoryPassiveMiningSpeedBonus(passive)), durability);
        }
        final double toughness = config.getCategoryPassiveBonusToughness(passive);
        final String toughnessText = toughness > 0
                ? LocaleLoader.getString("mcRPG.Passive.Effect.Toughness", formatNumber(toughness))
                : "";
        return LocaleLoader.getString(passive.effectKey(),
                formatNumber(config.getCategoryPassiveBonusArmor(passive)), toughnessText,
                durability);
    }

    /** Why a locked passive isn't working yet, or null if it's active or turned off. */
    public static @Nullable String requirement(@NotNull PlayerProfile profile,
            @NotNull CategoryPassive passive) {
        final int level = CategoryPassives.categoryLevel(profile, passive.category());
        final int needed = CategoryPassives.unlockLevel();
        return switch (CategoryPassives.status(profile, passive)) {
            case ACTIVE, DISABLED -> null;
            case NOT_SPECIALIZED -> level >= needed
                    ? LocaleLoader.getString("mcRPG.Passive.Requirement.Specialization",
                            categoryName(passive.category()))
                    : LocaleLoader.getString("mcRPG.Passive.Requirement.SpecializationAndLevels",
                            categoryName(passive.category()), needed, level);
            case NEEDS_LEVELS -> LocaleLoader.getString("mcRPG.Passive.Requirement.Levels",
                    needed, level);
        };
    }

    /**
     * The /rpgstats line for the category's passive, or null if it has none or passives are
     * turned off.
     */
    public static @Nullable String statsLine(@NotNull PlayerProfile profile,
            @NotNull SkillCategory category) {
        final CategoryPassive passive = CategoryPassive.of(category);
        if (passive == null
                || CategoryPassives.status(profile, passive) == CategoryPassives.Status.DISABLED) {
            return null;
        }
        final String requirement = requirement(profile, passive);
        return requirement == null
                ? LocaleLoader.getString("mcRPG.Passive.Stats.Active", name(passive),
                        effect(passive))
                : LocaleLoader.getString("mcRPG.Passive.Stats.Locked", name(passive),
                        requirement);
    }

    /**
     * Lines for a category's label in the Specialization menu: the passive, what it does, and
     * whether it's active. Empty if the category has no passive or passives are turned off.
     */
    public static @NotNull List<String> menuLore(@NotNull PlayerProfile profile,
            @NotNull SkillCategory category) {
        final CategoryPassive passive = CategoryPassive.of(category);
        if (passive == null
                || CategoryPassives.status(profile, passive) == CategoryPassives.Status.DISABLED) {
            return List.of();
        }
        final List<String> lore = new ArrayList<>();
        lore.add(LocaleLoader.getString("mcRPG.Menu.Category.Passive", name(passive)));
        lore.addAll(wrap(effect(passive), ChatColor.GRAY));
        final String requirement = requirement(profile, passive);
        if (requirement == null) {
            lore.add(LocaleLoader.getString("mcRPG.Menu.Category.PassiveActive"));
        } else {
            lore.addAll(wrap(LocaleLoader.getString("mcRPG.Menu.Category.PassiveLocked",
                    requirement), ChatColor.RED));
        }
        return lore;
    }

    /**
     * Sent after a category is chosen as a Specialization: its passive is now active, or what
     * it still needs. Null if the category has no passive or passives are turned off.
     */
    public static @Nullable String chooseMessage(@NotNull PlayerProfile profile,
            @NotNull SkillCategory category) {
        final CategoryPassive passive = CategoryPassive.of(category);
        if (passive == null) {
            return null;
        }
        return switch (CategoryPassives.status(profile, passive)) {
            case ACTIVE -> unlockMessage(passive);
            case NEEDS_LEVELS -> LocaleLoader.getString("mcRPG.Passive.Choose.Locked",
                    name(passive), CategoryPassives.unlockLevel(), categoryName(category),
                    CategoryPassives.categoryLevel(profile, category));
            case DISABLED, NOT_SPECIALIZED -> null;
        };
    }

    /** "Leather Mastery unlocked! Each piece of leather armor…" */
    public static @NotNull String unlockMessage(@NotNull CategoryPassive passive) {
        return LocaleLoader.getString("mcRPG.Passive.Unlocked", name(passive), effect(passive));
    }

    /**
     * Added to the /abandonspecialization warning when the category's passive is active, or
     * null otherwise.
     */
    public static @Nullable String abandonWarning(@NotNull PlayerProfile profile,
            @NotNull SkillCategory category) {
        final CategoryPassive passive = CategoryPassive.of(category);
        return passive != null && CategoryPassives.isActive(profile, passive)
                ? LocaleLoader.getString("mcRPG.Passive.Abandon.Warning", name(passive))
                : null;
    }

    /** Splits text into lines of about {@link #LORE_WIDTH} characters, each in this color. */
    static @NotNull List<String> wrap(@NotNull String text, @NotNull ChatColor color) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (String word : ChatColor.stripColor(text).split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > LORE_WIDTH) {
                lines.add(color + line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(color + line.toString());
        }
        return lines;
    }
}
