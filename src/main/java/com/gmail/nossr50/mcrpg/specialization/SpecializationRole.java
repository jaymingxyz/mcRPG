package com.gmail.nossr50.mcrpg.specialization;

import org.jetbrains.annotations.NotNull;

/**
 * How a skill relates to a player's specialization, which decides the XP multiplier it earns.
 * The multipliers are set in experience.yml under {@code Specialization.Multipliers}.
 */
public enum SpecializationRole {
    PRIMARY("Primary", 1.25),
    SECONDARY("Secondary", 1.0),
    UNSELECTED("Unselected", 0.35);

    private final @NotNull String configKey;
    private final double defaultMultiplier;

    SpecializationRole(@NotNull String configKey, double defaultMultiplier) {
        this.configKey = configKey;
        this.defaultMultiplier = defaultMultiplier;
    }

    /** The key under {@code Specialization.Multipliers} in experience.yml. */
    public @NotNull String configKey() {
        return configKey;
    }

    public double defaultMultiplier() {
        return defaultMultiplier;
    }
}
