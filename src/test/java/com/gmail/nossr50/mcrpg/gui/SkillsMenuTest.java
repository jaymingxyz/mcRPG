package com.gmail.nossr50.mcrpg.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.skills.RankUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** The skills menu opened by /skills, and each skill's page in it. */
class SkillsMenuTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(SkillsMenuTest.class.getName());

    private PlayerProfile profile;
    private Inventory inventory;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        when(Permissions.skillEnabled(any(), any(PrimarySkillType.class))).thenReturn(true);
        inventory = GuiTestSupport.prepareMenus();
        profile = mmoPlayer.getProfile();
    }

    @AfterEach
    void tearDown() {
        GuiTestSupport.reset();
        cleanUpStaticMocks();
    }

    private SkillsMenu openMenu() {
        SkillsMenu.open(player, mmoPlayer);
        return (SkillsMenu) lastCreatedHolder();
    }

    private SkillInfoMenu openInfo(PrimarySkillType skill) {
        SkillInfoMenu.open(player, profile, skill);
        return (SkillInfoMenu) lastCreatedHolder();
    }

    private InventoryHolder lastCreatedHolder() {
        final ArgumentCaptor<InventoryHolder> holders =
                ArgumentCaptor.forClass(InventoryHolder.class);
        mockedBukkit.verify(() -> Bukkit.createInventory(holders.capture(), anyInt(),
                anyString()), atLeastOnce());
        final List<InventoryHolder> all = holders.getAllValues();
        return all.get(all.size() - 1);
    }

    private InventoryClickEvent click(int rawSlot) {
        final InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(rawSlot);
        when(event.isLeftClick()).thenReturn(true);
        return event;
    }

    private static int slotOf(PrimarySkillType skill) {
        return SkillSelectionMenu.layout().entrySet().stream()
                .filter(entry -> entry.getValue() == skill)
                .findFirst().orElseThrow().getKey();
    }

    private static List<String> plain(List<String> lines) {
        return lines.stream().map(ChatColor::stripColor).toList();
    }

    // --- the skills menu ---

    @Test
    void openingShouldShowEverySkillInTheMenu() {
        openMenu();

        verify(player).openInventory(inventory);
        mockedBukkit.verify(() -> Bukkit.createInventory(any(SkillsMenu.class),
                eq(SkillsMenu.SIZE), eq("Skills")));
    }

    @Test
    void buttonsShouldSitBesideTheSkillsWithoutSharingASlot() {
        final Set<Integer> used = new HashSet<>(SkillSelectionMenu.layout().keySet());
        used.addAll(SkillSelectionMenu.categoryLabels().keySet());
        for (int slot : List.of(SkillsMenu.SUMMARY, SkillsMenu.SPECIALIZATIONS,
                SkillsMenu.HELP, SkillsMenu.CLOSE)) {
            assertThat(slot % 9).as("button column").isGreaterThanOrEqualTo(5);
            assertThat(slot).isLessThan(SkillsMenu.SIZE);
            assertThat(used.add(slot)).as("button slot %d", slot).isTrue();
        }
    }

    @Test
    void everyClickShouldBeCancelled() {
        final InventoryClickEvent event = click(slotOf(PrimarySkillType.MINING));

        openMenu().handleClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    void clickingASkillShouldOpenItsPage() {
        openMenu().handleClick(click(slotOf(PrimarySkillType.HERBALISM)));

        assertThat(lastCreatedHolder()).isInstanceOfSatisfying(SkillInfoMenu.class,
                page -> assertThat(page.skill()).isEqualTo(PrimarySkillType.HERBALISM));
    }

    @Test
    void clickingACategoryLabelShouldStayOnTheMenu() {
        final SkillsMenu menu = openMenu();

        // Metallurgy's label is the first slot of the third row
        menu.handleClick(click(18));

        assertThat(lastCreatedHolder()).isSameAs(menu);
    }

    @Test
    void specializationsButtonShouldOpenTheSpecializationMenu() {
        when(player.hasPermission(SkillsMenu.CHOOSE_SPECIALIZATION_PERMISSION)).thenReturn(true);

        openMenu().handleClick(click(SkillsMenu.SPECIALIZATIONS));

        assertThat(lastCreatedHolder()).isInstanceOf(SkillSelectionMenu.class);
    }

    @Test
    void helpBookShouldCloseTheMenuAndSendTheWikiLink() {
        final BukkitAudiences audiences = mock(BukkitAudiences.class);
        final Audience audience = mock(Audience.class);
        when(audiences.sender(player)).thenReturn(audience);
        mockedMcMMO.when(mcMMO::getAudiences).thenReturn(audiences);

        openMenu().handleClick(click(SkillsMenu.HELP));

        verify(player).closeInventory();
        final ArgumentCaptor<Component> link = ArgumentCaptor.forClass(Component.class);
        verify(audience).sendMessage(link.capture());
        assertThat(link.getValue().clickEvent())
                .isEqualTo(ClickEvent.openUrl("https://github.com/jaymingxyz/mcRPG/wiki"));
    }

    @Test
    void specializationsButtonShouldDoNothingWithoutPermission() {
        when(player.hasPermission(SkillsMenu.CHOOSE_SPECIALIZATION_PERMISSION)).thenReturn(false);
        final SkillsMenu menu = openMenu();

        menu.handleClick(click(SkillsMenu.SPECIALIZATIONS));

        assertThat(lastCreatedHolder()).isSameAs(menu);
    }

    @Test
    void closeButtonShouldCloseTheMenu() {
        openMenu().handleClick(click(SkillsMenu.CLOSE));

        verify(player).closeInventory();
    }

    // --- a skill's page ---

    @Test
    void pageShouldBeTitledWithTheSkill() {
        openInfo(PrimarySkillType.MINING);

        mockedBukkit.verify(() -> Bukkit.createInventory(any(SkillInfoMenu.class),
                eq(SkillInfoMenu.SIZE), eq("Skills: Mining")));
    }

    @Test
    void backShouldReturnToTheSkillsMenuAndCloseShouldClose() {
        final SkillInfoMenu page = openInfo(PrimarySkillType.MINING);

        page.handleClick(click(SkillInfoMenu.CLOSE));
        verify(player).closeInventory();

        page.handleClick(click(SkillInfoMenu.BACK));
        assertThat(lastCreatedHolder()).isInstanceOf(SkillsMenu.class);
    }

    @Test
    void clickingAnAbilityShouldOnlyBeCancelled() {
        final SkillInfoMenu page = openInfo(PrimarySkillType.MINING);
        final InventoryClickEvent event = click(SkillInfoMenu.ABILITY_SLOTS.get(0));

        page.handleClick(event);

        verify(event).setCancelled(true);
        verify(player, never()).closeInventory();
        assertThat(lastCreatedHolder()).isSameAs(page);
    }

    @Test
    void pageSlotsShouldNotOverlap() {
        final Set<Integer> used = new HashSet<>();
        final List<Integer> all = new ArrayList<>(SkillInfoMenu.ABILITY_SLOTS);
        all.addAll(SkillInfoMenu.GUIDE_SLOTS);
        all.addAll(List.of(SkillInfoMenu.HEADER, SkillInfoMenu.ABILITIES_LABEL,
                SkillInfoMenu.GUIDE_LABEL, SkillInfoMenu.BACK, SkillInfoMenu.CLOSE));
        for (int slot : all) {
            assertThat(slot).isBetween(0, SkillInfoMenu.SIZE - 1);
            assertThat(used.add(slot)).as("slot %d", slot).isTrue();
        }
    }

    @Test
    void abilitiesShouldBeTheSkillsSubSkillsThePlayerMayUse() {
        when(Permissions.isSubSkillEnabled(player, SubSkillType.MINING_BLAST_MINING))
                .thenReturn(false);

        assertThat(SkillInfoMenu.abilities(player, PrimarySkillType.MINING)).containsExactly(
                SubSkillType.MINING_BIGGER_BOMBS, SubSkillType.MINING_DEMOLITIONS_EXPERTISE,
                SubSkillType.MINING_DOUBLE_DROPS, SubSkillType.MINING_SUPER_BREAKER,
                SubSkillType.MINING_MOTHER_LODE);
    }

    @Test
    void everySkillsAbilitiesAndGuideShouldFitOnItsPage() {
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            assertThat(SkillInfoMenu.abilities(player, skill)).as("%s abilities", skill)
                    .isNotEmpty().hasSizeLessThanOrEqualTo(SkillInfoMenu.ABILITY_SLOTS.size());
            assertThat(SkillInfoMenu.guide(skill)).as("%s guide pages", skill)
                    .isNotEmpty().hasSizeLessThanOrEqualTo(SkillInfoMenu.GUIDE_SLOTS.size());
        }
    }

    @Test
    void everyAbilityShouldHaveANameAndDescription() {
        for (SubSkillType ability : SubSkillType.values()) {
            assertThat(ability.getLocaleName()).as("%s name", ability).doesNotStartWith("!");
            assertThat(ability.getLocaleDescription()).as("%s description", ability)
                    .doesNotStartWith("!");
        }
    }

    private void unlockLevels(SubSkillType ability, int... levels) {
        for (int rank = 1; rank <= levels.length; rank++) {
            when(RankUtils.getRankUnlockLevel(ability, rank)).thenReturn(levels[rank - 1]);
        }
    }

    @Test
    void rankShouldBeTheHighestOneTheLevelHasReached() {
        unlockLevels(SubSkillType.SALVAGE_SCRAP_COLLECTOR, 0, 10, 15, 20, 25, 30, 35, 40);

        assertThat(SkillInfoMenu.rank(SubSkillType.SALVAGE_SCRAP_COLLECTOR, 0)).isEqualTo(1);
        assertThat(SkillInfoMenu.rank(SubSkillType.SALVAGE_SCRAP_COLLECTOR, 12)).isEqualTo(2);
        assertThat(SkillInfoMenu.rank(SubSkillType.SALVAGE_SCRAP_COLLECTOR, 100)).isEqualTo(8);
    }

    @Test
    void abilityLoreShouldSayWhenItUnlocksAndWhatComesNext() {
        unlockLevels(SubSkillType.SALVAGE_SCRAP_COLLECTOR, 5, 10, 15, 20, 25, 30, 35, 40);

        assertThat(plain(SkillInfoMenu.abilityLore(SubSkillType.SALVAGE_SCRAP_COLLECTOR, 0)))
                .contains("Unlocks at level 5");
        assertThat(plain(SkillInfoMenu.abilityLore(SubSkillType.SALVAGE_SCRAP_COLLECTOR, 12)))
                .contains("Rank 2/8", "Next rank at level 15");
        assertThat(plain(SkillInfoMenu.abilityLore(SubSkillType.SALVAGE_SCRAP_COLLECTOR, 40)))
                .contains("Rank 8/8 (max)");
    }

    @Test
    void abilitiesWithoutRanksShouldAlwaysBeUnlocked() {
        assertThat(plain(SkillInfoMenu.abilityLore(SubSkillType.ACROBATICS_ROLL, 0)))
                .first().isEqualTo("Land strategically to avoid damage.");
        assertThat(plain(SkillInfoMenu.abilityLore(SubSkillType.ACROBATICS_ROLL, 0)))
                .last().isEqualTo("Unlocked");
    }

    @Test
    void guidePagesShouldStartWithTheirHeading() {
        final List<List<String>> guide = SkillInfoMenu.guide(PrimarySkillType.MINING);

        assertThat(guide).hasSize(6);
        assertThat(ChatColor.stripColor(guide.get(0).get(0))).isEqualTo("About Mining:");
        assertThat(ChatColor.stripColor(guide.get(2).get(0)))
                .isEqualTo("How to use Super Breaker:");
    }

    @Test
    void smeltingAndSalvageGuidesShouldDescribeMcRPG() {
        final String smelting = String.join("\n", SkillInfoMenu.guide(PrimarySkillType.SMELTING)
                .stream().flatMap(List::stream).toList());
        final String salvage = String.join("\n", SkillInfoMenu.guide(PrimarySkillType.SALVAGE)
                .stream().flatMap(List::stream).toList());

        assertThat(smelting).doesNotContain("Coming soon").contains("Second Smelt");
        assertThat(salvage).doesNotContain("child skill").contains("Salvage Mastery");
    }

    // --- wrapping ---

    @Test
    void wrapShouldBreakLongLinesAndKeepTheirColor() {
        assertThat(MenuItems.wrap(ChatColor.YELLOW + "one two three", 7))
                .containsExactly(ChatColor.YELLOW + "one two", ChatColor.YELLOW + "three");
    }

    @Test
    void wrapShouldKeepLineBreaksAndBlankLines() {
        assertThat(MenuItems.wrap("first\n\nsecond", 40))
                .containsExactly("first", "", "second");
    }
}
