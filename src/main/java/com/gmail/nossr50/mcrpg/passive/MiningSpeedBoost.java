package com.gmail.nossr50.mcrpg.passive;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.AttributeMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Wooden Mastery's faster mining: a block break speed modifier on the player while they hold
 * a wooden tool. Minecraft saves attribute modifiers with the player, so the modifier is
 * removed when they leave or mcRPG shuts down, and one left behind by a crash is removed when
 * they join. The modifier's ID never changes, so an old one can always be found.
 */
public final class MiningSpeedBoost {
    static final UUID MODIFIER_ID = UUID.fromString("5f0e2c8a-6d3b-4c1e-9a7f-2b8d4e6c1a93");
    private static final String MODIFIER_NAME = "mcrpg_wooden_mastery";

    /** The bonus applied to each online player who has one. Region threads share it on Folia. */
    private static final Map<UUID, Double> APPLIED = new ConcurrentHashMap<>();

    /** Null in tests and on servers without the attribute, which turns the boost off. */
    @VisibleForTesting
    static @Nullable Attribute speedAttribute = AttributeMapper.MAPPED_BLOCK_BREAK_SPEED;

    private static volatile boolean unsupported;

    private MiningSpeedBoost() {
    }

    /**
     * Gives the player this mining speed bonus (0.5 is 50% faster), replacing any they have.
     * A bonus of 0 removes it.
     */
    static void set(@NotNull Player player, double bonus) {
        final UUID playerId = player.getUniqueId();
        final Double current = APPLIED.get(playerId);
        if (current == null ? bonus <= 0 : current == bonus) {
            return;
        }
        final AttributeInstance speed = attribute(player);
        if (speed == null) {
            return;
        }
        try {
            speed.removeModifier(modifier(0));
            if (bonus > 0) {
                speed.addModifier(modifier(bonus));
                APPLIED.put(playerId, bonus);
            } else {
                APPLIED.remove(playerId);
            }
        } catch (LinkageError error) {
            // A server version without this attribute modifier API
            unsupported = true;
            mcMMO.p.getLogger().warning("Wooden Mastery can't change mining speed on this "
                    + "server version, so it only reduces durability loss: " + error);
        }
    }

    /** Removes the player's bonus, including one saved by an earlier session. */
    static void remove(@NotNull Player player) {
        APPLIED.remove(player.getUniqueId());
        final AttributeInstance speed = attribute(player);
        if (speed != null) {
            try {
                speed.removeModifier(modifier(0));
            } catch (LinkageError error) {
                unsupported = true;
            }
        }
    }

    /** Removes every online player's bonus. Called when mcRPG shuts down. */
    public static void removeAll() {
        for (UUID playerId : APPLIED.keySet()) {
            final Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                APPLIED.remove(playerId);
                continue;
            }
            try {
                remove(player);
            } catch (RuntimeException e) {
                // Folia may refuse changes off the player's thread; their next join cleans up
                mcMMO.p.getLogger().warning("Couldn't remove Wooden Mastery's mining speed from "
                        + player.getName() + ": " + e.getMessage());
            }
        }
        APPLIED.clear();
    }

    /** The bonus applied to the player now, or 0. */
    @VisibleForTesting
    static double applied(@NotNull Player player) {
        return APPLIED.getOrDefault(player.getUniqueId(), 0D);
    }

    private static @Nullable AttributeInstance attribute(@NotNull Player player) {
        final Attribute type = speedAttribute;
        return type == null || unsupported ? null : player.getAttribute(type);
    }

    @SuppressWarnings("deprecation") // The key-based constructor doesn't exist before 1.21
    private static @NotNull AttributeModifier modifier(double bonus) {
        return new AttributeModifier(MODIFIER_ID, MODIFIER_NAME, bonus,
                AttributeModifier.Operation.ADD_SCALAR);
    }
}
