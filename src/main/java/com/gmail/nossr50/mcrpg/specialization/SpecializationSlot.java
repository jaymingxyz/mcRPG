package com.gmail.nossr50.mcrpg.specialization;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** The two specialization slots every player has. */
public enum SpecializationSlot {
    PRIMARY,
    SECONDARY;

    public @NotNull SpecializationSlot other() {
        return this == PRIMARY ? SECONDARY : PRIMARY;
    }

    /** The role a skill in this slot has, which decides its XP multiplier. */
    public @NotNull SpecializationRole role() {
        return this == PRIMARY ? SpecializationRole.PRIMARY : SpecializationRole.SECONDARY;
    }

    /** The name used in commands, e.g. {@code primary}. */
    public @NotNull String commandName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses {@code primary} or {@code secondary}, ignoring case. */
    public static @Nullable SpecializationSlot fromCommandName(@NotNull String name) {
        for (SpecializationSlot slot : values()) {
            if (slot.commandName().equalsIgnoreCase(name)) {
                return slot;
            }
        }
        return null;
    }
}
