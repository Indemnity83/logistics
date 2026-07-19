package com.logistics.power.engine.fuel;

import com.logistics.LogisticsCore;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * The Fuel Engine's supported fuels and their properties, keyed by fluid <em>registry id</em>.
 *
 * <p>Keying by id (not by resolved {@link Fluid}) is deliberate: a static map of {@code Fluid}
 * objects built before the mod's custom fluids register would permanently cache {@code Fluids.EMPTY}.
 * The id map is registration-order-independent; the {@code Fluid} is resolved to its id only at
 * lookup time (always after registration).
 */
public final class FuelEngineFuels {

    private static final Map<Identifier, FuelEngineFuel> BY_ID = Map.of(
            LogisticsCore.resource("crude_oil").toIdentifier(), new FuelEngineFuel(40_000L, 3.0),
            LogisticsCore.resource("bio_fuel").toIdentifier(), new FuelEngineFuel(80_000L, 1.5),
            LogisticsCore.resource("fuel_oil").toIdentifier(), new FuelEngineFuel(150_000L, 2.25));

    private FuelEngineFuels() {}

    @Nullable
    public static FuelEngineFuel lookup(@Nullable Fluid fluid) {
        return fluid == null ? null : byId(BuiltInRegistries.FLUID.getKey(fluid));
    }

    /** Single source of truth shared with the fuel tank's insert filter. */
    public static boolean isFuel(@Nullable Fluid fluid) {
        return lookup(fluid) != null;
    }

    @Nullable
    static FuelEngineFuel byId(@Nullable Identifier id) {
        return id == null ? null : BY_ID.get(id);
    }
}
