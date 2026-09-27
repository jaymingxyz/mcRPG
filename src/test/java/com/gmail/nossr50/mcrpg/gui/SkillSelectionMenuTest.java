package com.gmail.nossr50.mcrpg.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import com.gmail.nossr50.util.Permissions;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** The Specialization menu's layout and click handling, and its confirmation screen. */
class SkillSelectionMenuTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(SkillSelectionMenuTest.class.getName());

    private PlayerProfile profile;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        when(Permissions.skillEnabled(any(), any(PrimarySkillType.class))).thenReturn(true);
        GuiTestSupport.prepareMenus();
        profile = mmoPlayer.getProfile();
    }

    @AfterEach
    void tearDown() {
        GuiTestSupport.reset();
        cleanUpStaticMocks();
    }

    // --- layout ---

    @Test
    void layoutShouldPutEachCategoryOnItsOwnRowAfterItsLabel() {
        final Map<Integer, PrimarySkillType> layout = SkillSelectionMenu.layout();

        // Row 1 Melee, 2 Ranged, 3 Metallurgy, 4 Botany, 5 Blacksmithing, 6 Survivalism
        assertThat(List.of(layout.get(1), layout.get(2), layout.get(3), layout.get(4)))
                .containsExactly(PrimarySkillType.SWORDS, PrimarySkillType.AXES,
                        PrimarySkillType.MACES, PrimarySkillType.SPEARS);
        assertThat(List.of(layout.get(10), layout.get(11), layout.get(12)))
                .containsExactly(PrimarySkillType.ARCHERY, PrimarySkillType.CROSSBOWS,
                        PrimarySkillType.TRIDENTS);
        assertThat(List.of(layout.get(19), layout.get(20), layout.get(21)))
                .containsExactly(PrimarySkillType.MINING, PrimarySkillType.SMELTING,
                        PrimarySkillType.EXCAVATION);
        assertThat(List.of(layout.get(28), layout.get(29), layout.get(30)))
                .containsExactly(PrimarySkillType.WOODCUTTING, PrimarySkillType.HERBALISM,
                        PrimarySkillType.ALCHEMY);
        assertThat(List.of(layout.get(37), layout.get(38)))
                .containsExactly(PrimarySkillType.REPAIR, PrimarySkillType.SALVAGE);
        assertThat(List.of(layout.get(46), layout.get(47), layout.get(48), layout.get(49)))
                .containsExactly(PrimarySkillType.TAMING, PrimarySkillType.ACROBATICS,
                        PrimarySkillType.FISHING, PrimarySkillType.UNARMED);
        assertThat(layout).hasSize(19);
    }

    @Test
    void eachRowShouldStartWithItsCategoryLabel() {
        assertThat(SkillSelectionMenu.categoryLabels()).containsExactly(
                Map.entry(0, SkillCategory.MELEE), Map.entry(9, SkillCategory.RANGED),
                Map.entry(18, SkillCategory.METALLURGY), Map.entry(27, SkillCategory.BOTANY),
                Map.entry(36, SkillCategory.BLACKSMITHING),
                Map.entry(45, SkillCategory.SURVIVALISM));
    }

    @Test
    void skillsLabelsAndButtonsShouldNeverShareASlot() {
        final Set<Integer> used = new HashSet<>();
        for (int slot : SkillSelectionMenu.layout().keySet()) {
            assertThat(slot % 9).as("skill column").isBetween(1, 4);
            assertThat(used.add(slot)).isTrue();
        }
        for (int slot : SkillSelectionMenu.categoryLabels().keySet()) {
            assertThat(used.add(slot)).as("label slot %d", slot).isTrue();
        }
        for (int slot : List.of(SkillSelectionMenu.PRIMARY_INDICATOR,
                SkillSelectionMenu.SECONDARY_INDICATOR, SkillSelectionMenu.INFO,
                SkillSelectionMenu.CLOSE)) {
            assertThat(slot % 9).as("button column").isGreaterThanOrEqualTo(5);
            assertThat(used.add(slot)).as("button slot %d", slot).isTrue();
        }
    }

    @Test
    void layoutShouldLeaveOutSkillsMissingFromThisMinecraftVersion() {
        when(minecraftGameVersion.isAtLeast(1, 21, 11)).thenReturn(false);

        final Map<Integer, PrimarySkillType> layout = SkillSelectionMenu.layout();

        assertThat(layout).hasSize(18).doesNotContainValue(PrimarySkillType.SPEARS);
        assertThat(List.of(layout.get(1), layout.get(3)))
                .containsExactly(PrimarySkillType.SWORDS, PrimarySkillType.MACES);
        assertThat(layout.get(4)).isNull();
    }

    // --- clicks ---

    private SkillSelectionMenu openMenu() {
        SkillSelectionMenu.open(player, profile);
        return (SkillSelectionMenu) lastCreatedHolder();
    }

    private InventoryHolder lastCreatedHolder() {
        final ArgumentCaptor<InventoryHolder> holders =
                ArgumentCaptor.forClass(InventoryHolder.class);
        mockedBukkit.verify(() -> Bukkit.createInventory(holders.capture(), anyInt(),
                anyString()), atLeastOnce());
        final List<InventoryHolder> all = holders.getAllValues();
        return all.get(all.size() - 1);
    }

    private InventoryClickEvent click(int rawSlot, boolean left) {
        final InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(rawSlot);
        when(event.isLeftClick()).thenReturn(left);
        when(event.isRightClick()).thenReturn(!left);
        return event;
    }

    private static int slotOf(PrimarySkillType skill) {
        return SkillSelectionMenu.layout().entrySet().stream()
                .filter(entry -> entry.getValue() == skill)
                .findFirst().orElseThrow().getKey();
    }

    @Test
    void everyClickShouldBeCancelled() {
        final InventoryClickEvent event = click(0, true);

        openMenu().handleClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    void leftClickShouldAskForConfirmationBeforeChoosingPrimary() {
        openMenu().handleClick(click(slotOf(PrimarySkillType.MINING), true));

        assertThat(lastCreatedHolder()).isInstanceOf(ConfirmChoiceMenu.class);
        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
    }

    @Test
    void confirmingShouldChooseTheSkillAndCancellingShouldNot() {
        openMenu().handleClick(click(slotOf(PrimarySkillType.MINING), false));
        final ConfirmChoiceMenu confirm = (ConfirmChoiceMenu) lastCreatedHolder();

        confirm.handleClick(click(ConfirmChoiceMenu.CANCEL, true));
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY)).isNull();

        confirm.handleClick(click(ConfirmChoiceMenu.CONFIRM, true));
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.MINING);
    }

    @Test
    void withoutConfirmationClicksShouldChooseDirectly() {
        when(generalConfig.getSpecializationGuiConfirmation()).thenReturn(false);
        final SkillSelectionMenu menu = openMenu();

        menu.handleClick(click(slotOf(PrimarySkillType.SWORDS), true));
        menu.handleClick(click(slotOf(PrimarySkillType.ACROBATICS), false));

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.SWORDS);
        assertThat(profile.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.ACROBATICS);
    }

    @Test
    void clickingForAFilledSlotShouldChangeNothing() {
        when(generalConfig.getSpecializationGuiConfirmation()).thenReturn(false);
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);

        openMenu().handleClick(click(slotOf(PrimarySkillType.SWORDS), true));

        assertThat(profile.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.MINING);
    }

    @Test
    void closeButtonShouldCloseTheMenu() {
        openMenu().handleClick(click(SkillSelectionMenu.CLOSE, true));

        verify(player).closeInventory();
    }
}
