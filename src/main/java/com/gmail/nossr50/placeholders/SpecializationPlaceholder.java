package com.gmail.nossr50.placeholders;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * mcRPG: %mcrpg_primary_skill% and %mcrpg_secondary_skill%. The skill's name, or an empty
 * string if the slot is empty or the player's data isn't loaded.
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
        final PrimarySkillType skill = mmoPlayer.getProfile().getSpecialization(slot);
        return skill == null ? "" : SpecializationDisplay.skillName(skill);
    }

    @Override
    public String getName() {
        return slot.commandName() + "_skill";
    }
}
