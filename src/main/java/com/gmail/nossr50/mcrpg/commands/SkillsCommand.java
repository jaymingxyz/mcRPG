package com.gmail.nossr50.mcrpg.commands;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.gui.SkillInfoMenu;
import com.gmail.nossr50.mcrpg.gui.SkillsMenu;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * /skills (alias /rpgskills) opens the skills menu: every skill by category, and a page for
 * each one explaining how it works. /skills &lt;skill&gt; opens that skill's page directly.
 */
public class SkillsCommand implements TabExecutor {
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
            SkillsMenu.open(player, mmoPlayer);
            return true;
        }
        if (args.length != 1) {
            return false; // Bukkit sends the usage
        }

        final PrimarySkillType skill = mcMMO.p.getSkillTools().matchSkill(args[0]);
        if (skill == null || !SkillCategory.isAvailable(skill)) {
            player.sendMessage(LocaleLoader.getString("Commands.Skill.Invalid"));
            return true;
        }
        SkillInfoMenu.open(player, mmoPlayer.getProfile(), skill);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0],
                    mcMMO.p.getSkillTools().LOCALIZED_SKILL_NAMES, new ArrayList<>());
        }
        return List.of();
    }
}
