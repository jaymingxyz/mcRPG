package com.gmail.nossr50.mcrpg.commands;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lets the admin skill commands (/rpgaddxp, /rpgaddlevels, /rpgsetlevel and /rpgskillreset)
 * take a category, such as {@code blacksmithing}, wherever they take a skill. A category
 * stands for each of its skills that exists on this server, and the command runs for each
 * one.
 */
public final class CategoryArgument {
    private CategoryArgument() {
    }

    /**
     * The skills this argument names as a category, or null if it's a skill or anything else.
     * A skill with the same name wins, so no existing skill argument changes meaning.
     */
    public static @Nullable List<PrimarySkillType> skillsOf(@NotNull String argument) {
        if (mcMMO.p.getSkillTools().matchSkill(argument) != null) {
            return null;
        }
        final SkillCategory category = SkillCategory.fromCommandName(argument);
        return category == null ? null : category.availableSkills();
    }

    public static boolean isCategory(@NotNull String argument) {
        return skillsOf(argument) != null;
    }

    /**
     * The confirmation for whoever ran the command, e.g. "Blacksmithing has been modified for
     * bob.", or null if the argument isn't a category.
     */
    public static @Nullable String modifiedMessage(@NotNull String argument,
            @NotNull String playerName) {
        if (!isCategory(argument)) {
            return null;
        }
        final SkillCategory category = SkillCategory.fromCommandName(argument);
        return LocaleLoader.getString("Commands.addlevels.AwardSkill.2",
                SpecializationDisplay.categoryName(category), playerName);
    }

    /** The skill names for tab completion, followed by the category names. */
    public static @NotNull List<String> skillsAndCategories(@NotNull List<String> skillNames) {
        final List<String> names = new ArrayList<>(skillNames);
        names.addAll(ChooseSpecializationCommand.categoryNames());
        return names;
    }
}
