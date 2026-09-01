package com.logistics.power.engine.fuel;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * The Fuel Engine's supported fuels and their properties, keyed by fluid <em>registry id</em> string.
 *
 * <p>Keying by id (not by resolved {@link Fluid}) is deliberate: a static map of {@code Fluid} objects
 * built before the mod's custom fluids register would permanently cache {@code Fluids.EMPTY}. The id map
 * is registration-order-independent; the {@code Fluid} is resolved to its id only at lookup time (always
 * after registration).
 */
public final class FuelEngineFuels {

    private static final Map<String, FuelEngineFuel> BY_ID = Map.of(
            LogisticsCore.resource("crude_oil").toString(), new FuelEngineFuel(40_000L, 3.0),
            LogisticsCore.resource("bio_fuel").toString(), new FuelEngineFuel(80_000L, 1.5),
            LogisticsCore.resource("fuel_oil").toString(), new FuelEngineFuel(150_000L, 2.25));

    private FuelEngineFuels() {}

    @Nullable
    public static FuelEngineFuel lookup(@Nullable Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        var key = BuiltInRegistries.FLUID.getKey(fluid);
        return key == null ? null : byId(key.toString());
    }

    /** Single source of truth shared with the fuel tank's insert filter. */
    public static boolean isFuel(@Nullable Fluid fluid) {
        return lookup(fluid) != null;
    }

    @Nullable
    static FuelEngineFuel byId(@Nullable String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** One supported fuel, resolved to its registered {@link Fluid}. */
    public record Entry(Fluid fluid, FuelEngineFuel fuel) {}

    /** Every supported fuel, resolved at call time like {@link #lookup}; unregistered ids are skipped. */
    public static List<Entry> entries() {
        List<Entry> entries = new ArrayList<>(BY_ID.size());
        for (Map.Entry<String, FuelEngineFuel> entry : BY_ID.entrySet()) {
            ResourceId id = ResourceId.tryParse(entry.getKey());
            Fluid fluid = id == null ? null : BuiltInRegistries.FLUID.getValue(id.toIdentifier());
            if (fluid != null && fluid != Fluids.EMPTY) {
                entries.add(new Entry(fluid, entry.getValue()));
            }
        }
        return List.copyOf(entries);
    }
}
