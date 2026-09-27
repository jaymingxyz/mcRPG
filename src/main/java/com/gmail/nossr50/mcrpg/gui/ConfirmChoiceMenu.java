package com.gmail.nossr50.mcrpg.gui;

import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.skillName;
import static com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay.slotName;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SpecializationActions;
import com.gmail.nossr50.mcrpg.specialization.SpecializationDisplay;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.player.UserManager;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Asks the player to confirm a choice made in the Specialization menu, since a slot can only
 * be emptied again by abandoning the skill.
 */
public final class ConfirmChoiceMenu implements InventoryHolder {
    static final int SIZE = 27;
    static final int CONFIRM = 11;
    static final int SKILL = 13;
    static final int CANCEL = 15;

    private final Inventory inventory;
    private final PrimarySkillType skill;
    private final SpecializationSlot slot;

    private ConfirmChoiceMenu(@NotNull PrimarySkillType skill, @NotNull SpecializationSlot slot) {
        this.skill = skill;
        this.slot = slot;
        this.inventory = Bukkit.createInventory(this, SIZE,
                LocaleLoader.getString("mcRPG.Menu.Confirm.Title"));

        for (int each = 0; each < SIZE; each++) {
            inventory.setItem(each, MenuItems.filler());
        }
        final String kept = SpecializationDisplay.formatNumber(
                mcMMO.p.getGeneralConfig().getAbandonXpKeptPercent());
        inventory.setItem(SKILL, MenuItems.item(GuiConfig.getInstance().getIcon(skill),
                LocaleLoader.getString("mcRPG.Menu.Confirm.Question", skillName(skill),
                        slotName(slot)),
                List.of(LocaleLoader.getString("mcRPG.Menu.Confirm.Warning1"),
                        LocaleLoader.getString("mcRPG.Menu.Confirm.Warning2", kept)),
                true));
        inventory.setItem(CONFIRM, MenuItems.item(Material.LIME_CONCRETE,
                LocaleLoader.getString("mcRPG.Menu.Confirm.Yes")));
        inventory.setItem(CANCEL, MenuItems.item(Material.RED_CONCRETE,
                LocaleLoader.getString("mcRPG.Menu.Confirm.No")));
    }

    /** Opens the confirmation on the player's own thread (required on Folia). */
    static void open(@NotNull Player player, @NotNull PrimarySkillType skill,
            @NotNull SpecializationSlot slot) {
        mcMMO.p.getFoliaLib().getScheduler().runAtEntity(player, task -> {
            if (player.isOnline()) {
                player.openInventory(new ConfirmChoiceMenu(skill, slot).getInventory());
            }
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /** Handles a click anywhere while this menu is open. Every click is cancelled. */
    void handleClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final int rawSlot = event.getRawSlot();
        if (rawSlot != CONFIRM && rawSlot != CANCEL) {
            return;
        }

        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (mmoPlayer == null) {
            player.closeInventory();
            player.sendMessage(LocaleLoader.getString("Profile.PendingLoad"));
            return;
        }

        if (rawSlot == CONFIRM) {
            // Checks the rules again, since the player could have chosen in the meantime
            SpecializationActions.tryChoose(player, mmoPlayer.getProfile(), slot, skill);
        }
        SkillSelectionMenu.open(player, mmoPlayer.getProfile());
    }
}
