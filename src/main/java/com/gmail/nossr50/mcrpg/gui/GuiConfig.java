package com.gmail.nossr50.mcrpg.gui;

import com.gmail.nossr50.config.BukkitConfig;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SkillCategory;
import com.gmail.nossr50.util.text.StringUtils;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * gui.yml: icons used by the Specialization menu. Loaded once, the first time the menu opens.
 * Unknown materials, or materials that don't exist on this Minecraft version, fall back to the
 * built-in defaults and then to paper.
 */
public final class GuiConfig extends BukkitConfig {
    private static volatile GuiConfig instance;

    private static final Map<PrimarySkillType, String> DEFAULT_ICONS =
            new EnumMap<>(PrimarySkillType.class);

    static {
        DEFAULT_ICONS.put(PrimarySkillType.SWORDS, "IRON_SWORD");
        DEFAULT_ICONS.put(PrimarySkillType.AXES, "IRON_AXE");
        DEFAULT_ICONS.put(PrimarySkillType.UNARMED, "LEATHER");
        DEFAULT_ICONS.put(PrimarySkillType.MACES, "MACE");
        DEFAULT_ICONS.put(PrimarySkillType.SPEARS, "IRON_SPEAR");
        DEFAULT_ICONS.put(PrimarySkillType.ARCHERY, "BOW");
        DEFAULT_ICONS.put(PrimarySkillType.CROSSBOWS, "CROSSBOW");
        DEFAULT_ICONS.put(PrimarySkillType.TRIDENTS, "TRIDENT");
        DEFAULT_ICONS.put(PrimarySkillType.MINING, "IRON_PICKAXE");
        DEFAULT_ICONS.put(PrimarySkillType.WOODCUTTING, "OAK_LOG");
        DEFAULT_ICONS.put(PrimarySkillType.HERBALISM, "WHEAT");
        DEFAULT_ICONS.put(PrimarySkillType.EXCAVATION, "IRON_SHOVEL");
        DEFAULT_ICONS.put(PrimarySkillType.FISHING, "FISHING_ROD");
        DEFAULT_ICONS.put(PrimarySkillType.TAMING, "BONE");
        DEFAULT_ICONS.put(PrimarySkillType.REPAIR, "ANVIL");
        DEFAULT_ICONS.put(PrimarySkillType.ALCHEMY, "BREWING_STAND");
        DEFAULT_ICONS.put(PrimarySkillType.SALVAGE, "GOLD_BLOCK");
        DEFAULT_ICONS.put(PrimarySkillType.SMELTING, "FURNACE");
        DEFAULT_ICONS.put(PrimarySkillType.ACROBATICS, "FEATHER");
    }

    private static final Map<SkillCategory, String> DEFAULT_CATEGORY_ICONS = Map.of(
            SkillCategory.MELEE, "SHIELD",
            SkillCategory.RANGED, "ARROW",
            SkillCategory.METALLURGY, "IRON_INGOT",
            SkillCategory.BOTANY, "OAK_SAPLING",
            SkillCategory.BLACKSMITHING, "SMITHING_TABLE",
            SkillCategory.SURVIVALISM, "CAMPFIRE");

    private final Map<PrimarySkillType, Material> icons = new EnumMap<>(PrimarySkillType.class);
    private final Map<SkillCategory, Material> categoryIcons = new EnumMap<>(SkillCategory.class);

    private GuiConfig(@NotNull File dataFolder) {
        super("gui.yml", dataFolder);
        loadKeys();
    }

    public static @NotNull GuiConfig getInstance() {
        GuiConfig config = instance;
        if (config == null) {
            synchronized (GuiConfig.class) {
                config = instance;
                if (config == null) {
                    config = new GuiConfig(mcMMO.p.getDataFolder());
                    instance = config;
                }
            }
        }
        return config;
    }

    /** Forgets the loaded config so the next {@link #getInstance()} loads it again. */
    @VisibleForTesting
    static void resetForTests() {
        instance = null;
    }

    @Override
    protected void loadKeys() {
        icons.clear();
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            final String configured = config.getString(
                    "Skill_Selection.Icons." + StringUtils.getCapitalized(skill.toString()));
            icons.put(skill, resolveMaterial(configured, DEFAULT_ICONS.get(skill)));
        }
        categoryIcons.clear();
        for (SkillCategory category : SkillCategory.values()) {
            final String configured = config.getString(
                    "Skill_Selection.Category_Icons." + StringUtils.getCapitalized(category.name()));
            categoryIcons.put(category,
                    resolveMaterial(configured, DEFAULT_CATEGORY_ICONS.get(category)));
        }
    }

    /** The icon for a skill in the Specialization menu. */
    public @NotNull Material getIcon(@NotNull PrimarySkillType skill) {
        return icons.getOrDefault(skill, Material.PAPER);
    }

    /** The icon that labels a category's row in the Specialization menu. */
    public @NotNull Material getCategoryIcon(@NotNull SkillCategory category) {
        return categoryIcons.getOrDefault(category, Material.PAPER);
    }

    static @NotNull Material resolveMaterial(String configured, String fallback) {
        for (String name : new String[] {configured, fallback}) {
            if (name != null) {
                final Material material = Material.matchMaterial(name);
                if (material != null && material.isItem()) {
                    return material;
                }
            }
        }
        return Material.PAPER;
    }
}
