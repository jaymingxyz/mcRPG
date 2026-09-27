package com.gmail.nossr50.commands.experience;

import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import org.bukkit.entity.Player;

/** Lets mcRPG tests run /skillreset's reset step, which is protected. */
public final class SkillresetTestAccess {
    private SkillresetTestAccess() {
    }

    public static void reset(Player player, PlayerProfile profile, PrimarySkillType skill) {
        new SkillresetCommand().handleCommand(player, profile, skill);
    }
}
