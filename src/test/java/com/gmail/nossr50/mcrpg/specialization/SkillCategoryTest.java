package com.gmail.nossr50.mcrpg.specialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * mcRPG's six skill categories (each Specialization is one of them), how they're named in
 * commands and storage, and the text the menu and /rpgstats show for them.
 */
class SkillCategoryTest {
    @Test
    void commandNamesShouldBeTheLowercaseCategory() {
        assertThat(SkillCategory.METALLURGY.commandName()).isEqualTo("metallurgy");
        for (SkillCategory category : SkillCategory.values()) {
            assertThat(SkillCategory.fromCommandName(category.commandName())).isEqualTo(category);
            assertThat(SkillCategory.fromCommandName(category.name())).isEqualTo(category);
        }
        assertThat(SkillCategory.fromCommandName("mining")).isNull();
        assertThat(SkillCategory.fromCommandName("gathering")).isNull();
    }

    @Test
    void storedCategoryNamesShouldReadBack() {
        for (SkillCategory category : SkillCategory.values()) {
            assertThat(SkillCategory.fromStoredName(category.name())).isEqualTo(category);
        }
        assertThat(SkillCategory.fromStoredName(" botany ")).isEqualTo(SkillCategory.BOTANY);
    }

    @Test
    void storedSkillNamesFromOlderBuildsShouldReadAsTheirCategory() {
        // Before 2026-09-27 a Specialization was one skill
        assertThat(SkillCategory.fromStoredName("MINING")).isEqualTo(SkillCategory.METALLURGY);
        assertThat(SkillCategory.fromStoredName("UNARMED"))
                .isEqualTo(SkillCategory.SURVIVALISM);
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            assertThat(SkillCategory.fromStoredName(skill.name()))
                    .isEqualTo(SkillCategory.of(skill));
        }
    }

    @Test
    void emptyOrUnknownStoredNamesShouldMeanNotChosen() {
        assertThat(SkillCategory.fromStoredName(null)).isNull();
        assertThat(SkillCategory.fromStoredName("")).isNull();
        assertThat(SkillCategory.fromStoredName("NONE")).isNull();
        assertThat(SkillCategory.fromStoredName("GATHERING")).isNull();
    }

    @Test
    void categoriesShouldHoldTheirSkillsInDisplayOrder() {
        assertThat(SkillCategory.MELEE.skills()).containsExactly(PrimarySkillType.SWORDS,
                PrimarySkillType.AXES, PrimarySkillType.MACES, PrimarySkillType.SPEARS);
        assertThat(SkillCategory.RANGED.skills()).containsExactly(PrimarySkillType.ARCHERY,
                PrimarySkillType.CROSSBOWS, PrimarySkillType.TRIDENTS);
        assertThat(SkillCategory.METALLURGY.skills()).containsExactly(PrimarySkillType.MINING,
                PrimarySkillType.SMELTING, PrimarySkillType.EXCAVATION);
        assertThat(SkillCategory.BOTANY.skills()).containsExactly(PrimarySkillType.WOODCUTTING,
                PrimarySkillType.HERBALISM, PrimarySkillType.ALCHEMY);
        assertThat(SkillCategory.BLACKSMITHING.skills()).containsExactly(PrimarySkillType.REPAIR,
                PrimarySkillType.SALVAGE);
        assertThat(SkillCategory.SURVIVALISM.skills()).containsExactly(PrimarySkillType.TAMING,
                PrimarySkillType.ACROBATICS, PrimarySkillType.FISHING, PrimarySkillType.UNARMED);
    }

    @Test
    void everySkillShouldBelongToExactlyOneCategory() {
        // A skill mcMMO adds in an update must be given a category before this passes
        final long placed = Arrays.stream(SkillCategory.values())
                .mapToLong(category -> category.skills().size()).sum();

        assertThat(placed).isEqualTo(PrimarySkillType.values().length);
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            assertThat(SkillCategory.of(skill).skills()).contains(skill);
        }
    }

    @Test
    void everyCategoryShouldFitInItsMenuRow() {
        for (SkillCategory category : SkillCategory.values()) {
            assertThat(category.skills().size()).as("%s", category)
                    .isLessThanOrEqualTo(SkillCategory.MAX_SKILLS_PER_CATEGORY);
        }
    }

    @Test
    void everyCategoryShouldHaveEnglishText() throws Exception {
        final Properties english = new Properties();
        try (InputStream in = SkillCategoryTest.class.getClassLoader().getResourceAsStream(
                "com/gmail/nossr50/locale/locale_en_US.properties")) {
            assertThat(in).isNotNull();
            english.load(in);
        }

        assertThat(english.getProperty(SkillCategory.METALLURGY.localeKey()))
                .isEqualTo("&6-=METALLURGY=-");
        assertThat(english.getProperty(SkillCategory.MELEE.nameKey())).isEqualTo("Melee Combat");
        for (SkillCategory category : SkillCategory.values()) {
            assertThat(english.getProperty(category.localeKey())).as(category.localeKey())
                    .isNotBlank();
            assertThat(english.getProperty(category.nameKey())).as(category.nameKey())
                    .isNotBlank();
        }
    }
}
