package com.gmail.nossr50.util.commands;

import com.gmail.nossr50.commands.McLevelUpSoundCommand;
import com.gmail.nossr50.commands.McabilityCommand;
import com.gmail.nossr50.commands.McconvertCommand;
import com.gmail.nossr50.commands.McgodCommand;
import com.gmail.nossr50.commands.McmmoCommand;
import com.gmail.nossr50.commands.McnotifyCommand;
import com.gmail.nossr50.commands.McrefreshCommand;
import com.gmail.nossr50.commands.McscoreboardCommand;
import com.gmail.nossr50.commands.admin.McmmoReloadLocaleCommand;
import com.gmail.nossr50.commands.admin.PlayerDebugCommand;
import com.gmail.nossr50.commands.database.McpurgeCommand;
import com.gmail.nossr50.commands.database.McremoveCommand;
import com.gmail.nossr50.commands.database.MmoshowdbCommand;
import com.gmail.nossr50.commands.experience.AddlevelsCommand;
import com.gmail.nossr50.commands.experience.AddxpCommand;
import com.gmail.nossr50.commands.experience.MmoeditCommand;
import com.gmail.nossr50.commands.experience.SkillresetCommand;
import com.gmail.nossr50.commands.player.InspectCommand;
import com.gmail.nossr50.commands.player.McRankCommand;
import com.gmail.nossr50.commands.player.McTopCommand;
import com.gmail.nossr50.commands.player.MccooldownCommand;
import com.gmail.nossr50.commands.player.McstatsCommand;
import com.gmail.nossr50.commands.player.XPBarCommand;
import com.gmail.nossr50.commands.skills.AcrobaticsCommand;
import com.gmail.nossr50.commands.skills.AlchemyCommand;
import com.gmail.nossr50.commands.skills.ArcheryCommand;
import com.gmail.nossr50.commands.skills.AxesCommand;
import com.gmail.nossr50.commands.skills.CrossbowsCommand;
import com.gmail.nossr50.commands.skills.ExcavationCommand;
import com.gmail.nossr50.commands.skills.FishingCommand;
import com.gmail.nossr50.commands.skills.HerbalismCommand;
import com.gmail.nossr50.commands.skills.MacesCommand;
import com.gmail.nossr50.commands.skills.MiningCommand;
import com.gmail.nossr50.commands.skills.MmoInfoCommand;
import com.gmail.nossr50.commands.skills.RepairCommand;
import com.gmail.nossr50.commands.skills.SalvageCommand;
import com.gmail.nossr50.commands.skills.SmeltingCommand;
import com.gmail.nossr50.commands.skills.SpearsCommand;
import com.gmail.nossr50.commands.skills.SwordsCommand;
import com.gmail.nossr50.commands.skills.TamingCommand;
import com.gmail.nossr50.commands.skills.TridentsCommand;
import com.gmail.nossr50.commands.skills.UnarmedCommand;
import com.gmail.nossr50.commands.skills.WoodcuttingCommand;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.commands.AbandonSpecializationCommand;
import com.gmail.nossr50.mcrpg.commands.ChooseSpecializationCommand;
import com.gmail.nossr50.mcrpg.commands.SetSpecializationCommand;
import com.gmail.nossr50.util.text.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CommandRegistrationManager {
    private CommandRegistrationManager() {
    }

    /**
     * Everything needed to wire one plugin.yml command declaration to its executor. Description
     * and usage lines are suppliers so the table can be built without touching the locale or a
     * running plugin instance.
     */
    private record CommandSpec(@NotNull String name, @NotNull Supplier<String> description,
            @Nullable String permission, @NotNull Supplier<List<String>> usageLines,
            @NotNull Supplier<? extends CommandExecutor> executor, @NotNull List<String> aliases,
            boolean requiresParty) {
    }

    private static @NotNull CommandSpec spec(@NotNull String name, @Nullable String permission,
            @NotNull Supplier<List<String>> usageLines,
            @NotNull Supplier<? extends CommandExecutor> executor) {
        return spec(name, () -> LocaleLoader.getString("Commands.Description." + name),
                permission, usageLines, executor);
    }

    private static @NotNull CommandSpec spec(@NotNull String name,
            @NotNull Supplier<String> description, @Nullable String permission,
            @NotNull Supplier<List<String>> usageLines,
            @NotNull Supplier<? extends CommandExecutor> executor) {
        return new CommandSpec(name, description, permission, usageLines, executor, List.of(),
                false);
    }

    private static final List<CommandSpec> COMMAND_SPECS = List.of(
            // Generic Commands
            spec("rpgxpbar", null, () -> List.of(
                    LocaleLoader.getString("Commands.Usage.1", "rpgxpbar", "<reset | disable>"),
                    LocaleLoader.getString("Commands.Usage.2", "rpgxpbar",
                            "<show | hide | disable>", "<skillname>")),
                    XPBarCommand::new),
            spec("rpginfo", "mcrpg.commands.info", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.1", "rpginfo",
                            "[" + LocaleLoader.getString("Commands.Usage.SubSkill") + "]")),
                    MmoInfoCommand::new),
            // No permission required on rpgdebug to save support headaches
            spec("rpgdebug", null, () -> List.of(
                    LocaleLoader.getString("Commands.Usage.0", "rpgdebug")),
                    PlayerDebugCommand::new),
            spec("rpgability", "mcrpg.commands.ability;mcrpg.commands.ability.others",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.1", "rpgability",
                            "[" + LocaleLoader.getString("Commands.Usage.Player") + "]")),
                    McabilityCommand::new),
            spec("rpggod", "mcrpg.commands.god;mcrpg.commands.god.others", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.1", "rpggod",
                            "[" + LocaleLoader.getString("Commands.Usage.Player") + "]")),
                    McgodCommand::new),
            // /mcchatspy only spies on party chat, which mcRPG doesn't have
            spec("mcrpg", "mcrpg.commands.mcrpg.description;mcrpg.commands.mcrpg.help",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.0", "mcrpg"),
                            LocaleLoader.getString("Commands.Usage.1", "mcrpg", "help")),
                    McmmoCommand::new),
            spec("rpgnotify", "mcrpg.commands.notify", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.0", "rpgnotify")),
                    McnotifyCommand::new),
            spec("rpglevelupsound", "mcrpg.commands.levelupsound", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.0", "rpglevelupsound")),
                    McLevelUpSoundCommand::new),
            spec("rpgrefresh", "mcrpg.commands.refresh;mcrpg.commands.refresh.others",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.1", "rpgrefresh",
                            "[" + LocaleLoader.getString("Commands.Usage.Player") + "]")),
                    McrefreshCommand::new),
            spec("rpgscoreboard",
                    () -> "Change the current mcRPG scoreboard being displayed", //TODO: Localize
                    "mcrpg.commands.scoreboard", () -> List.of(
                            LocaleLoader.getString("Commands.Usage.1", "rpgscoreboard",
                                    "<CLEAR | KEEP>"),
                            LocaleLoader.getString("Commands.Usage.2", "rpgscoreboard", "time",
                                    "<seconds>")),
                    McscoreboardCommand::new),
            // mcRPG has no XP events, so /xprate is not registered

            // Database Commands
            spec("rpgpurge", () -> LocaleLoader.getString("Commands.Description.rpgpurge",
                    mcMMO.p.getGeneralConfig().getOldUsersCutoff()), "mcrpg.commands.purge",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.0", "rpgpurge")),
                    McpurgeCommand::new),
            spec("rpgremove", "mcrpg.commands.remove", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.1", "rpgremove",
                            "<" + LocaleLoader.getString("Commands.Usage.Player") + ">")),
                    McremoveCommand::new),
            spec("rpgshowdb", "mcrpg.commands.showdb", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.0", "rpgshowdb")),
                    MmoshowdbCommand::new),
            spec("rpgconvert",
                    "mcrpg.commands.convert;mcrpg.commands.convert.experience;"
                            + "mcrpg.commands.convert.database",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.2", "rpgconvert",
                                    "database", "<flatfile|sql>"),
                            LocaleLoader.getString("Commands.Usage.2", "rpgconvert", "experience",
                                    "<linear|exponential>")),
                    McconvertCommand::new),

            // Experience Commands (mcRPG: the skill argument can also be a category)
            spec("rpgaddlevels", "mcrpg.commands.addlevels;mcrpg.commands.addlevels.others",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.3.XP", "rpgaddlevels",
                            "[" + LocaleLoader.getString("Commands.Usage.Player") + "]",
                            "<" + LocaleLoader.getString("mcRPG.Usage.SkillOrCategory") + ">",
                            "<" + LocaleLoader.getString("Commands.Usage.Level") + ">")),
                    AddlevelsCommand::new),
            spec("rpgaddxp", "mcrpg.commands.addxp;mcrpg.commands.addxp.others", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.3.XP", "rpgaddxp",
                            "[" + LocaleLoader.getString("Commands.Usage.Player") + "]",
                            "<" + LocaleLoader.getString("mcRPG.Usage.SkillOrCategory") + ">",
                            "<" + LocaleLoader.getString("Commands.Usage.XP") + ">")),
                    AddxpCommand::new),
            spec("rpgsetlevel", "mcrpg.commands.setlevel;mcrpg.commands.setlevel.others",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.3.XP", "rpgsetlevel",
                            "[" + LocaleLoader.getString("Commands.Usage.Player") + "]",
                            "<" + LocaleLoader.getString("mcRPG.Usage.SkillOrCategory") + ">",
                            "<" + LocaleLoader.getString("Commands.Usage.Level") + ">")),
                    MmoeditCommand::new),
            // Only the main permission nodes are needed here, not the per-skill ones
            spec("rpgskillreset", "mcrpg.commands.skillreset;mcrpg.commands.skillreset.others",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.2", "rpgskillreset",
                            "[" + LocaleLoader.getString("Commands.Usage.Player") + "]",
                            "<" + LocaleLoader.getString("mcRPG.Usage.SkillOrCategory") + ">")),
                    SkillresetCommand::new),

            // mcRPG has no party system, so /party and /ptp are not registered

            // mcRPG: Specialization commands (aliases such as /csp and /asp are in plugin.yml)
            spec("choosespecialization", "mcrpg.commands.choosespecialization",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.0",
                                    "choosespecialization"),
                            LocaleLoader.getString("Commands.Usage.2", "choosespecialization",
                                    "<primary|secondary>", "<category>")),
                    ChooseSpecializationCommand::new),
            spec("abandonspecialization", "mcrpg.commands.abandonspecialization",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.2",
                            "abandonspecialization", "<category|primary|secondary>",
                            "[confirm]")),
                    AbandonSpecializationCommand::new),
            spec("rpgsetspecialization", "mcrpg.commands.setspecialization",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.3",
                            "rpgsetspecialization",
                            "<" + LocaleLoader.getString("Commands.Usage.Player") + ">",
                            "<primary|secondary>", "<category|none>")),
                    SetSpecializationCommand::new),

            // Player Commands
            spec("rpginspect",
                    "mcrpg.commands.inspect;mcrpg.commands.inspect.far;"
                            + "mcrpg.commands.inspect.offline",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.1", "rpginspect",
                            "<" + LocaleLoader.getString("Commands.Usage.Player") + ">")),
                    InspectCommand::new),
            spec("rpgcooldowns", "mcrpg.commands.cooldowns", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.0", "rpgcooldowns")),
                    MccooldownCommand::new),
            spec("rpgrank",
                    "mcrpg.commands.rank;mcrpg.commands.rank.others;"
                            + "mcrpg.commands.rank.others.far;"
                            + "mcrpg.commands.rank.others.offline",
                    () -> List.of(LocaleLoader.getString("Commands.Usage.1", "rpgrank",
                            "[" + LocaleLoader.getString("Commands.Usage.Player") + "]")),
                    McRankCommand::new),
            spec("rpgstats", "mcrpg.commands.stats", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.0", "rpgstats")),
                    McstatsCommand::new),
            // Only the main rpgtop permission node is needed, not the per-skill ones
            spec("rpgtop", "mcrpg.commands.top", () -> List.of(
                    LocaleLoader.getString("Commands.Usage.2", "rpgtop",
                            "[" + LocaleLoader.getString("Commands.Usage.Skill") + "]",
                            "[" + LocaleLoader.getString("Commands.Usage.Page") + "]")),
                    McTopCommand::new),

            // Admin commands
            spec("rpgreloadlocale", () -> "Reloads locale", // TODO: Localize
                    "mcrpg.commands.reloadlocale", () -> List.of(
                            LocaleLoader.getString("Commands.Usage.0", "rpgreloadlocale")),
                    McmmoReloadLocaleCommand::new)
    );

    /**
     * Command names wired by the spec table. Package-private so the registration coverage test
     * can compare them against the plugin.yml declarations.
     */
    static @NotNull Set<String> specCommandNames() {
        return COMMAND_SPECS.stream().map(CommandSpec::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Command names wired by the skill command loop. Package-private for the registration
     * coverage test.
     */
    static @NotNull Set<String> skillCommandNames() {
        return Arrays.stream(PrimarySkillType.values())
                .map(skill -> skill.toString().toLowerCase(Locale.ENGLISH))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void applySpec(@NotNull CommandSpec spec, @NotNull String permissionMessage) {
        final PluginCommand command = mcMMO.p.getCommand(spec.name());
        if (command == null) {
            mcMMO.p.getLogger().severe("Command not found: " + spec.name());
            return;
        }

        command.setDescription(spec.description().get());
        command.setPermission(spec.permission());
        command.setPermissionMessage(permissionMessage);

        final List<String> usageLines = spec.usageLines().get();
        if (!usageLines.isEmpty()) {
            command.setUsage(String.join("\n", usageLines));
        }

        if (!spec.aliases().isEmpty()) {
            command.setAliases(spec.aliases());
        }

        command.setExecutor(spec.executor().get());
    }

    private static void registerSkillCommands(@NotNull String permissionMessage) {
        for (PrimarySkillType primarySkillType : PrimarySkillType.values()) {
            if (primarySkillType == PrimarySkillType.SPEARS
                    && !mcMMO.getMinecraftGameVersion().isAtLeast(1, 21, 11)) {
                continue;
            }

            final String commandName = primarySkillType.toString().toLowerCase(Locale.ENGLISH);
            final String localizedName = mcMMO.p.getSkillTools()
                    .getHeaderBannerSkillName(primarySkillType).toLowerCase(Locale.ENGLISH);

            final PluginCommand command = mcMMO.p.getCommand(commandName);
            if (command == null) {
                mcMMO.p.getLogger().severe("Command not found: " + commandName);
                continue;
            }

            command.setDescription(LocaleLoader.getString("Commands.Description.Skill",
                    StringUtils.getCapitalized(localizedName)));
            command.setPermission("mcrpg.commands." + commandName);
            command.setPermissionMessage(permissionMessage);
            command.setUsage(LocaleLoader.getString("Commands.Usage.0", commandName));
            command.setUsage(command.getUsage() + "\n" + LocaleLoader.getString("Commands.Usage.2",
                    commandName, "?", "[" + LocaleLoader.getString("Commands.Usage.Page") + "]"));

            switch (primarySkillType) {
                case ACROBATICS -> command.setExecutor(new AcrobaticsCommand());
                case ALCHEMY -> command.setExecutor(new AlchemyCommand());
                case ARCHERY -> command.setExecutor(new ArcheryCommand());
                case AXES -> command.setExecutor(new AxesCommand());
                case CROSSBOWS -> command.setExecutor(new CrossbowsCommand());
                case EXCAVATION -> command.setExecutor(new ExcavationCommand());
                case FISHING -> command.setExecutor(new FishingCommand());
                case HERBALISM -> command.setExecutor(new HerbalismCommand());
                case MACES -> command.setExecutor(new MacesCommand());
                case MINING -> command.setExecutor(new MiningCommand());
                case REPAIR -> command.setExecutor(new RepairCommand());
                case SALVAGE -> command.setExecutor(new SalvageCommand());
                case SMELTING -> command.setExecutor(new SmeltingCommand());
                case SPEARS -> command.setExecutor(new SpearsCommand());
                case SWORDS -> command.setExecutor(new SwordsCommand());
                case TAMING -> command.setExecutor(new TamingCommand());
                case TRIDENTS -> command.setExecutor(new TridentsCommand());
                case UNARMED -> command.setExecutor(new UnarmedCommand());
                case WOODCUTTING -> command.setExecutor(new WoodcuttingCommand());
                default -> throw new IllegalStateException("Unexpected value: " + primarySkillType);
            }
        }
    }

    public static void registerCommands() {
        final String permissionMessage = LocaleLoader.getString("mcMMO.NoPermission");
        final boolean partyEnabled = mcMMO.p.getPartyConfig().isPartyEnabled();

        for (CommandSpec spec : COMMAND_SPECS) {
            if (spec.requiresParty() && !partyEnabled) {
                continue;
            }
            applySpec(spec, permissionMessage);
        }

        // Skill Commands
        registerSkillCommands(permissionMessage);
    }
}
