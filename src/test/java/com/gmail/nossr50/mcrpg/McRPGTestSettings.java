package com.gmail.nossr50.mcrpg;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.gmail.nossr50.config.GeneralConfig;
import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.experience.FormulaType;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcrpg.passive.CategoryPassive;
import com.gmail.nossr50.mcrpg.specialization.SpecializationRole;
import org.mockito.MockedStatic;

/** Stubs mcRPG's default settings on the mocked configs of {@code MMOTestEnvironment}. */
public final class McRPGTestSettings {
    private McRPGTestSettings() {
    }

    /**
     * Default LINEAR curve in Standard mode, the default specialization multipliers
     * (1.25 / 1.0 / 0.35), 10% XP kept on abandon, a 30 second confirmation, no level caps,
     * and category passives on with their default values, unlocked at 25 combined levels.
     */
    public static void useDefaults(GeneralConfig generalConfig,
            MockedStatic<mcMMO> mockedMcMMO) {
        final ExperienceConfig experienceConfig = ExperienceConfig.getInstance();
        when(experienceConfig.getFormulaType()).thenReturn(FormulaType.LINEAR);
        when(experienceConfig.getBase(FormulaType.LINEAR)).thenReturn(1020);
        when(experienceConfig.getMultiplier(FormulaType.LINEAR)).thenReturn(20D);
        mockedMcMMO.when(mcMMO::isRetroModeEnabled).thenReturn(false);

        when(experienceConfig.getSpecializationMultiplier(SpecializationRole.PRIMARY))
                .thenReturn(1.25);
        when(experienceConfig.getSpecializationMultiplier(SpecializationRole.SECONDARY))
                .thenReturn(1.0);
        when(experienceConfig.getSpecializationMultiplier(SpecializationRole.UNSELECTED))
                .thenReturn(0.35);

        when(generalConfig.getAbandonXpKeptPercent()).thenReturn(10D);
        when(generalConfig.getAbandonConfirmTimeoutSeconds()).thenReturn(30);
        when(generalConfig.getSpecializationGuiConfirmation()).thenReturn(true);
        when(generalConfig.getSpecializationLoginReminder()).thenReturn(true);
        when(generalConfig.getLevelCap(any(PrimarySkillType.class)))
                .thenReturn(Integer.MAX_VALUE);

        when(generalConfig.getCategoryPassivesEnabled()).thenReturn(true);
        when(generalConfig.getCategoryPassiveUnlockLevel()).thenReturn(25);
        for (CategoryPassive passive : CategoryPassive.values()) {
            when(generalConfig.getCategoryPassiveBonusArmor(passive))
                    .thenReturn(passive.defaultBonusArmor());
            when(generalConfig.getCategoryPassiveBonusToughness(passive))
                    .thenReturn(passive.defaultBonusToughness());
            when(generalConfig.getCategoryPassiveMiningSpeedBonus(passive))
                    .thenReturn(passive.defaultMiningSpeedBonus());
            when(generalConfig.getCategoryPassiveDurabilityLossReduction(passive))
                    .thenReturn(passive.defaultDurabilityLossReduction());
        }
    }
}
