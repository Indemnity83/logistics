package com.logistics.core.machine.tier;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Machine tiers, lowest first. Persisted only by {@link #id()}; ordinals are never saved, so constants
 * may be reordered or inserted without breaking worlds.
 */
public enum MachineTier implements StringRepresentable {
    BASIC("basic", 0),
    INDUSTRIAL("industrial", 1),
    PRECISION("precision", 2),
    INFERNAL("infernal", 3);

    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/machine_tier");

    /** Strict codec: an unknown id is a decode error. */
    public static final Codec<MachineTier> CODEC = StringRepresentable.fromEnum(MachineTier::values);

    /** Same string encoding as {@link #CODEC}, but an unknown id decodes to {@link #BASIC} instead of failing. */
    public static final Codec<MachineTier> LENIENT_CODEC =
            Codec.STRING.xmap(MachineTier::byIdOrBasic, MachineTier::id);

    private static final MachineTier[] VALUES = values();

    private final String id;
    private final int rank;

    MachineTier(String id, int rank) {
        this.id = id;
        this.rank = rank;
    }

    /** Stable serialized id. */
    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Position on the ladder, {@code 0} for {@link #BASIC}. */
    public int rank() {
        return rank;
    }

    public String translationKey() {
        return "tier.logistics." + id;
    }

    /** The tier one rank above, or empty at the top. */
    public Optional<MachineTier> next() {
        return byRank(rank + 1);
    }

    /** The tier one rank below, or empty at the bottom. */
    public Optional<MachineTier> previous() {
        return byRank(rank - 1);
    }

    public static Optional<MachineTier> byId(String id) {
        for (MachineTier tier : VALUES) {
            if (tier.id.equals(id)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }

    /**
     * Load-time decode that never throws. A missing or empty id is {@link #BASIC} (the state of a
     * machine saved before it had a tier); an unknown id is also {@link #BASIC}, with a warning.
     */
    public static MachineTier byIdOrBasic(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return BASIC;
        }
        return byId(id).orElseGet(() -> {
            LOGGER.warn("Unknown machine tier '{}'; using '{}'", id, BASIC.id);
            return BASIC;
        });
    }

    public static Optional<MachineTier> byRank(int rank) {
        for (MachineTier tier : VALUES) {
            if (tier.rank == rank) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }
}
