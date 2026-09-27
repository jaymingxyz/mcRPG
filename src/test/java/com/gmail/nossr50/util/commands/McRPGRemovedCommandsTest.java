package com.gmail.nossr50.util.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

/**
 * mcRPG drops mcMMO's party system, chat channels (admin chat, party chat and chat spying), XP
 * rate events and XP perks. These tests check that none of their commands or permission nodes
 * come back, for example through a merge of a newer mcMMO release that adds nodes for a new
 * skill.
 */
class McRPGRemovedCommandsTest {
    private static final List<String> REMOVED_COMMANDS =
            List.of("party", "ptp", "partychat", "adminchat", "mcchatspy", "xprate");

    /**
     * Permission nodes for the removed features, including XP perk nodes for every skill. Uses
     * the renamed {@code mcrpg.} prefix; McRPGBrandingTest checks no {@code mcmmo.} nodes remain.
     */
    private static final Pattern REMOVED_PERMISSION = Pattern.compile(
            "mcrpg\\.(commands\\.(xprate|party|ptp|mcchatspy)(\\..*)?|chat(\\..*)?"
                    + "|perks\\.xp(\\..*)?|party(\\..*)?|bypass\\.partylimit)");

    @Test
    void removedCommandsShouldNotBeRegistered() {
        assertThat(CommandRegistrationManager.specCommandNames())
                .doesNotContainAnyElementsOf(REMOVED_COMMANDS);
    }

    @Test
    void removedCommandsShouldNotBeDeclaredInPluginYml() throws Exception {
        // Given - plugin.yml parsed the way the server parses it
        final PluginDescriptionFile pluginYml = loadPluginYml();

        // Then - none of the removed commands, or their aliases, are declared
        assertThat(pluginYml.getCommands().keySet()).doesNotContainAnyElementsOf(REMOVED_COMMANDS);
        final List<String> aliases = new ArrayList<>();
        for (Map<String, Object> command : pluginYml.getCommands().values()) {
            final Object commandAliases = command.get("aliases");
            if (commandAliases instanceof List<?> list) {
                list.forEach(alias -> aliases.add(String.valueOf(alias)));
            } else if (commandAliases != null) {
                aliases.add(String.valueOf(commandAliases));
            }
        }
        assertThat(aliases).as("command aliases").doesNotContain("p", "pc", "a", "ac",
                "mcxprate");
    }

    @Test
    void removedPermissionsShouldNotBeDeclaredOrGrantedInPluginYml() throws Exception {
        // Given - every permission declared in plugin.yml, and every child it grants
        final PluginDescriptionFile pluginYml = loadPluginYml();
        final List<String> declared = new ArrayList<>();
        final List<String> granted = new ArrayList<>();
        for (Permission permission : pluginYml.getPermissions()) {
            declared.add(permission.getName());
            granted.addAll(permission.getChildren().keySet());
        }

        // Then - none of them belong to a removed feature
        assertThat(declared).as("declared permission nodes")
                .isNotEmpty()
                .noneMatch(node -> REMOVED_PERMISSION.matcher(node).matches());
        assertThat(granted).as("child permission nodes")
                .noneMatch(node -> REMOVED_PERMISSION.matcher(node).matches());
    }

    @Test
    void nonXpPerksShouldStillBeDeclared() throws Exception {
        // Only XP perks were removed; cooldown, activation time and lucky perks stay
        final List<String> declared = loadPluginYml().getPermissions().stream()
                .map(Permission::getName)
                .toList();

        assertThat(declared).contains("mcrpg.perks.cooldowns.all",
                "mcrpg.perks.activationtime.all", "mcrpg.perks.lucky.all");
    }

    private static PluginDescriptionFile loadPluginYml() throws Exception {
        try (InputStream in = McRPGRemovedCommandsTest.class.getResourceAsStream("/plugin.yml")) {
            assertThat(in).as("plugin.yml on the test classpath").isNotNull();
            return new PluginDescriptionFile(in);
        }
    }
}
