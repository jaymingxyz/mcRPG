package com.gmail.nossr50.placeholders;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * mcRPG: %mcrpg_primary_specialization% and %mcrpg_secondary_specialization%. The chosen
 * category's name, or an empty string if it isn't chosen or the player's data isn't loaded.
 */
public class SpecializationPlaceholder implements Placeholder {
    private final SpecializationSlot slot;

    public SpecializationPlaceholder(@NotNull SpecializationSlot slot) {
        this.slot = slot;
    }

    @Override
    public String process(Player player, String params) {
        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) {
            return "";
        }
        final SkillCategory category = mmoPlayer.getProfile().getSpecialization(slot);
        return category == null ? "" : SpecializationDisplay.categoryName(category);
    }

    @Override
    public String getName() {
        return slot.commandName() + "_specialization";
    }
}
