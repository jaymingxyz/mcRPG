package com.gmail.nossr50.mcrpg.commands;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcrpg.gui.SkillSelectionMenu;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationActions;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /choosespecialization (alias /csp) opens the Specialization menu.
 * /choosespecialization &lt;primary|secondary&gt; &lt;category&gt; chooses a category directly;
 * typing the full command counts as the player's confirmation.
 */
public class ChooseSpecializationCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, String[] args) {
        if (CommandUtils.noConsoleUsage(sender)) {
            return true;
        }

        final Player player = (Player) sender;
        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) {
            player.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
            return true;
        }

        if (args.length == 0) {
            SkillSelectionMenu.open(player, mmoPlayer.getProfile());
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Choose.Usage"));
            return true;
        }

        final SpecializationSlot slot = SpecializationSlot.fromCommandName(args[0]);
        if (slot == null) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Choose.InvalidSlot", args[0]));
            return true;
        }

        final SkillCategory category = SkillCategory.fromCommandName(args[1]);
        if (category == null) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Choose.InvalidCategory", args[1],
                    categoryNamesText()));
            return true;
        }

        SpecializationActions.tryChoose(player, mmoPlayer.getProfile(), slot, category);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return startingWith(args[0], slotNames());
        }
        if (args.length == 2) {
            return startingWith(args[1], categoryNames());
        }
        return List.of();
    }

    static @NotNull List<String> slotNames() {
        return Arrays.stream(SpecializationSlot.values())
                .map(SpecializationSlot::commandName).toList();
    }

    static @NotNull List<String> categoryNames() {
        return Arrays.stream(SkillCategory.values()).map(SkillCategory::commandName).toList();
    }

    /** "melee, ranged, metallurgy, botany, blacksmithing, survivalism" */
    static @NotNull String categoryNamesText() {
        return String.join(", ", categoryNames());
    }

    static @NotNull List<String> startingWith(@NotNull String prefix,
            @NotNull List<String> options) {
        final String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(lowerPrefix)).toList();
    }
}
