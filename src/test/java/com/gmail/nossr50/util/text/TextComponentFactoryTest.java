package com.gmail.nossr50.util.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.config.GeneralConfig;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.skills.SkillTools;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@TestInstance(Lifecycle.PER_CLASS)
class TextComponentFactoryTest {

    private static final @NotNull Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static MockedStatic<mcMMO> mockedMcMMO;
    private SkillTools skillTools;

    @BeforeAll
    void setUpAll() throws IOException {
        mockedMcMMO = Mockito.mockStatic(mcMMO.class);

        mcMMO.p = mock(mcMMO.class);
        when(mcMMO.p.getLogger()).thenReturn(logger);

        skillTools = mock(SkillTools.class);
        when(mcMMO.p.getSkillTools()).thenReturn(skillTools);

        // LocaleLoader bootstrap: locale name from the general config and a writable
        // locales directory for the override file it creates on first use
        final GeneralConfig generalConfig = mock(GeneralConfig.class);
        when(mcMMO.p.getGeneralConfig()).thenReturn(generalConfig);
        when(generalConfig.getLocale()).thenReturn("en_US");

        final Path localesDirectory = Files.createTempDirectory("mcmmo-test-locales");
        mockedMcMMO.when(mcMMO::getLocalesDirectory)
                .thenReturn(localesDirectory + File.separator);
    }

    @AfterAll
    void tearDownAll() {
        mockedMcMMO.close();
    }

    // mcRPG: the getSubSkillWikiLink() tests went with mcMMO's wiki links

    // -------------------------------------------------------------------------
    // getNotificationLevelUpTextComponent()
    // -------------------------------------------------------------------------

    /**
     * Level-up notifications must parse the legacy color codes from the locale into component
     * styles. Literal section-sign characters left inside the component text crash strict
     * MiniMessage-based pipelines downstream and disconnect the receiving player.
     */
    @Test
    void getNotificationLevelUpTextComponentShouldNotLeakLegacySectionCodes() {
        // Given - the bundled en_US locale entry Overhaul.Levelup, which uses & color codes,
        // and the localized skill name resolving to "Mining"
        when(skillTools.getLocalizedSkillName(PrimarySkillType.MINING)).thenReturn("Mining");

        // When - a level up component is built for Mining reaching level 2
        final Component component = TextComponentFactory.getNotificationLevelUpTextComponent(
                PrimarySkillType.MINING, 1, 2);

        // Then - the visible text carries no legacy formatting characters
        final List<Component> segments = flattenDepthFirst(component);
        final String flattenedText = segments.stream()
                .filter(TextComponent.class::isInstance)
                .map(segment -> ((TextComponent) segment).content())
                .collect(Collectors.joining());
        assertThat(flattenedText)
                .isEqualTo("Mining increased to 2.")
                .doesNotContain("§");

        // And - the legacy codes became real styles instead of being stripped
        assertThat(segments).anySatisfy(segment ->
                assertThat(segment.decoration(TextDecoration.BOLD))
                        .isEqualTo(TextDecoration.State.TRUE));
        assertThat(segments).anySatisfy(segment ->
                assertThat(segment.color()).isEqualTo(NamedTextColor.GREEN));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static @NotNull List<Component> flattenDepthFirst(final @NotNull Component root) {
        final List<Component> segments = new ArrayList<>();
        collectSegments(root, segments);
        return segments;
    }

    private static void collectSegments(final @NotNull Component component,
            final @NotNull List<Component> sink) {
        sink.add(component);
        for (final Component child : component.children()) {
            collectSegments(child, sink);
        }
    }
}
