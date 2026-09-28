package com.gmail.nossr50.mcrpg.passive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.passive.CategoryPassives.Status;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import java.util.Iterator;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Category passives: which category unlocks which, when they're active, and how much they
 * change damage, mining speed and durability loss.
 */
class CategoryPassivesTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(CategoryPassivesTest.class.getName());

    private PlayerProfile profile;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        profile = mmoPlayer.getProfile();
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    static ItemStack item(Material material) {
        final ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        return item;
    }

    /** Survivalism as the Secondary with Taming 10 and Fishing 15: 25 combined levels. */
    private void unlockLeatherMastery() {
        profile.setSpecialization(SpecializationSlot.SECONDARY, SkillCategory.SURVIVALISM);
        profile.modifySkill(PrimarySkillType.TAMING, 10);
        profile.modifySkill(PrimarySkillType.FISHING, 15);
    }

    @Test
    void eachCategoryShouldUnlockTheRequestedPassive() {
        assertThat(CategoryPassive.of(SkillCategory.SURVIVALISM))
                .isEqualTo(CategoryPassive.LEATHER_MASTERY);
        assertThat(CategoryPassive.of(SkillCategory.BLACKSMITHING))
                .isEqualTo(CategoryPassive.CHAINMAIL_MASTERY);
        assertThat(CategoryPassive.of(SkillCategory.METALLURGY))
                .isEqualTo(CategoryPassive.COPPER_MASTERY);
        assertThat(CategoryPassive.of(SkillCategory.BOTANY))
                .isEqualTo(CategoryPassive.WOODEN_MASTERY);
        assertThat(CategoryPassive.of(SkillCategory.MELEE)).isNull();
        assertThat(CategoryPassive.of(SkillCategory.RANGED)).isNull();
    }

    @Test
    void eachPassiveShouldOnlyImproveItsOwnGear() {
        assertThat(CategoryPassive.LEATHER_MASTERY.appliesTo("leather_chestplate")).isTrue();
        assertThat(CategoryPassive.LEATHER_MASTERY.appliesTo("iron_chestplate")).isFalse();
        assertThat(CategoryPassive.CHAINMAIL_MASTERY.appliesTo("chainmail_boots")).isTrue();
        assertThat(CategoryPassive.CHAINMAIL_MASTERY.appliesTo("leather_boots")).isFalse();
        // Copper armor only exists on 1.21.9+, so it's matched by ID
        assertThat(CategoryPassive.COPPER_MASTERY.appliesTo("copper_helmet")).isTrue();
        assertThat(CategoryPassive.COPPER_MASTERY.appliesTo("copper_pickaxe")).isFalse();
        for (String tool : List.of("wooden_pickaxe", "wooden_axe", "wooden_shovel",
                "wooden_hoe", "wooden_sword", "wooden_spear")) {
            assertThat(CategoryPassive.WOODEN_MASTERY.appliesTo(tool)).as(tool).isTrue();
        }
        assertThat(CategoryPassive.WOODEN_MASTERY.appliesTo("stone_pickaxe")).isFalse();

        assertThat(CategoryPassive.forItem(item(Material.LEATHER_BOOTS)))
                .isEqualTo(CategoryPassive.LEATHER_MASTERY);
        assertThat(CategoryPassive.forItem(item(Material.WOODEN_AXE)))
                .isEqualTo(CategoryPassive.WOODEN_MASTERY);
        assertThat(CategoryPassive.forItem(item(Material.IRON_HELMET))).isNull();
        assertThat(CategoryPassive.forItem(null)).isNull();
    }

    @Test
    void passiveShouldNeedTheSpecializationAnd25CombinedLevels() {
        // Plenty of levels, but Survivalism isn't a Specialization
        profile.modifySkill(PrimarySkillType.UNARMED, 40);
        assertThat(CategoryPassives.status(profile, CategoryPassive.LEATHER_MASTERY))
                .isEqualTo(Status.NOT_SPECIALIZED);

        profile.modifySkill(PrimarySkillType.UNARMED, 0);
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.SURVIVALISM);
        profile.modifySkill(PrimarySkillType.TAMING, 10);
        profile.modifySkill(PrimarySkillType.ACROBATICS, 8);
        profile.modifySkill(PrimarySkillType.FISHING, 6);
        assertThat(CategoryPassives.categoryLevel(profile, SkillCategory.SURVIVALISM))
                .isEqualTo(24);
        assertThat(CategoryPassives.status(profile, CategoryPassive.LEATHER_MASTERY))
                .isEqualTo(Status.NEEDS_LEVELS);

        profile.modifySkill(PrimarySkillType.UNARMED, 1);
        assertThat(CategoryPassives.isActive(profile, CategoryPassive.LEATHER_MASTERY))
                .isTrue();
        // Other categories' passives stay locked
        assertThat(CategoryPassives.status(profile, CategoryPassive.WOODEN_MASTERY))
                .isEqualTo(Status.NOT_SPECIALIZED);
    }

    @Test
    void secondarySpecializationShouldAlsoUnlockItsPassive() {
        unlockLeatherMastery();

        assertThat(CategoryPassives.isActive(profile, CategoryPassive.LEATHER_MASTERY))
                .isTrue();
    }

    @Test
    void passivesShouldDoNothingWhenTurnedOff() {
        unlockLeatherMastery();
        when(generalConfig.getCategoryPassivesEnabled()).thenReturn(false);

        assertThat(CategoryPassives.status(profile, CategoryPassive.LEATHER_MASTERY))
                .isEqualTo(Status.DISABLED);
    }

    @Test
    void unlockLevelShouldComeFromTheConfig() {
        unlockLeatherMastery();
        when(generalConfig.getCategoryPassiveUnlockLevel()).thenReturn(30);

        assertThat(CategoryPassives.status(profile, CategoryPassive.LEATHER_MASTERY))
                .isEqualTo(Status.NEEDS_LEVELS);
    }

    @Test
    void eachPieceOfMasteredArmorShouldGiveAQuarterOfTheFullSetBonus() {
        final ItemStack[] leatherSet = {item(Material.LEATHER_BOOTS),
                item(Material.LEATHER_LEGGINGS), item(Material.LEATHER_CHESTPLATE),
                item(Material.LEATHER_HELMET)};
        assertThat(CategoryPassives.armorBonus(profile, leatherSet).isNone()).isTrue();

        unlockLeatherMastery();
        assertThat(CategoryPassives.armorBonus(profile, leatherSet))
                .isEqualTo(new CategoryPassives.ArmorBonus(9, 2));

        // Empty slots and other armor don't count
        final ItemStack[] mixed = {item(Material.LEATHER_BOOTS), null,
                item(Material.CHAINMAIL_CHESTPLATE), item(Material.IRON_HELMET)};
        assertThat(CategoryPassives.armorBonus(profile, mixed))
                .isEqualTo(new CategoryPassives.ArmorBonus(2.25, 0.5));

        // Each piece counts toward its own mastery: chainmail adds 8 / 4 and 9 / 4
        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.BLACKSMITHING);
        profile.modifySkill(PrimarySkillType.REPAIR, 25);
        assertThat(CategoryPassives.armorBonus(profile, mixed))
                .isEqualTo(new CategoryPassives.ArmorBonus(4.25, 2.75));
    }

    @Test
    void woodenToolsShouldMineFasterOnlyWithWoodenMastery() {
        final ItemStack woodenPickaxe = item(Material.WOODEN_PICKAXE);
        assertThat(CategoryPassives.miningSpeedBonus(profile, woodenPickaxe)).isZero();

        profile.setSpecialization(SpecializationSlot.PRIMARY, SkillCategory.BOTANY);
        profile.modifySkill(PrimarySkillType.HERBALISM, 25);
        assertThat(CategoryPassives.miningSpeedBonus(profile, woodenPickaxe)).isEqualTo(0.5);
        assertThat(CategoryPassives.miningSpeedBonus(profile, item(Material.STONE_PICKAXE)))
                .isZero();
        assertThat(CategoryPassives.miningSpeedBonus(profile, null)).isZero();
    }

    @Test
    void eachPointOfWearShouldBeIgnoredWithTheConfiguredChance() {
        // Rolls under 0.5 are ignored at 50%
        assertThat(CategoryPassives.keptDamage(4, 50, rolls(0.1, 0.6, 0.49, 0.5)))
                .isEqualTo(2);
        assertThat(CategoryPassives.keptDamage(3, 0, rolls(0.0, 0.0, 0.0))).isEqualTo(3);
        assertThat(CategoryPassives.keptDamage(3, 100, rolls(0.99, 0.99, 0.99))).isZero();
    }

    @Test
    void onlyMasteredGearShouldLoseLessDurability() {
        final ItemStack boots = item(Material.LEATHER_BOOTS);
        final ItemStack ironBoots = item(Material.IRON_BOOTS);
        assertThat(CategoryPassives.durabilityDamage(profile, boots, 2, () -> 0.0))
                .isEqualTo(2);

        unlockLeatherMastery();
        assertThat(CategoryPassives.durabilityDamage(profile, boots, 2, () -> 0.0)).isZero();
        assertThat(CategoryPassives.durabilityDamage(profile, boots, 2, () -> 0.9))
                .isEqualTo(2);
        assertThat(CategoryPassives.durabilityDamage(profile, ironBoots, 2, () -> 0.0))
                .isEqualTo(2);
    }

    private static DoubleSupplier rolls(Double... values) {
        final Iterator<Double> each = List.of(values).iterator();
        return each::next;
    }
}
