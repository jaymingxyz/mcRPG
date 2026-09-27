package com.gmail.nossr50.mcrpg.specialization;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.events.players.McMMOPlayerProfileLoadEvent;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.UserManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reminds players who haven't chosen both Specializations when they log in, with a clickable
 * link that opens the Specialization menu. Turned off with Specialization.Login_Reminder.
 */
public class LoginReminder implements Listener {
    /**
     * mcMMO fires the profile load event before it finishes setting the player up, so the
     * reminder waits this long for /choosespecialization to work.
     */
    static final long DELAY_TICKS = 40L;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProfileLoad(@NotNull McMMOPlayerProfileLoadEvent event) {
        if (!mcMMO.p.getGeneralConfig().getSpecializationLoginReminder()) {
            return;
        }
        final Player player = event.getPlayer();
        mcMMO.p.getFoliaLib().getScheduler().runAtEntityLater(player, () -> remind(player),
                DELAY_TICKS);
    }

    /** Sends the reminder if the player is online, loaded, and missing a Specialization. */
    static void remind(@NotNull Player player) {
        if (!player.isOnline()) {
            return;
        }
        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) {
            return;
        }
        final String text = reminderText(mmoPlayer.getProfile());
        if (text == null) {
            return;
        }

        final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
        final Component button = legacy.deserialize(
                        LocaleLoader.getString("mcRPG.Specialization.Reminder.Button"))
                .clickEvent(ClickEvent.runCommand("/choosespecialization"))
                .hoverEvent(HoverEvent.showText(Component.text(
                        LocaleLoader.getString("mcRPG.Specialization.Reminder.Hover"))));
        mcMMO.getAudiences().player(player).sendMessage(
                legacy.deserialize(text).append(Component.space()).append(button));
    }

    /** The reminder text for this profile, or null if both Specializations are chosen. */
    static @Nullable String reminderText(@NotNull PlayerProfile profile) {
        final boolean primaryEmpty = profile.getSpecialization(SpecializationSlot.PRIMARY) == null;
        final boolean secondaryEmpty =
                profile.getSpecialization(SpecializationSlot.SECONDARY) == null;
        if (primaryEmpty && secondaryEmpty) {
            return LocaleLoader.getString("mcRPG.Specialization.Reminder.Both");
        }
        if (primaryEmpty || secondaryEmpty) {
            return LocaleLoader.getString("mcRPG.Specialization.Reminder.One",
                    SpecializationDisplay.slotName(primaryEmpty
                            ? SpecializationSlot.PRIMARY : SpecializationSlot.SECONDARY));
        }
        return null;
    }
}
