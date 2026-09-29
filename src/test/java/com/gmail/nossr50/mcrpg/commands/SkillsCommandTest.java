package com.gmail.nossr50.mcrpg.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcrpg.McRPGTestSettings;
import com.gmail.nossr50.mcrpg.gui.GuiTestSupport;
import com.gmail.nossr50.mcrpg.gui.SkillInfoMenu;
import com.gmail.nossr50.mcrpg.gui.SkillsMenu;
import com.gmail.nossr50.util.Permissions;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** /skills (also /rpgskills) opens the skills menu, or one skill's page. */
class SkillsCommandTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(SkillsCommandTest.class.getName());

    private final Command command = mock(Command.class);
    private Inventory menu;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        McRPGTestSettings.useDefaults(generalConfig, mockedMcMMO);
        when(Permissions.skillEnabled(any(), any(PrimarySkillType.class))).thenReturn(true);
        menu = GuiTestSupport.prepareMenus();
    }

    @AfterEach
    void tearDown() {
        GuiTestSupport.reset();
        cleanUpStaticMocks();
    }

    private boolean skills(String... args) {
        return new SkillsCommand().onCommand(player, command, "skills", args);
    }

    @Test
    void noArgumentsShouldOpenTheSkillsMenu() {
        skills();

        verify(player).openInventory(menu);
        mockedBukkit.verify(() -> Bukkit.createInventory(any(SkillsMenu.class), anyInt(),
                anyString()));
    }

    @Test
    void aSkillNameShouldOpenThatSkillsPage() {
        skills("mining");

        verify(player).openInventory(menu);
        mockedBukkit.verify(() -> Bukkit.createInventory(any(SkillInfoMenu.class), anyInt(),
                eq("Skills: Mining")));
    }

    @Test
    void anUnknownSkillShouldSaySoAndOpenNothing() {
        skills("cooking");

        verify(player).sendMessage(LocaleLoader.getString("Commands.Skill.Invalid"));
        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    void aSkillMissingFromThisMinecraftVersionShouldBeUnknown() {
        when(minecraftGameVersion.isAtLeast(1, 21, 11)).thenReturn(false);

        skills("spears");

        verify(player).sendMessage(LocaleLoader.getString("Commands.Skill.Invalid"));
        mockedBukkit.verify(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(),
                anyString()), never());
    }

    @Test
    void tooManyArgumentsShouldShowTheUsage() {
        assertThat(skills("mining", "extra")).isFalse();
    }

    @Test
    void tabCompleteShouldSuggestSkills() {
        assertThat(new SkillsCommand().onTabComplete(player, command, "skills",
                new String[] {"mi"})).containsExactly("mining");
        assertThat(new SkillsCommand().onTabComplete(player, command, "skills",
                new String[] {"mining", ""})).isEmpty();
    }

    @Test
    void skillsAndRpgskillsShouldBelongToThisCommandOnly() throws Exception {
        final PluginDescriptionFile pluginYml;
        try (InputStream in = getClass().getResourceAsStream("/plugin.yml")) {
            assertThat(in).isNotNull();
            pluginYml = new PluginDescriptionFile(in);
        }

        final Map<String, Map<String, Object>> commands = pluginYml.getCommands();
        assertThat(aliases(commands.get("skills"))).containsExactly("rpgskills");
        assertThat(commands.get("skills")).containsEntry("permission", "mcrpg.commands.skills");
        for (Map.Entry<String, Map<String, Object>> other : commands.entrySet()) {
            if (other.getKey().equals("skills")) {
                continue;
            }
            assertThat(other.getKey()).isNotEqualTo("rpgskills");
            assertThat(aliases(other.getValue())).as(other.getKey())
                    .doesNotContain("skills", "rpgskills");
        }
    }

    private static List<String> aliases(Map<String, Object> command) {
        return command.get("aliases") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList() : List.of();
    }
}
