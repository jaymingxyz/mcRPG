package com.gmail.nossr50.mcrpg.specialization;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.categoryName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.passive.CategoryPassiveDisplay;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Player-facing Specialization actions shared by /choosespecialization and the
 * Specialization menu: permission checks and the messages that explain each result.
 */
public final class SpecializationActions {
    private SpecializationActions() {
    }

    /**
     * Whether the player may choose this category: they need the skill permission for at least
     * one of its skills on this server.
     */
    public static boolean canUseCategory(@NotNull Player player,
            @NotNull SkillCategory category) {
        return category.availableSkills().stream().anyMatch(
                skill -> mcMMO.p.getSkillTools().doesPlayerHaveSkillPermission(player, skill));
    }

    /**
     * Tries to choose a category as one of the player's Specializations and tells them the
     * result.
     *
     * @return true if it was chosen
     */
    public static boolean tryChoose(@NotNull Player player, @NotNull PlayerProfile profile,
            @NotNull SpecializationSlot slot, @NotNull SkillCategory category) {
        if (!canUseCategory(player, category)) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Choose.NoPermission",
                    categoryName(category)));
            return false;
        }

        final Specialization.ChooseResult result = Specialization.choose(profile, slot, category);
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(LocaleLoader.getString("mcRPG.Choose.Success",
                        categoryName(category), slotName(slot),
                        SpecializationDisplay.skillList(category),
                        SpecializationDisplay.multiplierText(slot.role())));
                final String passive = CategoryPassiveDisplay.chooseMessage(profile, category);
                if (passive != null) {
                    player.sendMessage(passive);
                }
            }
            case SLOT_FILLED -> {
                final SkillCategory current = profile.getSpecialization(slot);
                player.sendMessage(LocaleLoader.getString("mcRPG.Choose.SlotFilled",
                        slotName(slot), current == null ? "" : categoryName(current),
                        current == null ? "" : current.commandName()));
            }
            case CATEGORY_IN_OTHER_SLOT -> player.sendMessage(LocaleLoader.getString(
                    "mcRPG.Choose.CategoryInOtherSlot", categoryName(category),
                    slotName(slot.other())));
        }
        return result == Specialization.ChooseResult.SUCCESS;
    }
}
