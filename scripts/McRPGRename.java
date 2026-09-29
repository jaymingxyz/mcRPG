import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Renames mcMMO's permissions, command names and brand to mcRPG's.
 * <p>
 * Run it from the plugin folder after merging an mcMMO update, because merges bring in new
 * {@code mcmmo.} permission nodes and new locale text:
 * <pre>
 *     java scripts/McRPGRename.java           rewrites the files
 *     java scripts/McRPGRename.java --check   only lists files that need changes (exit code 1)
 * </pre>
 * It's safe to run more than once: renamed text never matches again.
 * <ul>
 *     <li><b>Permissions</b> in {@code plugin.yml} and all Java sources (main and test):
 *     {@code mcmmo.} becomes {@code mcrpg.}, and command permissions drop their old prefix, for
 *     example {@code mcmmo.commands.mcstats} becomes {@code mcrpg.commands.stats}.</li>
 *     <li><b>Locale files:</b> {@code Commands.Description.<command>} keys follow the command
 *     renames, and values get the new {@code /command} names and the mcRPG brand. Credit
 *     entries in {@link #KEEP_KEYS} are left alone.</li>
 *     <li><b>{@code plugin.yml} descriptions</b> get the new command names and the brand.</li>
 *     <li><b>Config files</b> ({@code src/main/resources/*.yml}): comments get the new
 *     permissions, command names and brand, and {@code %mcmmo_...%} PlaceholderAPI examples
 *     become {@code %mcrpg_...%}. Keys and values are left alone.</li>
 * </ul>
 * The command declarations themselves ({@code plugin.yml} {@code commands:} and
 * {@code CommandRegistrationManager}) were renamed by hand and aren't touched here.
 * Files are read and written byte for byte (ISO-8859-1), so encodings and line endings are
 * kept.
 */
public final class McRPGRename {
    /** mcMMO command name (and old aliases) to mcRPG command name. */
    private static final Map<String, String> COMMANDS = new LinkedHashMap<>();
    /** Permission segment after {@code commands.} for commands whose permission changes. */
    private static final Map<String, String> COMMAND_PERMISSIONS = new LinkedHashMap<>();

    static {
        command("mmoxpbar", "rpgxpbar", "xpbar");
        command("mmodebug", "rpgdebug", "debug");
        command("mmoinfo", "rpginfo", "info");
        command("mcinfo", "rpginfo", null);
        command("mcmmo", "mcrpg", "mcrpg");
        command("mctop", "rpgtop", "top");
        command("mcrank", "rpgrank", "rank");
        command("addxp", "rpgaddxp", null);
        command("addlevels", "rpgaddlevels", null);
        command("mcability", "rpgability", "ability");
        command("mcrefresh", "rpgrefresh", "refresh");
        command("mccooldown", "rpgcooldowns", "cooldowns");
        command("mccooldowns", "rpgcooldowns", "cooldowns");
        command("mcgod", "rpggod", "god");
        command("mcstats", "rpgstats", "stats");
        command("mcremove", "rpgremove", "remove");
        command("mmoedit", "rpgsetlevel", "setlevel");
        command("inspect", "rpginspect", null);
        command("mmoshowdb", "rpgshowdb", "showdb");
        command("mcconvert", "rpgconvert", "convert");
        command("skillreset", "rpgskillreset", null);
        command("mmopower", "rpgpower", "power");
        command("mcpurge", "rpgpurge", "purge");
        command("mcnotify", "rpgnotify", "notify");
        command("mclevelupsound", "rpglevelupsound", "levelupsound");
        command("mcscoreboard", "rpgscoreboard", "scoreboard");
        command("mcsb", "rpgsb", null);
        command("mcmmoreloadlocale", "rpgreloadlocale", null);
        command("mcreloadlocale", "rpgreloadlocale", null);
    }

    /** First segments of mcMMO permission nodes ({@code mcmmo.<root>...}). */
    private static final Set<String> PERMISSION_ROOTS = Set.of("ability", "admin", "all",
            "broadcast", "bypass", "chat", "commands", "defaults", "defaultsop", "item", "motd",
            "party", "perks", "showversion", "skillreset", "skills", "tools", "*");

    /**
     * Locale entries that credit mcMMO, so they keep its name. The English versions of these
     * were rewritten by hand for mcRPG. (mcMMO's JSON.URL link entries were removed.)
     */
    private static final List<Pattern> KEEP_KEYS = List.of(
            Pattern.compile("mcMMO\\.Description(\\..*)?"),
            Pattern.compile("MOTD\\.Website"));

    /** Config comments about mcMMO's own history, which stay as they are. */
    private static final List<Pattern> KEEP_COMMENTS = List.of(
            Pattern.compile("For 10 years mcMMO"),
            Pattern.compile("feeling of mcMMO"));

    private static final Pattern COMMAND_PERMISSION;
    private static final Pattern PERMISSION;
    private static final Pattern SLASH_COMMAND;
    private static final Pattern NAMED_COMMAND;
    private static final Pattern DESCRIPTION_KEY;
    /**
     * Start of a word: after a color code such as {@code &a} or {@code §a}, or after anything
     * that isn't part of a word, path or web address.
     */
    private static final String WORD_START =
            "(?:(?<=[&§][0-9a-fk-orA-FK-OR])|(?<![\\w./:]))";
    /** The brand in any case, but not inside a web address such as www.mcmmo.org. */
    private static final Pattern BRAND =
            Pattern.compile("(?i)" + WORD_START + "mcmmo(?!\\.org)");

    static {
        final String commandPermissions = String.join("|", COMMAND_PERMISSIONS.keySet());
        final String commands = String.join("|", COMMANDS.keySet());
        COMMAND_PERMISSION = Pattern.compile(
                "(?<![\\w.])mcmmo\\.commands\\.(" + commandPermissions + ")(?!\\w)");
        PERMISSION = Pattern.compile("(?<![\\w./:])mcmmo\\.(" + String.join("|",
                PERMISSION_ROOTS.stream().map(Pattern::quote).toList()) + ")(?![\\w])");
        SLASH_COMMAND = Pattern.compile(WORD_START + "/(" + commands + ")(?!\\w)");
        NAMED_COMMAND = Pattern.compile("(?<![\\w./])(" + commands + ")(?= command)");
        DESCRIPTION_KEY = Pattern.compile("^Commands\\.Description\\.(" + commands + ")$");
    }

    private static void command(String mcmmo, String mcrpg, String permission) {
        COMMANDS.put(mcmmo, mcrpg);
        if (permission != null) {
            COMMAND_PERMISSIONS.put(mcmmo, permission);
        }
    }

    public static void main(String[] args) throws IOException {
        final boolean check = List.of(args).contains("--check");
        final Path root = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(root.resolve("pom.xml"))) {
            System.err.println("Run this from the plugin folder (the one with pom.xml).");
            System.exit(2);
        }

        final List<Path> changed = new ArrayList<>();
        process(root.resolve("src/main/resources/plugin.yml"), McRPGRename::pluginYml, check,
                changed);
        for (Path config : list(root.resolve("src/main/resources"), ".yml")) {
            if (config.getParent().equals(root.resolve("src/main/resources"))
                    && !config.endsWith("plugin.yml")) {
                process(config, McRPGRename::configYml, check, changed);
            }
        }
        for (Path locale : list(root.resolve("src/main/resources/locale"), ".properties")) {
            process(locale, McRPGRename::locale, check, changed);
        }
        for (String sources : List.of("src/main/java", "src/test/java")) {
            for (Path java : list(root.resolve(sources), ".java")) {
                process(java, McRPGRename::permissions, check, changed);
            }
        }

        for (Path path : changed) {
            System.out.println((check ? "needs renaming: " : "renamed: ") + root.relativize(path));
        }
        System.out.println(changed.size() + " file(s) " + (check ? "need renaming" : "changed"));
        if (check && !changed.isEmpty()) {
            System.exit(1);
        }
    }

    private static List<Path> list(Path folder, String extension) throws IOException {
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(path -> path.toString().endsWith(extension)).sorted().toList();
        }
    }

    private interface Rewrite {
        String apply(String text);
    }

    private static void process(Path path, Rewrite rewrite, boolean check, List<Path> changed)
            throws IOException {
        final String before = Files.readString(path, StandardCharsets.ISO_8859_1);
        final String after = rewrite.apply(before);
        if (!after.equals(before)) {
            changed.add(path);
            if (!check) {
                Files.writeString(path, after, StandardCharsets.ISO_8859_1);
            }
        }
    }

    static String permissions(String text) {
        text = replace(COMMAND_PERMISSION, text,
                m -> "mcrpg.commands." + COMMAND_PERMISSIONS.get(m.group(1)));
        return replace(PERMISSION, text, m -> "mcrpg." + m.group(1));
    }

    static String pluginYml(String text) {
        final StringBuilder out = new StringBuilder();
        for (String line : lines(text)) {
            line = permissions(line);
            if (line.stripLeading().startsWith("description:")) {
                line = replace(NAMED_COMMAND, line, m -> COMMANDS.get(m.group(1)));
                line = replace(SLASH_COMMAND, line, m -> "/" + COMMANDS.get(m.group(1)));
                line = replace(BRAND, line, m -> "mcRPG");
            }
            out.append(line);
        }
        return out.toString();
    }

    /**
     * Config files: PlaceholderAPI examples everywhere, and permissions, commands and the
     * brand in comments. Keys and values are left alone, since the code reads them by name.
     */
    static String configYml(String text) {
        final StringBuilder out = new StringBuilder();
        for (String line : lines(text)) {
            line = line.replace("%mcmmo_", "%mcrpg_");
            final String comment = line.stripLeading();
            if (comment.startsWith("#")
                    && KEEP_COMMENTS.stream().noneMatch(keep -> keep.matcher(comment).find())) {
                line = permissions(line);
                line = replace(SLASH_COMMAND, line, m -> "/" + COMMANDS.get(m.group(1)));
                line = replace(BRAND, line, m -> "mcRPG");
            }
            out.append(line);
        }
        return out.toString();
    }

    static String locale(String text) {
        final StringBuilder out = new StringBuilder();
        for (String line : lines(text)) {
            final int equals = line.indexOf('=');
            final String trimmed = line.stripLeading();
            if (equals < 0 || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                out.append(line);
                continue;
            }
            String key = line.substring(0, equals);
            String value = line.substring(equals);
            final String name = key.strip();
            if (KEEP_KEYS.stream().anyMatch(keep -> keep.matcher(name).matches())) {
                out.append(line);
                continue;
            }
            final Matcher description = DESCRIPTION_KEY.matcher(name);
            if (description.matches()) {
                key = key.replace(name,
                        "Commands.Description." + COMMANDS.get(description.group(1)));
            }
            value = replace(SLASH_COMMAND, value, m -> "/" + COMMANDS.get(m.group(1)));
            value = replace(BRAND, value, m -> "mcRPG");
            out.append(key).append(value);
        }
        return out.toString();
    }

    /** Splits text into lines that keep their line endings, so joining them is lossless. */
    private static List<String> lines(String text) {
        return List.of(text.split("(?<=\n)", -1));
    }

    private interface Replacement {
        String apply(Matcher match);
    }

    private static String replace(Pattern pattern, String text, Replacement replacement) {
        final Matcher matcher = pattern.matcher(text);
        final StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.apply(matcher)));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
