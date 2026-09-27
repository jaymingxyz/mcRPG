package com.gmail.nossr50.database;

import static com.gmail.nossr50.database.FlatFileDatabaseManager.DATA_ENTRY_COUNT;
import static com.gmail.nossr50.database.FlatFileDatabaseManager.EXP_SALVAGE;
import static com.gmail.nossr50.database.FlatFileDatabaseManager.EXP_SMELTING;
import static com.gmail.nossr50.database.FlatFileDatabaseManager.SKILLS_SALVAGE;
import static com.gmail.nossr50.database.FlatFileDatabaseManager.SKILLS_SMELTING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.database.PlayerStat;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * mcRPG stores Salvage and Smelting like every other skill, with their own level and XP,
 * instead of deriving them from parent skills. These tests cover the FlatFile side: the new
 * fields are written, read back, ranked on leaderboards, and default correctly for rows that
 * don't have them yet.
 */
class McRPGStandaloneSkillStorageTest {
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final long PURGE_TIME = 2_630_000_000L;
    private static final UUID PLAYER_UUID =
            UUID.fromString("588fe472-1c82-4c4e-9aa1-7eefccb277e3");

    /** An mcMMO-format row: every field up to Spears (index 57), but no mcRPG fields. */
    private static final String ROW_WITHOUT_MCRPG_FIELDS =
            "nossr50:1:IGNORED:IGNORED:10:2:20:3:4:5:6:7:8:9:10:30:40:50:60:70:80:90:100:IGNORED"
                    + ":11:110:111:222:333:444:555:666:777:IGNORED:12:120:888:IGNORED:HEARTS:13"
                    + ":130:588fe472-1c82-4c4e-9aa1-7eefccb277e3:1111:999:2020:140:14:150:15"
                    + ":1111:2222:3333:160:16:4444:170:17:5555:";

    private static MockedStatic<ExperienceConfig> mockedExperienceConfig;

    @TempDir
    File tempDir;

    @BeforeAll
    static void setUpClass() throws Exception {
        mcMMO.p = mock(mcMMO.class);
        when(mcMMO.p.getLogger()).thenReturn(logger);
        when(mcMMO.p.getDataFolder())
                .thenReturn(Files.createTempDirectory("mcrpg-storage-test-").toFile());
        final Server server = mock(Server.class);
        when(mcMMO.p.getServer()).thenReturn(server);
        when(server.getPlayerExact(anyString())).thenReturn(null);

        final ExperienceConfig experienceConfig = mock(ExperienceConfig.class);
        mockedExperienceConfig = Mockito.mockStatic(ExperienceConfig.class);
        mockedExperienceConfig.when(ExperienceConfig::getInstance).thenReturn(experienceConfig);
    }

    @AfterAll
    static void tearDownClass() {
        mockedExperienceConfig.close();
        mcMMO.p = null;
    }

    private FlatFileDatabaseManager newDatabase() {
        return new FlatFileDatabaseManager(new File(tempDir, "mcrpg.users"), logger, PURGE_TIME,
                0, true);
    }

    private static PlayerProfile profileWithSalvageAndSmelting() {
        final PlayerProfile profile = new PlayerProfile("nossr50", PLAYER_UUID, 0);
        profile.modifySkill(PrimarySkillType.SALVAGE, 12);
        profile.setSkillXpLevel(PrimarySkillType.SALVAGE, 345F);
        profile.modifySkill(PrimarySkillType.SMELTING, 7);
        profile.setSkillXpLevel(PrimarySkillType.SMELTING, 89F);
        return profile;
    }

    @Test
    void salvageAndSmeltingShouldSurviveSaveAndReload() {
        // Given - a player with their own Salvage and Smelting progress
        final FlatFileDatabaseManager database = newDatabase();

        // When - the profile is saved and loaded back
        assertThat(database.saveUser(profileWithSalvageAndSmelting())).isTrue();
        final PlayerProfile loaded = database.loadPlayerProfile(PLAYER_UUID);

        // Then - both skills keep their own levels and XP, and their former parent skills
        // (Mining, Repair, Fishing) are untouched
        assertThat(loaded.isLoaded()).isTrue();
        assertThat(loaded.getSkillLevel(PrimarySkillType.SALVAGE)).isEqualTo(12);
        assertThat(loaded.getSkillXpLevel(PrimarySkillType.SALVAGE)).isEqualTo(345);
        assertThat(loaded.getSkillLevel(PrimarySkillType.SMELTING)).isEqualTo(7);
        assertThat(loaded.getSkillXpLevel(PrimarySkillType.SMELTING)).isEqualTo(89);
        assertThat(loaded.getSkillLevel(PrimarySkillType.MINING)).isZero();
        assertThat(loaded.getSkillLevel(PrimarySkillType.REPAIR)).isZero();
        assertThat(loaded.getSkillLevel(PrimarySkillType.FISHING)).isZero();
    }

    @Test
    void writtenRowShouldPutSalvageAndSmeltingInTheirFixedFields() throws Exception {
        // Given - the line FlatFile writes for a player
        final StringBuilder line = new StringBuilder();
        newDatabase().writeUserToLine(profileWithSalvageAndSmelting(), line);

        // When - it is split into its fields
        final String[] fields = line.toString().trim().split(":");

        // Then - it has every field, and mcRPG's fields sit at their permanent positions
        assertThat(fields).hasSize(DATA_ENTRY_COUNT);
        assertThat(fields[EXP_SALVAGE]).isEqualTo("345");
        assertThat(fields[SKILLS_SALVAGE]).isEqualTo("12");
        assertThat(fields[EXP_SMELTING]).isEqualTo("89");
        assertThat(fields[SKILLS_SMELTING]).isEqualTo("7");
    }

    @Test
    void mcRPGFieldPositionsShouldNeverChange() {
        // Changing these would make every existing mcRPG users file load the wrong values
        assertThat(EXP_SALVAGE).isEqualTo(58);
        assertThat(SKILLS_SALVAGE).isEqualTo(59);
        assertThat(EXP_SMELTING).isEqualTo(60);
        assertThat(SKILLS_SMELTING).isEqualTo(61);
    }

    @Test
    void rowWithoutMcRPGFieldsShouldLoadWithSalvageAndSmeltingAtZero() throws Exception {
        // Given - a users file whose row ends before the Salvage and Smelting fields
        final File usersFile = new File(tempDir, "mcrpg.users");
        Files.writeString(usersFile.toPath(), ROW_WITHOUT_MCRPG_FIELDS + System.lineSeparator());
        final FlatFileDatabaseManager database = new FlatFileDatabaseManager(usersFile, logger,
                PURGE_TIME, 0, true);

        // When - the player is loaded
        final PlayerProfile loaded = database.loadPlayerProfile(PLAYER_UUID);

        // Then - the existing skills load as written and the new skills start at zero
        assertThat(loaded.isLoaded()).isTrue();
        assertThat(loaded.getSkillLevel(PrimarySkillType.MINING)).isEqualTo(1);
        assertThat(loaded.getSkillLevel(PrimarySkillType.SPEARS)).isEqualTo(17);
        assertThat(loaded.getSkillLevel(PrimarySkillType.SALVAGE)).isZero();
        assertThat(loaded.getSkillLevel(PrimarySkillType.SMELTING)).isZero();
    }

    @Test
    void salvageAndSmeltingShouldHaveLeaderboardsAndCountTowardPowerLevel() throws Exception {
        // Given - a saved player whose only levels are in Salvage (12) and Smelting (7)
        final FlatFileDatabaseManager database = newDatabase();
        database.saveUser(profileWithSalvageAndSmelting());

        // When - the leaderboards are read
        final List<PlayerStat> salvageTop = database.readLeaderboard(PrimarySkillType.SALVAGE, 1,
                10);
        final List<PlayerStat> smeltingTop = database.readLeaderboard(PrimarySkillType.SMELTING,
                1, 10);
        final List<PlayerStat> powerLevelTop = database.readLeaderboard(null, 1, 10);

        // Then - each skill has its own ranking, and the power level includes both
        assertThat(salvageTop).containsExactly(new PlayerStat("nossr50", 12));
        assertThat(smeltingTop).containsExactly(new PlayerStat("nossr50", 7));
        assertThat(powerLevelTop).containsExactly(new PlayerStat("nossr50", 19));
    }
}
