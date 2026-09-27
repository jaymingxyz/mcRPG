package com.gmail.nossr50.mcrpg.specialization;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Player-facing specialization actions shared by /chooseskill and the Specialization menu,
 * so both enforce the same rules and send the same messages.
 */
public final class SpecializationActions {
    private SpecializationActions() {
    }

    /**
     * Whether the player may choose this skill at all: they need its skill permission.
     */
    public static boolean canUseSkill(@NotNull Player player, @NotNull PrimarySkillType skill) {
        return mcMMO.p.getSkillTools().doesPlayerHaveSkillPermission(player, skill);
    }

    /**
     * Tries to put a skill in a slot for this player and tells them the result.
     *
     * @return true if the slot was filled
     */
    public static boolean tryChoose(@NotNull Player player, @NotNull PlayerProfile profile,
            @NotNull SpecializationSlot slot, @NotNull PrimarySkillType skill) {
        if (!canUseSkill(player, skill)) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Choose.NoPermission",
                    skillName(skill)));
            return false;
        }

        final Specialization.ChooseResult result = Specialization.choose(profile, slot, skill);
        switch (result) {
            case SUCCESS -> player.sendMessage(LocaleLoader.getString("mcRPG.Choose.Success",
                    skillName(skill), slotName(slot),
                    SpecializationDisplay.multiplierText(slot.role())));
            case SLOT_FILLED -> {
                final PrimarySkillType current = profile.getSpecialization(slot);
                player.sendMessage(LocaleLoader.getString("mcRPG.Choose.SlotFilled",
                        slotName(slot), current == null ? "" : skillName(current),
                        current == null ? "" : current.name().toLowerCase(Locale.ROOT)));
            }
            case SKILL_IN_OTHER_SLOT -> player.sendMessage(LocaleLoader.getString(
                    "mcRPG.Choose.SkillInOtherSlot", skillName(skill), slotName(slot.other())));
            case SKILL_UNAVAILABLE -> player.sendMessage(LocaleLoader.getString(
                    "mcRPG.Choose.Unavailable", skillName(skill)));
        }
        return result == Specialization.ChooseResult.SUCCESS;
    }
}
