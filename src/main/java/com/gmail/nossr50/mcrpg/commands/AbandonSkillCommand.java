package com.gmail.nossr50.mcrpg.commands;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelDownEvent;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.Specialization;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * /abandonskill &lt;skill&gt; [confirm] removes a skill from the player's Primary or Secondary
 * slot. The skill keeps only part of its total XP (10% by default), so the first run only
 * shows a warning, and the player must repeat the command with "confirm" before it expires.
 */
public class AbandonSkillCommand implements TabExecutor {
    private static final String CONFIRM = "confirm";

    /** A warning shown to a player, waiting for them to confirm. */
    record PendingAbandon(@NotNull PrimarySkillType skill, long expiresAtMillis) {
    }

    private static final Map<UUID, PendingAbandon> PENDING = new ConcurrentHashMap<>();

    @VisibleForTesting
    static LongSupplier clock = System::currentTimeMillis;

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

        final boolean confirming = args.length == 2 && args[1].equalsIgnoreCase(CONFIRM);
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !confirming)) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.Usage"));
            return true;
        }

        if (CommandUtils.isInvalidSkill(player, args[0])) {
            return true;
        }
        final PrimarySkillType skill = mcMMO.p.getSkillTools().matchSkill(args[0]);
        final PlayerProfile profile = mmoPlayer.getProfile();

        final Specialization.AbandonPreview preview =
                Specialization.previewAbandon(profile, skill);
        if (preview == null) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.NotSelected",
                    skillName(skill)));
            return true;
        }

        final UUID playerId = player.getUniqueId();
        final PendingAbandon pending = PENDING.get(playerId);
        final long now = clock.getAsLong();
        if (!confirming || pending == null || pending.skill() != skill
                || now > pending.expiresAtMillis()) {
            warn(player, preview, now);
            return true;
        }

        PENDING.remove(playerId);
        abandon(player, profile, preview);
        return true;
    }

    private void warn(@NotNull Player player, @NotNull Specialization.AbandonPreview preview,
            long now) {
        final int timeoutSeconds = mcMMO.p.getGeneralConfig().getAbandonConfirmTimeoutSeconds();
        PENDING.put(player.getUniqueId(),
                new PendingAbandon(preview.skill(), now + timeoutSeconds * 1000L));

        player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.Warning",
                skillName(preview.skill()), preview.oldLevel(), preview.newLevel(),
                SpecializationDisplay.formatNumber(
                        mcMMO.p.getGeneralConfig().getAbandonXpKeptPercent())));
        player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.ConfirmPrompt",
                preview.skill().name().toLowerCase(Locale.ROOT), timeoutSeconds));
    }

    private void abandon(@NotNull Player player, @NotNull PlayerProfile profile,
            @NotNull Specialization.AbandonPreview preview) {
        // Let other plugins veto the level loss before anything changes
        if (preview.levelsLost() > 0) {
            final McMMOPlayerLevelDownEvent event = new McMMOPlayerLevelDownEvent(player,
                    preview.skill(), preview.levelsLost(), XPGainReason.COMMAND);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.Cancelled",
                        skillName(preview.skill())));
                return;
            }
        }

        Specialization.applyAbandon(profile, preview);
        player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.Success",
                skillName(preview.skill()), slotName(preview.slot()), preview.newLevel()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
            if (mmoPlayer == null) {
                return List.of();
            }
            final List<String> chosen = new ArrayList<>();
            for (SpecializationSlot slot : SpecializationSlot.values()) {
                final PrimarySkillType skill = mmoPlayer.getProfile().getSpecialization(slot);
                if (skill != null) {
                    chosen.add(skill.name().toLowerCase(Locale.ROOT));
                }
            }
            return ChooseSkillCommand.startingWith(args[0], chosen);
        }
        if (args.length == 2) {
            return ChooseSkillCommand.startingWith(args[1], List.of(CONFIRM));
        }
        return List.of();
    }

    @VisibleForTesting
    static void clearPendingConfirmations() {
        PENDING.clear();
    }
}
