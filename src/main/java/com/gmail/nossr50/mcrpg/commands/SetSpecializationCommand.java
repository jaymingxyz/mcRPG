package com.gmail.nossr50.mcrpg.commands;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.categoryName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * /rpgsetspecialization &lt;player&gt; &lt;primary|secondary&gt; &lt;category|none&gt; lets
 * admins set or clear a player's Specialization without changing any skill progress, for
 * example to fix a mistake. Works on online and offline players, like mcMMO's other admin
 * commands.
 */
public class SetSpecializationCommand implements TabExecutor {
    private static final String NONE = "none";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(LocaleLoader.getString("mcRPG.SetSpecialization.Usage"));
            return true;
        }

        final SpecializationSlot slot = SpecializationSlot.fromCommandName(args[1]);
        if (slot == null) {
            sender.sendMessage(LocaleLoader.getString("mcRPG.Choose.InvalidSlot", args[1]));
            return true;
        }

        final @Nullable SkillCategory category;
        if (args[2].equalsIgnoreCase(NONE)) {
            category = null;
        } else {
            category = SkillCategory.fromCommandName(args[2]);
            if (category == null) {
                sender.sendMessage(LocaleLoader.getString("mcRPG.Choose.InvalidCategory",
                        args[2], ChooseSpecializationCommand.categoryNamesText()));
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

        if (category != null && profile.getSpecialization(slot.other()) == category) {
            sender.sendMessage(LocaleLoader.getString(
                    "mcRPG.SetSpecialization.CategoryInOtherSlot", categoryName(category),
                    playerName, slotName(slot.other())));
            return true;
        }

        profile.setSpecialization(slot, category);
        if (mmoPlayer == null) {
            profile.scheduleAsyncSave();
        }

        sender.sendMessage(LocaleLoader.getString("mcRPG.SetSpecialization.Success", playerName,
                slotName(slot), category == null
                        ? LocaleLoader.getString("mcRPG.Specialization.None")
                        : categoryName(category)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        return switch (args.length) {
            case 1 -> ChooseSpecializationCommand.startingWith(args[0],
                    CommandUtils.getOnlinePlayerNames(sender));
            case 2 -> ChooseSpecializationCommand.startingWith(args[1],
                    ChooseSpecializationCommand.slotNames());
            case 3 -> {
                final List<String> options =
                        new ArrayList<>(ChooseSpecializationCommand.categoryNames());
                options.add(NONE);
                yield ChooseSpecializationCommand.startingWith(args[2], options);
            }
            default -> List.of();
        };
    }
}
