package com.gmail.nossr50.mcrpg.commands;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * /rpgsetskill &lt;player&gt; &lt;primary|secondary&gt; &lt;skill|none&gt; lets admins set or
 * clear a player's slot without changing any skill progress, for example to fix a mistake.
 * Works on online and offline players, like mcMMO's other admin commands.
 */
public class SetSkillCommand implements TabExecutor {
    private static final String NONE = "none";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(LocaleLoader.getString("mcRPG.SetSkill.Usage"));
            return true;
        }

        final SpecializationSlot slot = SpecializationSlot.fromCommandName(args[1]);
        if (slot == null) {
            sender.sendMessage(LocaleLoader.getString("mcRPG.Choose.InvalidSlot", args[1]));
            return true;
        }

        final @Nullable PrimarySkillType skill;
        if (args[2].equalsIgnoreCase(NONE)) {
            skill = null;
        } else if (CommandUtils.isInvalidSkill(sender, args[2])) {
            return true;
        } else {
            skill = mcMMO.p.getSkillTools().matchSkill(args[2]);
            if (!SkillCategory.isAvailable(skill)) {
                sender.sendMessage(LocaleLoader.getString("mcRPG.Choose.Unavailable",
                        skillName(skill)));
                return true;
            }
        }

        final String playerName = CommandUtils.getMatchedPlayerName(args[0]);
        final McMMOPlayer mmoPlayer = UserManager.getOfflinePlayer(playerName);
        final PlayerProfile profile;
        if (mmoPlayer != null) {
            profile = mmoPlayer.getProfile();
        } else {
            final PlayerProfile stored = mcMMO.getDatabaseManager().loadPlayerProfile(playerName);
            if (CommandUtils.unloadedProfile(sender, stored)) {
                return true;
            }
            profile = stored;
        }

        if (skill != null && profile.getSpecialization(slot.other()) == skill) {
            sender.sendMessage(LocaleLoader.getString("mcRPG.SetSkill.SkillInOtherSlot",
                    skillName(skill), playerName, slotName(slot.other())));
            return true;
        }

        profile.setSpecialization(slot, skill);
        if (mmoPlayer == null) {
            profile.scheduleAsyncSave();
        }

        sender.sendMessage(LocaleLoader.getString("mcRPG.SetSkill.Success", playerName,
                slotName(slot), skill == null
                        ? LocaleLoader.getString("mcRPG.Specialization.None")
                        : skillName(skill)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        return switch (args.length) {
            case 1 -> ChooseSkillCommand.startingWith(args[0],
                    CommandUtils.getOnlinePlayerNames(sender));
            case 2 -> ChooseSkillCommand.startingWith(args[1],
                    Arrays.stream(SpecializationSlot.values())
                            .map(SpecializationSlot::commandName).toList());
            case 3 -> {
                final List<String> options = new ArrayList<>(
                        ChooseSkillCommand.availableSkillNames());
                options.add(NONE);
                yield ChooseSkillCommand.startingWith(args[2], options);
            }
            default -> List.of();
        };
    }
}
