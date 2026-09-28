package com.gmail.nossr50.mcrpg.passive;

import com.gmail.nossr50.config.WorldBlacklist;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import com.gmail.nossr50.events.fake.FakeEvent;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.sounds.SoundManager;
import com.gmail.nossr50.util.sounds.SoundType;
import com.gmail.nossr50.worldguard.WorldGuardManager;
import com.gmail.nossr50.worldguard.WorldGuardUtils;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies category passives: armor masteries cut damage and armor wear, and Wooden Mastery
 * speeds up mining and cuts tool wear. Also tells players when a passive unlocks.
 */
public class CategoryPassiveListener implements Listener {

    /**
     * Armor masteries. Runs after mcMMO's own HIGHEST damage handling (this listener is
     * registered later), so the cut also covers mcMMO's bonus damage. Only damage that armor
     * protects from is cut, which is the damage the armor modifier reduced.
     */
    @SuppressWarnings("deprecation") // DamageModifier is the only way to see armor's share
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamaged(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !event.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)
                || event.getDamage(EntityDamageEvent.DamageModifier.ARMOR) >= 0) {
            return;
        }
        final PlayerProfile profile = profileWherePassivesWork(player);
        if (profile == null) {
            return;
        }
        final double reduction = CategoryPassives.armorDamageReduction(profile,
                player.getInventory().getArmorContents());
        if (reduction > 0) {
            // Setting the base damage works out armor, enchantments and absorption again
            event.setDamage(event.getDamage() * (1D - reduction));
        }
    }

    /**
     * Less wear on mastered armor and wooden tools. Runs before Salvage's MONITOR tracking,
     * so wear that's ignored doesn't count toward Salvage XP either.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(@NotNull PlayerItemDamageEvent event) {
        final PlayerProfile profile = profileWherePassivesWork(event.getPlayer());
        if (profile == null) {
            return;
        }
        final int damage = event.getDamage();
        final int kept = CategoryPassives.durabilityDamage(profile, event.getItem(), damage,
                () -> ThreadLocalRandom.current().nextDouble());
        if (kept <= 0) {
            event.setCancelled(true);
        } else if (kept != damage) {
            event.setDamage(kept);
        }
    }

    /** Wooden Mastery's mining speed, checked each time the player starts breaking a block. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDamage(@NotNull BlockDamageEvent event) {
        if (event instanceof FakeEvent) {
            return;
        }
        updateMiningSpeed(event.getPlayer(), event.getItemInHand());
    }

    /** Updates the mining speed as soon as the player switches items. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(@NotNull PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        updateMiningSpeed(player, player.getInventory().getItem(event.getNewSlot()));
    }

    /** Removes a mining speed modifier left over from a crash. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        MiningSpeedBoost.remove(event.getPlayer());
    }

    /** Minecraft saves attribute modifiers with the player, so don't leave this one behind. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        MiningSpeedBoost.remove(event.getPlayer());
    }

    /** Tells the player when a level up brings their category to the unlock level. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLevelUp(@NotNull McMMOPlayerLevelUpEvent event) {
        final Player player = event.getPlayer();
        final CategoryPassive passive = CategoryPassive.of(SkillCategory.of(event.getSkill()));
        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        if (passive == null || mmoPlayer == null || !player.isOnline()) {
            return;
        }
        final PlayerProfile profile = mmoPlayer.getProfile();
        final int levelBefore = CategoryPassives.categoryLevel(profile, passive.category())
                - event.getLevelsGained();
        if (levelBefore < CategoryPassives.unlockLevel()
                && CategoryPassives.isActive(profile, passive)) {
            player.sendMessage(CategoryPassiveDisplay.unlockMessage(passive));
            SoundManager.sendSound(player, player.getLocation(), SoundType.SKILL_UNLOCKED);
        }
    }

    private static void updateMiningSpeed(@NotNull Player player, @Nullable ItemStack held) {
        final PlayerProfile profile = profileWherePassivesWork(player);
        MiningSpeedBoost.set(player,
                profile == null ? 0D : CategoryPassives.miningSpeedBonus(profile, held));
    }

    /**
     * The player's profile, or null where passives don't work: mcMMO is off in the world or
     * WorldGuard region, or the player's data isn't loaded (which includes NPCs).
     */
    private static @Nullable PlayerProfile profileWherePassivesWork(@NotNull Player player) {
        if (WorldBlacklist.isWorldBlacklisted(player.getWorld())) {
            return null;
        }
        if (WorldGuardUtils.isWorldGuardLoaded()
                && !WorldGuardManager.getInstance().hasMainFlag(player)) {
            return null;
        }
        final McMMOPlayer mmoPlayer = UserManager.getPlayer(player);
        return mmoPlayer == null ? null : mmoPlayer.getProfile();
    }
}
