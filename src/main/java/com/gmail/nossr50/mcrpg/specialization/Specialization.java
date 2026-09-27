package com.gmail.nossr50.mcrpg.specialization;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.experience.FormulaType;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * mcRPG's specialization rules: which slot a skill is in, the XP multiplier it earns, choosing
 * a skill, and abandoning one. Everything here works on a {@link PlayerProfile}, so it applies
 * to online and offline players alike. Permission checks and messages are left to callers.
 */
public final class Specialization {
    private Specialization() {
    }

    /** Result of trying to put a skill in a slot. */
    public enum ChooseResult {
        SUCCESS,
        /** The slot already holds a skill; only /abandonskill can empty it. */
        SLOT_FILLED,
        /** The skill is already in the player's other slot. */
        SKILL_IN_OTHER_SLOT,
        /** The skill doesn't exist on this server's Minecraft version. */
        SKILL_UNAVAILABLE
    }

    /**
     * What abandoning a skill would do, worked out before anything changes so it can be shown
     * in the warning and then applied unchanged.
     *
     * @param skill the skill being abandoned
     * @param slot the slot it's being removed from
     * @param oldLevel its level before abandoning
     * @param newLevel its level after abandoning
     * @param newXp its XP toward the next level after abandoning
     */
    public record AbandonPreview(@NotNull PrimarySkillType skill, @NotNull SpecializationSlot slot,
            int oldLevel, int newLevel, float newXp) {
        public int levelsLost() {
            return oldLevel - newLevel;
        }
    }

    /** The slot holding this skill, or {@code null} if it isn't in either slot. */
    public static @Nullable SpecializationSlot slotOf(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        for (SpecializationSlot slot : SpecializationSlot.values()) {
            if (profile.getSpecialization(slot) == skill) {
                return slot;
            }
        }
        return null;
    }

    /** The role this skill has for this player: Primary, Secondary, or not selected. */
    public static @NotNull SpecializationRole roleOf(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        final SpecializationSlot slot = slotOf(profile, skill);
        return slot == null ? SpecializationRole.UNSELECTED : slot.role();
    }

    /** The XP multiplier this skill earns for this player. */
    public static double xpMultiplier(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        return ExperienceConfig.getInstance().getSpecializationMultiplier(roleOf(profile, skill));
    }

    /** True if either slot is still empty. */
    public static boolean hasEmptySlot(@NotNull PlayerProfile profile) {
        for (SpecializationSlot slot : SpecializationSlot.values()) {
            if (profile.getSpecialization(slot) == null) {
                return true;
            }
        }
        return false;
    }

    /** Checks the rules for putting a skill in a slot without changing anything. */
    public static @NotNull ChooseResult checkChoose(@NotNull PlayerProfile profile,
            @NotNull SpecializationSlot slot, @NotNull PrimarySkillType skill) {
        if (!SkillCategory.isAvailable(skill)) {
            return ChooseResult.SKILL_UNAVAILABLE;
        }
        if (profile.getSpecialization(slot) != null) {
            return ChooseResult.SLOT_FILLED;
        }
        if (profile.getSpecialization(slot.other()) == skill) {
            return ChooseResult.SKILL_IN_OTHER_SLOT;
        }
        return ChooseResult.SUCCESS;
    }

    /**
     * Puts a skill in an empty slot if the rules allow it. The skill keeps all its progress.
     *
     * @return {@link ChooseResult#SUCCESS} if the slot was filled, otherwise why it wasn't
     */
    public static @NotNull ChooseResult choose(@NotNull PlayerProfile profile,
            @NotNull SpecializationSlot slot, @NotNull PrimarySkillType skill) {
        final ChooseResult result = checkChoose(profile, slot, skill);
        if (result == ChooseResult.SUCCESS) {
            profile.setSpecialization(slot, skill);
        }
        return result;
    }

    /**
     * Works out what abandoning a skill would do, without changing anything.
     *
     * @return the preview, or {@code null} if the skill isn't in either slot
     */
    public static @Nullable AbandonPreview previewAbandon(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        final SpecializationSlot slot = slotOf(profile, skill);
        if (slot == null) {
            return null;
        }

        final int oldLevel = profile.getSkillLevel(skill);
        final double keptPercent = mcMMO.p.getGeneralConfig().getAbandonXpKeptPercent();
        final long totalXp = totalXp(oldLevel, profile.getSkillXpLevelRaw(skill));
        final long keptXp = (long) Math.floor(totalXp * keptPercent / 100D);
        final LevelAndXp kept = levelFromTotalXp(keptXp,
                mcMMO.p.getSkillTools().getLevelCap(skill));

        return new AbandonPreview(skill, slot, oldLevel, kept.level(), kept.xp());
    }

    /**
     * Abandons a skill: sets it to the level and XP from the preview and empties its slot.
     * The preview must come from {@link #previewAbandon} for the same profile.
     */
    public static void applyAbandon(@NotNull PlayerProfile profile,
            @NotNull AbandonPreview preview) {
        // modifySkill also clears XP toward the next level, so set the leftover XP after it
        profile.modifySkill(preview.skill(), preview.newLevel());
        profile.setSkillXpLevel(preview.skill(), preview.newXp());
        profile.setSpecialization(preview.slot(), null);
    }

    /**
     * Empties whichever slot holds this skill, if any. Used when an admin resets a skill.
     */
    public static void clearSlotHolding(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        final SpecializationSlot slot = slotOf(profile, skill);
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
