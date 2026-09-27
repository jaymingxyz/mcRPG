package com.gmail.nossr50.database;

import static com.gmail.nossr50.database.FlatFileDatabaseManager.DATA_ENTRY_COUNT;
import static com.gmail.nossr50.database.FlatFileDatabaseManager.PRIMARY_SKILL;
import static com.gmail.nossr50.database.FlatFileDatabaseManager.SECONDARY_SKILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.specialization.SpecializationSlot;
import java.io.File;
import java.nio.file.Files;
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
 * FlatFile storage of the Primary and Secondary slots (fields 62 and 63). Empty slots are
 * written as NONE, because mcMMO's row parser treats empty fields as damaged data.
 */
class McRPGSpecializationStorageTest {
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private static final long PURGE_TIME = 2_630_000_000L;
    private static final UUID PLAYER_UUID =
            UUID.fromString("588fe472-1c82-4c4e-9aa1-7eefccb277e3");

    private static MockedStatic<ExperienceConfig> mockedExperienceConfig;

    @TempDir
    File tempDir;

    @BeforeAll
    static void setUpClass() throws Exception {
        mcMMO.p = mock(mcMMO.class);
        when(mcMMO.p.getLogger()).thenReturn(logger);
        when(mcMMO.p.getDataFolder())
                .thenReturn(Files.createTempDirectory("mcrpg-slot-test-").toFile());
        final Server server = mock(Server.class);
        when(mcMMO.p.getServer()).thenReturn(server);
        when(server.getPlayerExact(anyString())).thenReturn(null);

        mockedExperienceConfig = Mockito.mockStatic(ExperienceConfig.class);
        mockedExperienceConfig.when(ExperienceConfig::getInstance)
                .thenReturn(mock(ExperienceConfig.class));
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

    @Test
    void slotsShouldSurviveSaveAndReload() {
        final FlatFileDatabaseManager database = newDatabase();
        final PlayerProfile profile = new PlayerProfile("nossr50", PLAYER_UUID, 0);
        profile.setSpecialization(SpecializationSlot.PRIMARY, PrimarySkillType.MINING);
        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.SMELTING);

        assertThat(database.saveUser(profile)).isTrue();
        final PlayerProfile loaded = database.loadPlayerProfile(PLAYER_UUID);

        assertThat(loaded.getSpecialization(SpecializationSlot.PRIMARY))
                .isEqualTo(PrimarySkillType.MINING);
        assertThat(loaded.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.SMELTING);
    }

    @Test
    void emptySlotsShouldBeWrittenAsNoneAndLoadAsEmpty() throws Exception {
        final FlatFileDatabaseManager database = newDatabase();
        final PlayerProfile profile = new PlayerProfile("nossr50", PLAYER_UUID, 0);
        profile.setSpecialization(SpecializationSlot.SECONDARY, PrimarySkillType.FISHING);

        final StringBuilder line = new StringBuilder();
        database.writeUserToLine(profile, line);
        final String[] fields = line.toString().trim().split(":");
        assertThat(fields).hasSize(DATA_ENTRY_COUNT);
        assertThat(fields[PRIMARY_SKILL]).isEqualTo("NONE");
        assertThat(fields[SECONDARY_SKILL]).isEqualTo("FISHING");

        database.saveUser(profile);
        final PlayerProfile loaded = database.loadPlayerProfile(PLAYER_UUID);
        assertThat(loaded.getSpecialization(SpecializationSlot.PRIMARY)).isNull();
        assertThat(loaded.getSpecialization(SpecializationSlot.SECONDARY))
                .isEqualTo(PrimarySkillType.FISHING);
    }

    @Test
    void slotFieldPositionsShouldNeverChange() {
        // Changing these would make every existing mcRPG users file load the wrong values
        assertThat(PRIMARY_SKILL).isEqualTo(62);
        assertThat(SECONDARY_SKILL).isEqualTo(63);
        assertThat(DATA_ENTRY_COUNT).isEqualTo(64);
    }

    @Test
    void missingOrUnknownSlotValuesShouldLoadAsEmpty() {
        final String[] shortRow = new String[PRIMARY_SKILL];
        assertThat(FlatFileDatabaseManager.specializationFromField(shortRow, PRIMARY_SKILL))
                .isNull();

        final String[] row = new String[DATA_ENTRY_COUNT];
        row[PRIMARY_SKILL] = "not_a_skill";
        row[SECONDARY_SKILL] = "mining";
        assertThat(FlatFileDatabaseManager.specializationFromField(row, PRIMARY_SKILL)).isNull();
        assertThat(FlatFileDatabaseManager.specializationFromField(row, SECONDARY_SKILL))
                .isEqualTo(PrimarySkillType.MINING);
    }
}
