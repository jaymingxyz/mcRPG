package com.gmail.nossr50.mcrpg.specialization;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * mcRPG's six skill categories. A player's Primary and Secondary Specializations are each one
 * of these categories, and every skill in a chosen category earns that Specialization's XP
 * rate. They also organize the skill list in /rpgstats and the Specialization menu. The menu
 * gives each category one row, so a category holds at most {@link #MAX_SKILLS_PER_CATEGORY}
 * skills.
 */
public enum SkillCategory {
    MELEE(PrimarySkillType.SWORDS, PrimarySkillType.AXES, PrimarySkillType.MACES,
            PrimarySkillType.SPEARS),
    RANGED(PrimarySkillType.ARCHERY, PrimarySkillType.CROSSBOWS, PrimarySkillType.TRIDENTS),
    METALLURGY(PrimarySkillType.MINING, PrimarySkillType.SMELTING, PrimarySkillType.EXCAVATION),
    BOTANY(PrimarySkillType.WOODCUTTING, PrimarySkillType.HERBALISM, PrimarySkillType.ALCHEMY),
    BLACKSMITHING(PrimarySkillType.REPAIR, PrimarySkillType.SALVAGE),
    SURVIVALISM(PrimarySkillType.TAMING, PrimarySkillType.ACROBATICS, PrimarySkillType.FISHING,
            PrimarySkillType.UNARMED);

    public static final int MAX_SKILLS_PER_CATEGORY = 4;

    private static final Map<PrimarySkillType, SkillCategory> BY_SKILL =
            new EnumMap<>(PrimarySkillType.class);

    static {
        for (SkillCategory category : values()) {
            for (PrimarySkillType skill : category.skills) {
                BY_SKILL.put(skill, category);
            }
        }
    }

    private final @NotNull List<PrimarySkillType> skills;

    SkillCategory(@NotNull PrimarySkillType... skills) {
        this.skills = List.of(skills);
    }

    /**
     * Every skill in this category, in display order, including skills that don't exist on
     * this server's Minecraft version.
     */
    public @NotNull List<PrimarySkillType> skills() {
        return skills;
    }

    /** The skills in this category that exist on this server's Minecraft version. */
    public @NotNull List<PrimarySkillType> availableSkills() {
        return skills.stream().filter(SkillCategory::isAvailable).toList();
    }

    /** The locale key for this category's /rpgstats header, e.g. {@code mcRPG.Category.Melee}. */
    public @NotNull String localeKey() {
        final String name = name();
        return "mcRPG.Category." + name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    /** The locale key for this category's plain name, e.g. "Melee Combat". */
    public @NotNull String nameKey() {
        return localeKey() + ".Name";
    }

    /** The name used in commands, e.g. {@code metallurgy}. */
    public @NotNull String commandName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses a category's command name, ignoring case, or returns null. */
    public static @Nullable SkillCategory fromCommandName(@NotNull String name) {
        for (SkillCategory category : values()) {
            if (category.commandName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }

    /**
     * Reads a stored Specialization: a category name, or null for anything else. mcRPG builds
     * before 2026-09-27 stored a single skill instead, which is read as that skill's category.
     */
    public static @Nullable SkillCategory fromStoredName(@Nullable String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        final String name = stored.trim().toUpperCase(Locale.ROOT);
        for (SkillCategory category : values()) {
            if (category.name().equals(name)) {
                return category;
            }
        }
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            if (skill.name().equals(name)) {
                return of(skill);
            }
        }
        return null;
    }

    public static @NotNull SkillCategory of(@NotNull PrimarySkillType skill) {
        final SkillCategory category = BY_SKILL.get(skill);
        if (category == null) {
            throw new IllegalArgumentException(skill + " has no mcRPG Specialization category");
        }
        return category;
    }

    /**
     * Maces and Spears only exist on newer Minecraft versions. mcMMO uses the same version
     * checks when it decides which skills to show and register.
     */
    public static boolean isAvailable(@NotNull PrimarySkillType skill) {
        return switch (skill) {
            case MACES -> mcMMO.getMinecraftGameVersion().isAtLeast(1, 21, 0);
            case SPEARS -> mcMMO.getMinecraftGameVersion().isAtLeast(1, 21, 11);
            default -> true;
        };
    }
}
