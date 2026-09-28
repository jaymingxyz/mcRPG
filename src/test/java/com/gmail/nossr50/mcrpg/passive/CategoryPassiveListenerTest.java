package com.gmail.nossr50.mcrpg.passive;

import static com.gmail.nossr50.mcrpg.passive.CategoryPassivesTest.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** How the listener applies category passives to damage, wear and mining speed. */
@SuppressWarnings("deprecation") // EntityDamageEvent.DamageModifier
class CategoryPassiveListenerTest extends MMOTestEnvironment {
    private static final Logger logger =
            Logger.getLogger(CategoryPassiveListenerTest.class.getName());

    private final CategoryPassiveListener listener = new CategoryPassiveListener();
    private PlayerProfile profile;
    private Attribute originalSpeedAttribute;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        profile = mmoPlayer.getProfile();
        originalSpeedAttribute = MiningSpeedBoost.speedAttribute;
    }

    @AfterEach
    void tearDown() {
        MiningSpeedBoost.remove(player);
        MiningSpeedBoost.speedAttribute = originalSpeedAttribute;
        cleanUpStaticMocks();
    }

    private void unlockLeatherMastery() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.SURVIVALISM);
        profile.modifySkill(PrimarySkillType.ACROBATICS, 25);
    }

    private void wearLeatherSet() {
        // Built before stubbing: Mockito can't create mocks inside thenReturn
        final ItemStack[] armor = {item(Material.LEATHER_BOOTS),
                item(Material.LEATHER_LEGGINGS), item(Material.LEATHER_CHESTPLATE),
                item(Material.LEATHER_HELMET)};
        when(playerInventory.getArmorContents()).thenReturn(armor);
    }

    private EntityDamageEvent damage(double base, double armorModifier) {
        final EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getDamage()).thenReturn(base);
        when(event.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)).thenReturn(true);
        when(event.getDamage(EntityDamageEvent.DamageModifier.ARMOR)).thenReturn(armorModifier);
        return event;
    }

    @Test
    void masteredArmorShouldCutDamageThatArmorBlocks() {
        unlockLeatherMastery();
        wearLeatherSet();
        final EntityDamageEvent event = damage(10, -2.2);

        listener.onPlayerDamaged(event);

        // 8% for each of 4 leather pieces
        verify(event).setDamage(eq(6.8, 1e-9));
    }

    @Test
    void damageThatIgnoresArmorShouldNotBeCut() {
        unlockLeatherMastery();
        wearLeatherSet();
        final EntityDamageEvent event = damage(10, 0);

        listener.onPlayerDamaged(event);

        verify(event, never()).setDamage(anyDouble());
    }

    @Test
    void armorShouldNotCutDamageWhileThePassiveIsLocked() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.SURVIVALISM);
        profile.modifySkill(PrimarySkillType.ACROBATICS, 24);
        wearLeatherSet();
        final EntityDamageEvent event = damage(10, -2.2);

        listener.onPlayerDamaged(event);

        verify(event, never()).setDamage(anyDouble());
    }

    @Test
    void wearIgnoredEntirelyShouldCancelTheDurabilityLoss() {
        unlockLeatherMastery();
        when(generalConfig.getCategoryPassiveDurabilityLossReduction(
                CategoryPassive.LEATHER_MASTERY)).thenReturn(100D);
        final PlayerItemDamageEvent leather =
                new PlayerItemDamageEvent(player, item(Material.LEATHER_HELMET), 3);
        final PlayerItemDamageEvent iron =
                new PlayerItemDamageEvent(player, item(Material.IRON_HELMET), 3);

        listener.onItemDamage(leather);
        listener.onItemDamage(iron);

        assertThat(leather.isCancelled()).isTrue();
        assertThat(iron.isCancelled()).isFalse();
        assertThat(iron.getDamage()).isEqualTo(3);
    }

    @Test
    void woodenToolsShouldGetTheMiningSpeedModifierWhileHeld() {
        MiningSpeedBoost.speedAttribute = Attribute.PLAYER_BLOCK_BREAK_SPEED;
        final AttributeInstance speed = mock(AttributeInstance.class);
        when(player.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED)).thenReturn(speed);
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.BOTANY);
        profile.modifySkill(PrimarySkillType.WOODCUTTING, 25);

        listener.onBlockDamage(blockDamage(Material.WOODEN_AXE));

        final ArgumentCaptor<AttributeModifier> added =
                ArgumentCaptor.forClass(AttributeModifier.class);
        verify(speed).addModifier(added.capture());
        assertThat(added.getValue().getUniqueId()).isEqualTo(MiningSpeedBoost.MODIFIER_ID);
        assertThat(added.getValue().getAmount()).isEqualTo(0.5);
        assertThat(added.getValue().getOperation())
                .isEqualTo(AttributeModifier.Operation.ADD_SCALAR);
        assertThat(MiningSpeedBoost.applied(player)).isEqualTo(0.5);

        listener.onBlockDamage(blockDamage(Material.STONE_AXE));

        assertThat(MiningSpeedBoost.applied(player)).isZero();
        verify(speed, times(2)).removeModifier(any());
    }

    @Test
    void levelUpReachingTheUnlockLevelShouldAnnounceThePassive() {
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.SURVIVALISM);
        profile.modifySkill(PrimarySkillType.FISHING, 26);

        // 26 now and 1 level gained: it was already unlocked
        listener.onLevelUp(new McMMOPlayerLevelUpEvent(player, PrimarySkillType.FISHING, 1,
                XPGainReason.PVE));
        verify(player, never()).sendMessage(anyString());

        // 26 now and 2 levels gained: it was 24
        listener.onLevelUp(new McMMOPlayerLevelUpEvent(player, PrimarySkillType.FISHING, 2,
                XPGainReason.PVE));
        final ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(player).sendMessage(message.capture());
        assertThat(ChatColor.stripColor(message.getValue()))
                .startsWith("Leather Mastery unlocked!");
    }

    private BlockDamageEvent blockDamage(Material held) {
        final ItemStack item = item(held);
        final BlockDamageEvent event = mock(BlockDamageEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItemInHand()).thenReturn(item);
        return event;
    }
}
