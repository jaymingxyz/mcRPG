package com.gmail.nossr50.mcrpg.salvage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.skills.salvage.salvageables.SalvageableManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Which events record wear and found enchantments for Salvage XP. */
class SalvageTrackingListenerTest {
    private final SalvageTrackingListener listener = new SalvageTrackingListener();
    private MockedStatic<mcMMO> mockedMcMMO;
    private MockedStatic<SalvageTracking> tracking;

    @BeforeEach
    void setUp() {
        final SalvageableManager salvageables = mock(SalvageableManager.class);
        when(salvageables.isSalvageable(Material.IRON_PICKAXE)).thenReturn(true);
        when(salvageables.isSalvageable(Material.DIAMOND_CHESTPLATE)).thenReturn(true);
        mockedMcMMO = mockStatic(mcMMO.class);
        mockedMcMMO.when(mcMMO::getSalvageableManager).thenReturn(salvageables);

        // Keep the real salvageable check; watch what gets recorded
        tracking = mockStatic(SalvageTracking.class);
        tracking.when(() -> SalvageTracking.isSalvageable(any())).thenCallRealMethod();
    }

    @AfterEach
    void tearDown() {
        tracking.close();
        mockedMcMMO.close();
    }

    private static ItemStack item(Material material, boolean enchanted) {
        final ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        final Map<Enchantment, Integer> enchantments = enchanted
                ? SalvageXpTest.enchantmentLevels(2) : SalvageXpTest.enchantmentLevels();
        when(item.getEnchantments()).thenReturn(enchantments);
        return item;
    }

    @Test
    void damageToSalvageableGearShouldCountAsWear() {
        final ItemStack pickaxe = item(Material.IRON_PICKAXE, false);
        final ItemStack shears = item(Material.SHEARS, false);

        listener.onItemDamage(new PlayerItemDamageEvent(mock(Player.class), pickaxe, 3));
        listener.onItemDamage(new PlayerItemDamageEvent(mock(Player.class), shears, 3));

        tracking.verify(() -> SalvageTracking.addWear(pickaxe, 3));
        tracking.verify(() -> SalvageTracking.addWear(shears, 3), never());
    }

    @Test
    void enchantedMobDropsShouldBeMarkedFound() {
        final ItemStack enchanted = item(Material.DIAMOND_CHESTPLATE, true);
        final ItemStack plain = item(Material.IRON_PICKAXE, false);
        final List<ItemStack> drops = new ArrayList<>(List.of(enchanted, plain));

        listener.onEntityDeath(new EntityDeathEvent(mock(LivingEntity.class), drops));

        tracking.verify(() -> SalvageTracking.markFound(enchanted));
        tracking.verify(() -> SalvageTracking.markFound(plain), never());
    }

    @Test
    void aPlayersOwnDropsShouldNotBeMarkedFound() {
        final ItemStack enchanted = item(Material.DIAMOND_CHESTPLATE, true);
        final List<ItemStack> drops = new ArrayList<>(List.of(enchanted));

        listener.onEntityDeath(new EntityDeathEvent(mock(Player.class), drops));

        tracking.verify(() -> SalvageTracking.markFound(any()), never());
    }

    @Test
    void enchantedFishingCatchesShouldBeMarkedFound() {
        final ItemStack enchanted = item(Material.DIAMOND_CHESTPLATE, true);
        final Item caught = mock(Item.class);
        when(caught.getItemStack()).thenReturn(enchanted);
        final PlayerFishEvent event = mock(PlayerFishEvent.class);
        when(event.getState()).thenReturn(PlayerFishEvent.State.CAUGHT_FISH);
        when(event.getCaught()).thenReturn(caught);

        listener.onFish(event);

        tracking.verify(() -> SalvageTracking.markFound(enchanted));
    }

    @Test
    void nothingShouldBeRecordedWhileStillFishing() {
        final PlayerFishEvent event = mock(PlayerFishEvent.class);
        when(event.getState()).thenReturn(PlayerFishEvent.State.FISHING);

        listener.onFish(event);

        tracking.verify(() -> SalvageTracking.markFound(any()), never());
        tracking.verify(() -> SalvageTracking.addWear(any(), anyInt()), never());
    }
}
