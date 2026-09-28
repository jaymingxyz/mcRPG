package com.gmail.nossr50.mcrpg.commands;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.categoryName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelDownEvent;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.passive.CategoryPassiveDisplay;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.Specialization;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.commands.CommandUtils;
import com.gmail.nossr50.util.player.UserManager;
import java.util.ArrayList;
import java.util.List;
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * /abandonspecialization &lt;category|primary|secondary&gt; [confirm] (alias /asp) clears one
 * of the player's Specializations. Every skill in the category keeps only part of its total XP
 * (10% by default), so the first run only shows a warning, and the player must repeat the
 * command with "confirm" before it expires.
 */
public class AbandonSpecializationCommand implements TabExecutor {
    private static final String CONFIRM = "confirm";

    /** A warning shown to a player, waiting for them to confirm. */
    record PendingAbandon(@NotNull SkillCategory category, long expiresAtMillis) {
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

        final PlayerProfile profile = mmoPlayer.getProfile();
        final SkillCategory category = resolveCategory(player, profile, args[0]);
        if (category == null) {
            return true;
        }

        final Specialization.AbandonPreview preview =
                Specialization.previewAbandon(profile, category);
        if (preview == null) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.NotSelected",
                    categoryName(category)));
            return true;
        }

        final UUID playerId = player.getUniqueId();
        final PendingAbandon pending = PENDING.get(playerId);
        final long now = clock.getAsLong();
        if (!confirming || pending == null || pending.category() != category
                || now > pending.expiresAtMillis()) {
            warn(player, profile, preview, now);
            return true;
        }

        PENDING.remove(playerId);
        abandon(player, profile, preview);
        return true;
    }

    /**
     * The category the player named, or the one chosen as their primary or secondary
     * Specialization. Tells the player and returns null if there isn't one.
     */
    private static @Nullable SkillCategory resolveCategory(@NotNull Player player,
            @NotNull PlayerProfile profile, @NotNull String argument) {
        final SpecializationSlot slot = SpecializationSlot.fromCommandName(argument);
        if (slot != null) {
            final SkillCategory chosen = profile.getSpecialization(slot);
            if (chosen == null) {
                player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.NoneChosen",
                        slotName(slot)));
            }
            return chosen;
        }

        final SkillCategory category = SkillCategory.fromCommandName(argument);
        if (category == null) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Choose.InvalidCategory", argument,
                    ChooseSpecializationCommand.categoryNamesText()));
        }
        return category;
    }

    private void warn(@NotNull Player player, @NotNull PlayerProfile profile,
            @NotNull Specialization.AbandonPreview preview, long now) {
        final int timeoutSeconds = mcMMO.p.getGeneralConfig().getAbandonConfirmTimeoutSeconds();
        PENDING.put(player.getUniqueId(),
                new PendingAbandon(preview.category(), now + timeoutSeconds * 1000L));

        player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.Warning",
                categoryName(preview.category()), keptPercentText()));
        for (Specialization.SkillAbandon each : preview.skills()) {
            player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.WarningSkill",
                    skillName(each.skill()), each.oldLevel(), each.newLevel()));
        }
        final String passive = CategoryPassiveDisplay.abandonWarning(profile, preview.category());
        if (passive != null) {
            player.sendMessage(passive);
        }
        player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.ConfirmPrompt",
                preview.category().commandName(), timeoutSeconds));
    }

    private void abandon(@NotNull Player player, @NotNull PlayerProfile profile,
            @NotNull Specialization.AbandonPreview preview) {
        // Let other plugins veto any skill's level loss before anything changes
        for (Specialization.SkillAbandon each : preview.skills()) {
            if (each.levelsLost() <= 0) {
                continue;
            }
            final McMMOPlayerLevelDownEvent event = new McMMOPlayerLevelDownEvent(player,
                    each.skill(), each.levelsLost(), XPGainReason.COMMAND);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.Cancelled",
                        categoryName(preview.category())));
                return;
            }
        }

        Specialization.applyAbandon(profile, preview);
        player.sendMessage(LocaleLoader.getString("mcRPG.Abandon.Success",
                categoryName(preview.category()), slotName(preview.slot()), keptPercentText()));
    }

    private static @NotNull String keptPercentText() {
        return SpecializationDisplay.formatNumber(
                mcMMO.p.getGeneralConfig().getAbandonXpKeptPercent());
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
                final SkillCategory category = mmoPlayer.getProfile().getSpecialization(slot);
                if (category != null) {
                    chosen.add(category.commandName());
                }
            }
            return ChooseSpecializationCommand.startingWith(args[0], chosen);
        }
        if (args.length == 2) {
            return ChooseSpecializationCommand.startingWith(args[1], List.of(CONFIRM));
        }
        return List.of();
    }

    @VisibleForTesting
    static void clearPendingConfirmations() {
        PENDING.clear();
    }
}
