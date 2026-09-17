package com.logistics.core.lib.power;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

/**
 * Whether an item burns as furnace fuel, and for how long.
 *
 * <p>Fuel is a plain item component ({@code minecraft:cooking_fuel}), so this is ordinary vanilla
 * API with no loader-specific behaviour behind it. The mod's own fuels declare the component at
 * item registration; see {@link FurnaceFuels}.
 */
public final class FuelHelper {

    private FuelHelper() {}

    /** Returns {@code true} if {@code stack} can be burned as fuel. */
    public static boolean isFuel(Level level, ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL);
    }

    /**
     * Returns the number of ticks {@code stack} burns for, or {@code 0} if it is not a fuel.
     *
     * <p>A burn time is a resolvable value rather than a plain number, and resolving one needs a
     * loot context, which only exists server-side. Every caller that asks for a duration — the
     * engines' burn loops — runs on the server; the client only ever asks {@link #isFuel}, which
     * needs no context.
     */
    public static int getBurnDuration(Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        LootParams params = new LootParams.Builder(serverLevel).create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params).create(Optional.empty());
        return ResolvableInt.getFromItem(
                stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
    }
}
