package com.gmail.nossr50.util;

import com.gmail.nossr50.commands.party.PartySubcommandType;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.ItemType;
import com.gmail.nossr50.datatypes.skills.MaterialType;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.skills.RankUtils;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Permissions {
    private static final Map<PrimarySkillType, String> SKILL_ENABLED_NODES =
            perSkillNodes("mcrpg.skills.");
    private static final Map<PrimarySkillType, String> LUCKY_PERK_NODES =
            perSkillNodes("mcrpg.perks.lucky.");
    private static final Map<PrimarySkillType, String> XP_QUADRUPLE_NODES =
            perSkillNodes("mcrpg.perks.xp.quadruple.");
    private static final Map<PrimarySkillType, String> XP_TRIPLE_NODES =
            perSkillNodes("mcrpg.perks.xp.triple.");
    private static final Map<PrimarySkillType, String> XP_150_PERCENT_NODES =
            perSkillNodes("mcrpg.perks.xp.150percentboost.");
    private static final Map<PrimarySkillType, String> XP_DOUBLE_NODES =
            perSkillNodes("mcrpg.perks.xp.double.");
    private static final Map<PrimarySkillType, String> XP_50_PERCENT_NODES =
            perSkillNodes("mcrpg.perks.xp.50percentboost.");
    private static final Map<PrimarySkillType, String> XP_25_PERCENT_NODES =
            perSkillNodes("mcrpg.perks.xp.25percentboost.");
    private static final Map<PrimarySkillType, String> XP_10_PERCENT_NODES =
            perSkillNodes("mcrpg.perks.xp.10percentboost.");
    private static final Map<PrimarySkillType, String> XP_CUSTOM_BOOST_NODES =
            perSkillNodes("mcrpg.perks.xp.customboost.");
    private static final Map<PrimarySkillType, String> VANILLA_XP_BOOST_NODES =
            perSkillNodes("mcrpg.ability.", ".vanillaxpboost");
    private static final Map<ItemType, String> REPAIR_ITEM_TYPE_NODES = perEnumNodes(
            ItemType.class, type -> "mcrpg.ability.repair."
                    + type.toString().toLowerCase(Locale.ENGLISH) + "repair");
    private static final Map<MaterialType, String> REPAIR_MATERIAL_TYPE_NODES = perEnumNodes(
            MaterialType.class, type -> "mcrpg.ability.repair."
                    + type.toString().toLowerCase(Locale.ENGLISH) + "repair");
    private static final Map<ItemType, String> SALVAGE_ITEM_TYPE_NODES = perEnumNodes(
            ItemType.class, type -> "mcrpg.ability.salvage."
                    + type.toString().toLowerCase(Locale.ENGLISH) + "salvage");
    private static final Map<MaterialType, String> SALVAGE_MATERIAL_TYPE_NODES = perEnumNodes(
            MaterialType.class, type -> "mcrpg.ability.salvage."
                    + type.toString().toLowerCase(Locale.ENGLISH) + "salvage");
    private static final Map<EntityType, String> CALL_OF_THE_WILD_NODES = perEnumNodes(
            EntityType.class, type -> "mcrpg.ability.taming.callofthewild."
                    + type.toString().toLowerCase(Locale.ENGLISH));
    // Material is too large to precompute every node eagerly; these fill lazily and are read
    // from region threads on Folia
    private static final Map<Material, String> GREEN_THUMB_BLOCK_NODES =
            new ConcurrentHashMap<>();
    private static final Map<Material, String> GREEN_THUMB_PLANT_NODES =
            new ConcurrentHashMap<>();

    private Permissions() {
    }

    private static Map<PrimarySkillType, String> perSkillNodes(String prefix) {
        return perSkillNodes(prefix, "");
    }

    private static Map<PrimarySkillType, String> perSkillNodes(String prefix, String suffix) {
        final Map<PrimarySkillType, String> nodes = new EnumMap<>(PrimarySkillType.class);

        for (PrimarySkillType skill : PrimarySkillType.values()) {
            nodes.put(skill, prefix + skill.toString().toLowerCase(Locale.ENGLISH) + suffix);
        }

        return nodes;
    }

    private static <T extends Enum<T>> Map<T, String> perEnumNodes(Class<T> enumClass,
            Function<T, String> nodeBuilder) {
        final Map<T, String> nodes = new EnumMap<>(enumClass);

        for (T constant : enumClass.getEnumConstants()) {
            nodes.put(constant, nodeBuilder.apply(constant));
        }

        return nodes;
    }

    /*
     * GENERAL
     */
    public static boolean motd(Permissible permissible) {
        return permissible.hasPermission("mcrpg.motd");
    }

    public static boolean levelUpBroadcast(Permissible permissible) {
        return permissible.hasPermission("mcrpg.broadcast.levelup");
    }

    public static boolean updateNotifications(Permissible permissible) {
        return permissible.hasPermission("mcrpg.tools.updatecheck");
    }

    public static boolean chimaeraWing(Permissible permissible) {
        return permissible.hasPermission("mcrpg.item.chimaerawing");
    }

    public static boolean showversion(Permissible permissible) {
        return permissible.hasPermission("mcrpg.showversion");
    }

    /* BYPASS */
    public static boolean hardcoreBypass(Permissible permissible) {
        return permissible.hasPermission("mcrpg.bypass.hardcoremode");
    }

    public static boolean arcaneBypass(Permissible permissible) {
        return permissible.hasPermission("mcrpg.bypass.arcanebypass");
    }

    /* CHAT */
    public static boolean partyChat(Permissible permissible) {
        return permissible.hasPermission("mcrpg.chat.partychat");
    }

    public static boolean adminChat(Permissible permissible) {
        return permissible.hasPermission("mcrpg.chat.adminchat");
    }

    public static boolean colorChat(Permissible permissible) {
        return permissible.hasPermission("mcrpg.chat.colors");
    }

    /*
     * COMMANDS
     */

    public static boolean mmoinfo(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.info");
    }

    public static boolean addlevels(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.addlevels");
    }

    public static boolean addlevelsOthers(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.addlevels.others");
    }

    public static boolean addxp(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.addxp");
    }

    public static boolean addxpOthers(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.addxp.others");
    }

    public static boolean hardcoreModify(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.hardcore.modify");
    }

    public static boolean hardcoreToggle(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.hardcore.toggle");
    }

    public static boolean inspect(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.inspect"));
    }

    public static boolean inspectFar(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.inspect.far"));
    }

    public static boolean inspectHidden(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.inspect.hidden"));
    }

    public static boolean mcability(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.ability"));
    }

    public static boolean mcabilityOthers(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.ability.others"));
    }

    public static boolean adminChatSpy(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.mcchatspy");
    }

    public static boolean adminChatSpyOthers(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.mcchatspy.others");
    }

    public static boolean mcgod(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.god");
    }

    public static boolean mcgodOthers(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.god.others");
    }

    public static boolean mcmmoDescription(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.mcrpg.description");
    }

    public static boolean mcmmoHelp(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.mcrpg.help");
    }

    public static boolean mcrank(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.rank"));
    }

    public static boolean mcrankOthers(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.rank.others"));
    }

    public static boolean mcrankFar(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.rank.others.far"));
    }

    public static boolean mcrankOffline(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.rank.others.offline"));
    }

    public static boolean mcrefresh(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.refresh"));
    }

    public static boolean mcrefreshOthers(Permissible permissible) {
        return (permissible.hasPermission("mcrpg.commands.refresh.others"));
    }

    public static boolean mctop(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission(
                "mcrpg.commands.top." + skill.toString().toLowerCase(Locale.ENGLISH));
    }

    public static boolean mmoedit(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.setlevel");
    }

    public static boolean mmoeditOthers(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.setlevel.others");
    }

    public static boolean skillreset(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.skillreset");
    }

    public static boolean skillreset(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission(
                "mcrpg.commands.skillreset." + skill.toString().toLowerCase(Locale.ENGLISH));
    }

    public static boolean skillresetOthers(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.skillreset.others");
    }

    public static boolean skillresetOthers(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission(
                "mcrpg.commands.skillreset.others." + skill.toString().toLowerCase(Locale.ENGLISH));
    }

    public static boolean xplock(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission(
                "mcrpg.commands.xplock." + skill.toString().toLowerCase(Locale.ENGLISH));
    }

    public static boolean xprateSet(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.xprate.set");
    }

    public static boolean xprateReset(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.xprate.reset");
    }

    public static boolean xprateShow(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.xprate.show");
    }

    public static boolean mcpurge(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.purge");
    }

    public static boolean mcremove(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.remove");
    }

    public static boolean mmoupdate(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.mmoupdate");
    }

    public static boolean reloadlocale(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.reloadlocale");
    }

    /*
     * PERKS
     */

    /* BYPASS PERKS */

    public static boolean hasRepairEnchantBypassPerk(Permissible permissible) {
        return permissible.hasPermission("mcrpg.perks.bypass.repairenchant");
    }

    public static boolean hasSalvageEnchantBypassPerk(Permissible permissible) {
        return permissible.hasPermission("mcrpg.perks.bypass.salvageenchant");
    }

    public static boolean lucky(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission(LUCKY_PERK_NODES.get(skill));
    }

    /* XP PERKS */
    public static boolean quadrupleXp(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission("mcrpg.perks.xp.quadruple.all")
                || permissible.hasPermission(XP_QUADRUPLE_NODES.get(skill));
    }

    public static boolean tripleXp(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission("mcrpg.perks.xp.triple.all")
                || permissible.hasPermission(XP_TRIPLE_NODES.get(skill));
    }

    public static boolean doubleAndOneHalfXp(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission("mcrpg.perks.xp.150percentboost.all")
                || permissible.hasPermission(XP_150_PERCENT_NODES.get(skill));
    }

    public static boolean doubleXp(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission("mcrpg.perks.xp.double.all")
                || permissible.hasPermission(XP_DOUBLE_NODES.get(skill));
    }

    public static boolean oneAndOneHalfXp(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission("mcrpg.perks.xp.50percentboost.all")
                || permissible.hasPermission(XP_50_PERCENT_NODES.get(skill));
    }

    public static boolean oneAndAQuarterXp(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission("mcrpg.perks.xp.25percentboost.all")
                || permissible.hasPermission(XP_25_PERCENT_NODES.get(skill));
    }

    public static boolean oneAndOneTenthXp(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission("mcrpg.perks.xp.10percentboost.all")
                || permissible.hasPermission(XP_10_PERCENT_NODES.get(skill));
    }

    public static boolean customXpBoost(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission("mcrpg.perks.xp.customboost.all")
                || permissible.hasPermission(XP_CUSTOM_BOOST_NODES.get(skill));
    }


    /* ACTIVATION PERKS */
    public static boolean twelveSecondActivationBoost(Permissible permissible) {
        return permissible.hasPermission("mcrpg.perks.activationtime.twelveseconds");
    }

    public static boolean eightSecondActivationBoost(Permissible permissible) {
        return permissible.hasPermission("mcrpg.perks.activationtime.eightseconds");
    }

    public static boolean fourSecondActivationBoost(Permissible permissible) {
        return permissible.hasPermission("mcrpg.perks.activationtime.fourseconds");
    }

    /* COOLDOWN PERKS */
    public static boolean halvedCooldowns(Permissible permissible) {
        return permissible.hasPermission("mcrpg.perks.cooldowns.halved");
    }

    public static boolean thirdedCooldowns(Permissible permissible) {
        return permissible.hasPermission("mcrpg.perks.cooldowns.thirded");
    }

    public static boolean quarteredCooldowns(Permissible permissible) {
        return permissible.hasPermission("mcrpg.perks.cooldowns.quartered");
    }

    /*
     * SKILLS
     */

    public static boolean skillEnabled(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission(SKILL_ENABLED_NODES.get(skill));
    }

    public static boolean vanillaXpBoost(Permissible permissible, PrimarySkillType skill) {
        return permissible.hasPermission(VANILLA_XP_BOOST_NODES.get(skill));
    }

    public static boolean isSubSkillEnabled(@Nullable Permissible permissible,
            @NotNull SubSkillType subSkillType) {
        if (permissible == null) {
            return false;
        }
        return permissible.hasPermission(subSkillType.getPermissionNodeAddress());
    }

    public static boolean isSubSkillEnabled(@Nullable McMMOPlayer permissible,
            @NotNull SubSkillType subSkillType) {
        if (permissible == null) {
            return false;
        }

        return isSubSkillEnabled(permissible.getPlayer(), subSkillType);
    }

    /* ACROBATICS */
    public static boolean dodge(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.acrobatics.dodge");
    }

    public static boolean gracefulRoll(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.acrobatics.gracefulroll");
    }

    public static boolean roll(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.acrobatics.roll");
    }

    /* ALCHEMY */
    public static boolean catalysis(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.alchemy.catalysis");
    }

    public static boolean concoctions(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.alchemy.concoctions");
    }

    /* ARCHERY */
    public static boolean arrowRetrieval(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.archery.trackarrows");
    }

    public static boolean daze(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.archery.daze");
    }

    /* AXES */
    public static boolean skullSplitter(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.axes.skullsplitter");
    }

    /* EXCAVATION */
    public static boolean gigaDrillBreaker(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.excavation.gigadrillbreaker");
    }

    /* HERBALISM */
    public static boolean greenTerra(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.herbalism.greenterra");
    }

    public static boolean greenThumbBlock(Permissible permissible, Material material) {
        return permissible.hasPermission(GREEN_THUMB_BLOCK_NODES.computeIfAbsent(material,
                mat -> "mcrpg.ability.herbalism.greenthumb.blocks."
                        + mat.toString().replace("_", "").toLowerCase(Locale.ENGLISH)));
    }

    public static boolean greenThumbPlant(Permissible permissible, Material material) {
        return permissible.hasPermission(GREEN_THUMB_PLANT_NODES.computeIfAbsent(material,
                mat -> "mcrpg.ability.herbalism.greenthumb.plants."
                        + mat.toString().replace("_", "").toLowerCase(Locale.ENGLISH)));
    }

    /* MINING */
    public static boolean biggerBombs(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.mining.blastmining.biggerbombs");
    }

    public static boolean demolitionsExpertise(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.mining.blastmining.demolitionsexpertise");
    }

    public static boolean remoteDetonation(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.mining.blastmining.detonate");
    }

    public static boolean superBreaker(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.mining.superbreaker");
    }

    /* REPAIR */
    public static boolean repairItemType(Permissible permissible, ItemType repairItemType) {
        return permissible.hasPermission(REPAIR_ITEM_TYPE_NODES.get(repairItemType));
    }

    public static boolean repairMaterialType(Permissible permissible,
            MaterialType repairMaterialType) {
        return permissible.hasPermission(REPAIR_MATERIAL_TYPE_NODES.get(repairMaterialType));
    }

    /* SALVAGE */
    public static boolean arcaneSalvage(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.salvage.arcanesalvage");
    }

    public static boolean salvageItemType(Permissible permissible, ItemType salvageItemType) {
        return permissible.hasPermission(SALVAGE_ITEM_TYPE_NODES.get(salvageItemType));
    }

    public static boolean salvageMaterialType(Permissible permissible,
            MaterialType salvageMaterialType) {
        return permissible.hasPermission(SALVAGE_MATERIAL_TYPE_NODES.get(salvageMaterialType));
    }

    /* SMELTING */
    public static boolean fluxMining(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.smelting.fluxmining");
    }

    public static boolean fuelEfficiency(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.smelting.fuelefficiency");
    }

    /* SWORDS */
    public static boolean serratedStrikes(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.swords.serratedstrikes");
    }

    /* TAMING */
    public static boolean callOfTheWild(Permissible permissible, EntityType type) {
        return permissible.hasPermission(CALL_OF_THE_WILD_NODES.get(type));
    }

    /* UNARMED */
    public static boolean berserk(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.unarmed.berserk");
    }

    /* WOODCUTTING */
    public static boolean treeFeller(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.woodcutting.treefeller");
    }

    /* CROSSBOWS */
    public static boolean trickShot(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.crossbows.trickshot");
    }

    public static boolean poweredShot(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.crossbows.poweredshot");
    }

    /* TRIDENTS */
    public static boolean tridentsLimitBreak(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.tridents.tridentslimitbreak");
    }

    /* MACES */
    public static boolean macesLimitBreak(Permissible permissible) {
        return permissible.hasPermission("mcrpg.ability.maces.maceslimitbreak");
    }

    /*
     * PARTY
     */
    public static boolean partySizeBypass(Permissible permissible) {
        return permissible.hasPermission("mcrpg.bypass.partylimit");
    }

    public static boolean party(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.party");
    }

    public static boolean partySubcommand(Permissible permissible, PartySubcommandType subcommand) {
        return permissible.hasPermission(
                "mcrpg.commands.party." + subcommand.toString().toLowerCase(Locale.ENGLISH));
    }

    public static boolean friendlyFire(Permissible permissible) {
        return permissible.hasPermission("mcrpg.party.friendlyfire");
    }

    /* TELEPORT */
    public static boolean partyTeleportSend(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.ptp.send");
    }

    public static boolean partyTeleportAccept(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.ptp.accept");
    }

    public static boolean partyTeleportAcceptAll(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.ptp.acceptall");
    }

    public static boolean partyTeleportToggle(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.ptp.toggle");
    }

    public static boolean partyTeleportAllWorlds(Permissible permissible) {
        return permissible.hasPermission("mcrpg.commands.ptp.world.all");
    }

    public static boolean partyTeleportWorld(Permissible permissible, World world) {
        return permissible.hasPermission("mcrpg.commands.ptp.world." + world.getName());
    }

    public static void generateWorldTeleportPermissions() {
        Server server = mcMMO.p.getServer();
        PluginManager pluginManager = server.getPluginManager();

        for (World world : server.getWorlds()) {
            addDynamicPermission("mcrpg.commands.ptp.world." + world.getName(),
                    PermissionDefault.OP, pluginManager);
        }
    }

    private static void addDynamicPermission(String permissionName,
            PermissionDefault permissionDefault, PluginManager pluginManager) {
        Permission permission = new Permission(permissionName);
        permission.setDefault(permissionDefault);
        pluginManager.addPermission(permission);
    }

    /**
     * Checks if a player can use a skill
     *
     * @param player target player
     * @param subSkillType target subskill
     * @return true if the player has permission and has the skill unlocked
     */
    public static boolean canUseSubSkill(@NotNull Player player,
            @NotNull SubSkillType subSkillType) {
        return isSubSkillEnabled(player, subSkillType) && RankUtils.hasUnlockedSubskill(player,
                subSkillType);
    }
}
