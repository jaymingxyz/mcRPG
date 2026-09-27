package com.gmail.nossr50.mcrpg;

import static org.assertj.core.api.Assertions.assertThat;

import com.gmail.nossr50.util.blockmeta.McMMORegionBackupStore;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Checks that mcRPG's player-facing names stay renamed. Merging an mcMMO update brings in new
 * {@code mcmmo.} permissions and new text; when one of these tests fails after a merge, run
 * {@code java scripts/McRPGRename.java} from the plugin folder.
 */
class McRPGBrandingTest {
    private static final String FIX = "run 'java scripts/McRPGRename.java' from the plugin folder";
    /** An mcMMO permission node, but not a web address such as www.mcmmo.org. */
    private static final Pattern MCMMO_PERMISSION = Pattern.compile("(?<![\\w./:])mcmmo\\.");
    /** English entries that credit mcMMO or link to its sites. */
    private static final Pattern KEEP_KEYS =
            Pattern.compile("JSON\\.URL\\..*|mcMMO\\.Description(\\..*)?|MOTD\\.Website");
    /** mcMMO's command names, which mcRPG replaced with rpg-prefixed ones. */
    private static final Set<String> MCMMO_COMMANDS = Set.of("mcmmo", "mcstats", "mctop",
            "mcrank", "mmoedit", "mmoinfo", "mmoxpbar", "mmodebug", "mmopower", "mcability",
            "mcrefresh", "mccooldown", "mcgod", "mcremove", "mmoshowdb", "mcconvert", "mcpurge",
            "mcnotify", "mclevelupsound", "mcscoreboard", "mcmmoreloadlocale", "addxp",
            "addlevels", "inspect", "skillreset");

    private static String resource(String name) throws IOException {
        try (InputStream in = McRPGBrandingTest.class.getClassLoader()
                .getResourceAsStream(name)) {
            assertThat(in).as("%s on the test classpath", name).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void pluginShouldBeNamedMcRPG() throws IOException {
        final YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(McRPGBrandingTest.class.getClassLoader()
                        .getResourceAsStream("plugin.yml"), StandardCharsets.UTF_8));

        assertThat(pluginYml.getString("name")).isEqualTo("mcRPG");
        assertThat(pluginYml.getConfigurationSection("commands").getKeys(false))
                .as("mcMMO command names in plugin.yml").doesNotContainAnyElementsOf(
                        MCMMO_COMMANDS);
    }

    @Test
    void pluginYmlShouldHaveNoMcMMOPermissions() throws IOException {
        final List<String> found = new ArrayList<>();
        resource("plugin.yml").lines()
                .filter(line -> MCMMO_PERMISSION.matcher(line).find())
                .forEach(found::add);

        assertThat(found).as("mcmmo. permissions in plugin.yml; " + FIX).isEmpty();
    }

    @Test
    void javaSourcesShouldHaveNoMcMMOPermissions() throws IOException {
        final Pattern permissionString = Pattern.compile("\"mcmmo\\.(?!users)");
        final List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (permissionString.matcher(Files.readString(file)).find()) {
                    found.add(file.toString());
                }
            }
        }

        assertThat(found).as("files with \"mcmmo. permission strings; " + FIX).isEmpty();
    }

    @Test
    void englishTextShouldSayMcRPG() throws IOException {
        final Pattern brand = Pattern.compile("(?i)(?<![./])mcmmo(?!\\.org)");
        final List<String> found = new ArrayList<>();
        // The build copies locale files into the locale package
        final String english = resource("com/gmail/nossr50/locale/locale_en_US.properties");
        for (String line : english.lines().toList()) {
            final int equals = line.indexOf('=');
            if (line.startsWith("#") || equals < 0
                    || KEEP_KEYS.matcher(line.substring(0, equals).strip()).matches()) {
                continue;
            }
            if (brand.matcher(line.substring(equals)).find()) {
                found.add(line);
            }
        }

        assertThat(found).as("English text naming mcMMO; " + FIX).isEmpty();
    }

    @Test
    void dataShouldBeStoredUnderMcRPGNames() throws IOException {
        // A server switching from mcMMO starts fresh instead of reading mcMMO's block data
        assertThat(McMMORegionBackupStore.IN_WORLD_FOLDER_NAME).isEqualTo("mcrpg_regions");

        final YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(McRPGBrandingTest.class.getClassLoader()
                        .getResourceAsStream("config.yml"), StandardCharsets.UTF_8));
        assertThat(config.getString("MySQL.Database.TablePrefix")).isEqualTo("mcrpg_");
    }
}
