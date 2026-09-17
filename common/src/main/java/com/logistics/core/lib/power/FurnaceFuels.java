package com.logistics.core.lib.power;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

/**
 * Burn times, in ticks, for the mod's items that work as furnace fuel.
 *
 * <p>Fuel is an item component, so a fuel declares itself at registration through
 * {@link #applyTo} and both loaders pick it up with no wiring of their own. Coal is 1600
 * for reference.
 *
 * <p>Keys are item paths within the mod's namespace.
 */
public final class FurnaceFuels {

    /** Item path to burn time in ticks. */
    public static final Map<String, Integer> BURN_TIMES = Map.of(
            "core/peat", 2000,
            "core/bitumen", 3200,
            "core/tar", 800,
            "core/sawdust", 100);

    private FurnaceFuels() {}

    /**
     * Adds the cooking-fuel component to {@code properties} when {@code itemPath} names one of the
     * fuels above, and returns the properties either way so it can sit inline in a registration.
     */
    public static Item.Properties applyTo(Item.Properties properties, String itemPath) {
        Integer burnTicks = BURN_TIMES.get(itemPath);
        if (burnTicks == null) {
            return properties;
        }
        return properties.component(
                DataComponents.COOKING_FUEL,
                new CookingFuel(
                        new ResolvableInt.Constant(burnTicks),
                        ResolvableFloat.fromKey(ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)));
    }
}
