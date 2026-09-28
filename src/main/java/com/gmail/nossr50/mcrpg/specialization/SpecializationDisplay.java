package com.gmail.nossr50.mcrpg.specialization;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.passive.CategoryPassiveDisplay;
import com.gmail.nossr50.util.commands.CommandUtils;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Text shown to players about Specializations: names, tags, and the skill list by category. */
public final class SpecializationDisplay {
    private SpecializationDisplay() {
    }

    /** Formats a multiplier or percentage without trailing zeros: 1.25, 1, 0.35, 10. */
    public static @NotNull String formatNumber(double value) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US))
                .format(value);
    }

    public static @NotNull String skillName(@NotNull PrimarySkillType skill) {
        return mcMMO.p.getSkillTools().getLocalizedSkillName(skill);
    }

    /** A category's plain name, e.g. "Metallurgy". */
    public static @NotNull String categoryName(@NotNull SkillCategory category) {
        return LocaleLoader.getString(category.nameKey());
    }

    /** The category's skills on this server, e.g. "Mining, Smelting, Excavation". */
    public static @NotNull String skillList(@NotNull SkillCategory category) {
        return String.join(", ", category.availableSkills().stream()
                .map(SpecializationDisplay::skillName).toList());
    }

    public static @NotNull String slotName(@NotNull SpecializationSlot slot) {
        return LocaleLoader.getString(slot == SpecializationSlot.PRIMARY
                ? "mcRPG.Slot.Primary" : "mcRPG.Slot.Secondary");
    }

    public static @NotNull String multiplierText(@NotNull SpecializationRole role) {
        return formatNumber(ExperienceConfig.getInstance().getSpecializationMultiplier(role));
    }

    /** The category chosen as a Specialization, or "none". */
    public static @NotNull String slotContents(@NotNull PlayerProfile profile,
            @NotNull SpecializationSlot slot) {
        final SkillCategory category = profile.getSpecialization(slot);
        return category == null ? LocaleLoader.getString("mcRPG.Specialization.None")
                : categoryName(category);
    }

    /** "Primary: Metallurgy | Secondary: none" */
    public static @NotNull String summary(@NotNull PlayerProfile profile) {
        return LocaleLoader.getString("mcRPG.Specialization.Summary",
                slotContents(profile, SpecializationSlot.PRIMARY),
                slotContents(profile, SpecializationSlot.SECONDARY));
    }

    /** The tag after a skill in /rpgstats, e.g. "(Primary, 1.25x XP)" or "(0.35x XP)". */
    public static @NotNull String statsTag(@NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        return roleTag(Specialization.roleOf(profile, skill));
    }

    /** "(Primary, 1.25x XP)", "(Secondary, 1x XP)" or "(0.35x XP)". */
    public static @NotNull String roleTag(@NotNull SpecializationRole role) {
        final String key = switch (role) {
            case PRIMARY -> "mcRPG.Specialization.Tag.Primary";
            case SECONDARY -> "mcRPG.Specialization.Tag.Secondary";
            case UNSELECTED -> "mcRPG.Specialization.Tag.Unselected";
        };
        return LocaleLoader.getString(key, multiplierText(role));
    }

    /** Adds " (Primary)" or " (Secondary)" to an XP bar title. Unselected skills get no tag. */
    public static @NotNull String xpBarTitle(@NotNull String title, @NotNull PlayerProfile profile,
            @NotNull PrimarySkillType skill) {
        return switch (Specialization.roleOf(profile, skill)) {
            case PRIMARY -> title + LocaleLoader.getString("mcRPG.Specialization.XpBarTag.Primary");
            case SECONDARY ->
                    title + LocaleLoader.getString("mcRPG.Specialization.XpBarTag.Secondary");
            case UNSELECTED -> title;
        };
    }

    /**
     * Sends the Specializations summary and every available skill, grouped by category. A
     * category chosen as a Specialization is tagged in its header. Used by /rpgstats and
     * /rpginspect.
     *
     * @param display who receives the lines
     * @param profile whose skills to show
     * @param permissionTarget if not null, skills this player has no permission for are left
     *         out (online players); offline profiles pass null and show every skill
     */
    public static void sendSkillsByCategory(@NotNull CommandSender display,
            @NotNull PlayerProfile profile, @Nullable Player permissionTarget) {
        display.sendMessage(summary(profile));
        for (SkillCategory category : SkillCategory.values()) {
            final String header = LocaleLoader.getString(category.localeKey());
            final SpecializationSlot slot = Specialization.slotOf(profile, category);
            display.sendMessage(slot == null ? header : header + " " + roleTag(slot.role()));
            for (PrimarySkillType skill : category.availableSkills()) {
                if (permissionTarget == null || mcMMO.p.getSkillTools()
                        .doesPlayerHaveSkillPermission(permissionTarget, skill)) {
                    display.sendMessage(CommandUtils.displaySkill(profile, skill));
                }
            }
            final String passive = CategoryPassiveDisplay.statsLine(profile, category);
            if (passive != null) {
                display.sendMessage(passive);
            }
        }
    }
}
