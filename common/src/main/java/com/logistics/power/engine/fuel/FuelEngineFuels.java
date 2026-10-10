package com.logistics.power.engine.fuel;

import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * The Fuel Engine's fuels, keyed by the conventional {@code c:} fluid tags so any mod's fuel of a
 * tier burns like ours. Tiers are ordered best first; a fluid in several tags burns as the best.
 */
public final class FuelEngineFuels {

    private static final List<Tier> TIERS = List.of(
            new Tier(new FuelEngineFuel(150_000L, 2.25), List.of(fluidTag("fuel"), fluidTag("diesel"))),
            new Tier(new FuelEngineFuel(80_000L, 1.5), List.of(fluidTag("biofuel"))),
            new Tier(new FuelEngineFuel(40_000L, 3.0), List.of(fluidTag("crude_oil"), fluidTag("oil"))));

    private FuelEngineFuels() {}

    private record Tier(FuelEngineFuel fuel, List<TagKey<Fluid>> tags) {}

    private static TagKey<Fluid> fluidTag(String path) {
        return TagKey.create(Registries.FLUID, ResourceId.in("c", path).toIdentifier());
    }

    @Nullable
    public static FuelEngineFuel lookup(@Nullable Fluid fluid) {
        return fluid == null ? null : forTags(fluid.defaultFluidState()::is);
    }

    /** Single source of truth shared with the fuel tank's insert filter. */
    public static boolean isFuel(@Nullable Fluid fluid) {
        return lookup(fluid) != null;
    }

    /** The best tier with a tag the fluid is in, or null when it is in none. */
    @Nullable
    static FuelEngineFuel forTags(Predicate<TagKey<Fluid>> inTag) {
        for (Tier tier : TIERS) {
            if (tier.tags().stream().anyMatch(inTag)) {
                return tier.fuel();
            }
        }
        return null;
    }

    /** One burnable fluid and the tier it burns at. */
    public record Entry(Fluid fluid, FuelEngineFuel fuel) {}

    /** Every registered source fluid that burns, resolved against the currently bound tags. */
    public static List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (!fluid.defaultFluidState().isSource()) {
                continue;
            }
            FuelEngineFuel fuel = lookup(fluid);
            if (fuel != null) {
                entries.add(new Entry(fluid, fuel));
            }
        }
        return List.copyOf(entries);
    }
}
