package com.gmail.nossr50.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.MMOTestEnvironment;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.McRPGLinks;
import com.gmail.nossr50.util.Permissions;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** /mcrpg with no arguments: what mcRPG is, some tips, and who develops it. */
class McRPGAboutCommandTest extends MMOTestEnvironment {
    private static final Logger logger = Logger.getLogger(McRPGAboutCommandTest.class.getName());

    private Audience audience;

    @BeforeEach
    void setUp() throws InvalidSkillException {
        mockBaseEnvironment(logger);
        when(Permissions.mcmmoDescription(player)).thenReturn(true);
        final BukkitAudiences audiences = mock(BukkitAudiences.class);
        audience = mock(Audience.class);
        when(audiences.sender(player)).thenReturn(audience);
        mockedMcMMO.when(mcMMO::getAudiences).thenReturn(audiences);
    }

    @AfterEach
    void tearDown() {
        cleanUpStaticMocks();
    }

    /** Every line sent to the player as plain text, without colors. */
    private List<String> textSent() {
        return mockingDetails(player).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("sendMessage"))
                .flatMap(invocation -> Arrays.stream(invocation.getRawArguments()))
                .flatMap(argument -> argument instanceof String[] lines ? Arrays.stream(lines)
                        : Stream.of(argument))
                .map(line -> ChatColor.stripColor(String.valueOf(line)))
                .toList();
    }

    private static String plainText(Component component) {
        final StringBuilder text = new StringBuilder(
                component instanceof TextComponent textComponent ? textComponent.content() : "");
        component.children().forEach(child -> text.append(plainText(child)));
        return text.toString();
    }

    @Test
    void aboutShouldNotListMcMMOsDevelopers() {
        new McmmoCommand().onCommand(player, mock(Command.class), "mcrpg", new String[0]);

        assertThat(textSent()).contains("About mcRPG:").noneMatch(line -> line.matches(
                "(?i).*(nossr50|electronicboy|kashike|t00thpick1|NuclearW|Glitchfinder"
                        + "|Former mcMMO Devs|Its developers).*"));
    }

    @Test
    void aboutShouldCreditJaymingxyzWithAClickableRepositoryLink() {
        new McmmoCommand().onCommand(player, mock(Command.class), "mcrpg", new String[0]);

        final ArgumentCaptor<Component> credit = ArgumentCaptor.forClass(Component.class);
        verify(audience).sendMessage(credit.capture());
        assertThat(plainText(credit.getValue())).isEqualTo(
                "mcRPG is developed by jaymingxyz: https://github.com/jaymingxyz/mcRPG");
        assertThat(credit.getValue().clickEvent())
                .isEqualTo(ClickEvent.openUrl("https://github.com/jaymingxyz/mcRPG"));
    }

    @Test
    void pluginYmlWebsiteShouldBeTheRepository() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/plugin.yml")) {
            assertThat(in).isNotNull();
            // The join MOTD shows plugin.yml's website, so it must match the credit link
            assertThat(new PluginDescriptionFile(in).getWebsite())
                    .isEqualTo(McRPGLinks.REPOSITORY);
        }
        assertThat(McRPGLinks.WIKI).isEqualTo("https://github.com/jaymingxyz/mcRPG/wiki");
    }

    @Test
    void otherLanguagesShouldUseTheEnglishDescription() throws Exception {
        // Their descriptions were mcMMO's, listing its developers
        try (Stream<Path> files = Files.list(Path.of("src/main/resources/locale"))) {
            for (Path file : files.filter(path -> !path.endsWith("locale_en_US.properties"))
                    .toList()) {
                assertThat(Files.readString(file, StandardCharsets.UTF_8)).as(file.toString())
                        .doesNotContainPattern("(?m)^mcMMO\\.Description");
            }
        }
    }
}
