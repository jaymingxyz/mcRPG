package com.gmail.nossr50.mcrpg.specialization;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.experience.FormulaType;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * mcRPG's Specialization rules. A player's Primary and Secondary Specializations are each a
 * {@link SkillCategory}, and every skill in a chosen category earns that Specialization's XP
 * multiplier. This class covers which Specialization a skill belongs to, the multiplier it
 * earns, choosing a category, and abandoning one. Everything here works on a
 * {@link PlayerProfile}, so it applies to online and offline players alike. Permission checks
 * and messages are left to callers.
 */
public final class Specialization {
    private Specialization() {
    }

    /** Result of trying to choose a category as a Specialization. */
    public enum ChooseResult {
        SUCCESS,
        /** That Specialization is already chosen; only /abandonspecialization can clear it. */
        SLOT_FILLED,
        /** The category is already the player's other Specialization. */
        CATEGORY_IN_OTHER_SLOT
    }

    /**
     * What abandoning one skill of a Specialization would do.
     *
     * @param skill the skill
     * @param oldLevel its level before abandoning
     * @param newLevel its level after abandoning
     * @param newXp its XP toward the next level after abandoning
     */
    public record SkillAbandon(@NotNull PrimarySkillType skill, int oldLevel, int newLevel,
            float newXp) {
        public int levelsLost() {
            return oldLevel - newLevel;
        }
    }

    /**
     * What abandoning a Specialization would do, worked out before anything changes so it can
     * be shown in the warning and then applied unchanged.
     *
     * @param category the category being abandoned
     * @param slot the Specialization it's being removed from
     * @param skills what happens to each of the category's skills
     */
    public record AbandonPreview(@NotNull SkillCategory category, @NotNull SpecializationSlot slot,
            @NotNull List<SkillAbandon> skills) {
    }

    /** Which Specialization this category is, or {@code null} if it isn't chosen. */
    public static @Nullable SpecializationSlot slotOf(@NotNull PlayerProfile profile,
            @NotNull SkillCategory category) {
        for (SpecializationSlot slot : SpecializationSlot.values()) {
            if (profile.getSpecialization(slot) == category) {
                return slot;
            }
        }
        return null;
    }

    /**
     * The role this skill has for this player: in their Primary Specialization's category,
     * their Secondary's, or neither.
     */
    public static @NotNull SpecializationRole roleOf(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        final SpecializationSlot slot = slotOf(profile, SkillCategory.of(skill));
        return slot == null ? SpecializationRole.UNSELECTED : slot.role();
    }

    /** The XP multiplier this skill earns for this player. */
    public static double xpMultiplier(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        return ExperienceConfig.getInstance().getSpecializationMultiplier(roleOf(profile, skill));
    }

    /** True if either Specialization hasn't been chosen yet. */
    public static boolean hasEmptySlot(@NotNull PlayerProfile profile) {
        for (SpecializationSlot slot : SpecializationSlot.values()) {
            if (profile.getSpecialization(slot) == null) {
                return true;
            }
        }
        return false;
    }

    /** Checks the rules for choosing a category as a Specialization without changing anything. */
    public static @NotNull ChooseResult checkChoose(@NotNull PlayerProfile profile,
            @NotNull SpecializationSlot slot, @NotNull SkillCategory category) {
        if (profile.getSpecialization(slot) != null) {
            return ChooseResult.SLOT_FILLED;
        }
        if (profile.getSpecialization(slot.other()) == category) {
            return ChooseResult.CATEGORY_IN_OTHER_SLOT;
        }
        return ChooseResult.SUCCESS;
    }

    /**
     * Chooses a category as a Specialization if the rules allow it. Its skills keep all their
     * progress.
     *
     * @return {@link ChooseResult#SUCCESS} if it was chosen, otherwise why it wasn't
     */
    public static @NotNull ChooseResult choose(@NotNull PlayerProfile profile,
            @NotNull SpecializationSlot slot, @NotNull SkillCategory category) {
        final ChooseResult result = checkChoose(profile, slot, category);
        if (result == ChooseResult.SUCCESS) {
            profile.setSpecialization(slot, category);
        }
        return result;
    }

    /**
     * Works out what abandoning a Specialization would do, without changing anything. Every
     * skill in the category that exists on this server keeps part of its total XP (the
     * configured percentage, 10% by default).
     *
     * @return the preview, or {@code null} if the category isn't one of the player's
     *         Specializations
     */
    public static @Nullable AbandonPreview previewAbandon(@NotNull PlayerProfile profile,
            @NotNull SkillCategory category) {
        final SpecializationSlot slot = slotOf(profile, category);
        if (slot == null) {
            return null;
        }

        final double keptPercent = mcMMO.p.getGeneralConfig().getAbandonXpKeptPercent();
        final List<SkillAbandon> skills = new ArrayList<>();
        for (PrimarySkillType skill : category.availableSkills()) {
            final int oldLevel = profile.getSkillLevel(skill);
            final long totalXp = totalXp(oldLevel, profile.getSkillXpLevelRaw(skill));
            final long keptXp = (long) Math.floor(totalXp * keptPercent / 100D);
            final LevelAndXp kept = levelFromTotalXp(keptXp,
                    mcMMO.p.getSkillTools().getLevelCap(skill));
            skills.add(new SkillAbandon(skill, oldLevel, kept.level(), kept.xp()));
        }
        return new AbandonPreview(category, slot, List.copyOf(skills));
    }

    /**
     * Abandons a Specialization: sets each of its skills to the level and XP from the preview
     * and clears the Specialization. The preview must come from {@link #previewAbandon} for
     * the same profile.
     */
    public static void applyAbandon(@NotNull PlayerProfile profile,
            @NotNull AbandonPreview preview) {
        for (SkillAbandon each : preview.skills()) {
            // modifySkill also clears XP toward the next level, so set the leftover XP after it
            profile.modifySkill(each.skill(), each.newLevel());
            profile.setSkillXpLevel(each.skill(), each.newXp());
        }
        profile.setSpecialization(preview.slot(), null);
    }

    /**
     * Clears the Specialization whose category holds this skill, if any. Used when an admin
     * resets a skill.
     */
    public static void clearSlotHolding(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        final SpecializationSlot slot = slotOf(profile, SkillCategory.of(skill));
        if (slot != null) {
            profile.setSpecialization(slot, null);
        }
    }

    /** A level and the XP toward the next one. */
    public record LevelAndXp(int level, float xp) {
    }

    /**
     * All the XP needed to reach {@code level} from 0, plus the XP toward the next level.
     * Uses {@code long} because totals pass {@link Integer#MAX_VALUE} around level 1,460 on
     * the default curve, and skill levels have no cap by default.
     */
    static long totalXp(int level, float xpTowardNextLevel) {
        final FormulaType formulaType = ExperienceConfig.getInstance().getFormulaType();
        long total = 0;
        for (int each = 0; each < level; each++) {
            total += mcMMO.getFormulaManager().getXPtoNextLevel(each, formulaType);
        }
        return total + (long) Math.floor(xpTowardNextLevel);
    }

    /**
     * The level reached by earning {@code totalXp} from level 0, and the XP left over toward
     * the next level. Stops at {@code levelCap}, like mcMMO's own level conversion.
     */
    static @NotNull LevelAndXp levelFromTotalXp(long totalXp, int levelCap) {
        final FormulaType formulaType = ExperienceConfig.getInstance().getFormulaType();
        long remaining = totalXp;
        int level = 0;
        while (level < levelCap) {
            final int needed = mcMMO.getFormulaManager().getXPtoNextLevel(level, formulaType);
            if (remaining < needed) {
                break;
            }
            remaining -= needed;
            level++;
        }
        return new LevelAndXp(level, level < levelCap ? remaining : 0F);
    }
}
