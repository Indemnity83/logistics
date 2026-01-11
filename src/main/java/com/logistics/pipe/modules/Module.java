package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.runtime.RoutePlan;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.List;

public interface Module {
    default void onTick(PipeContext ctx) {
    }

    default float getTargetSpeed(PipeContext ctx) {
        return 0f;
    }

    default float getAcceleration(PipeContext ctx) {
        return 0f;
    }

    default float getMaxSpeed(PipeContext ctx) {
        return 0f;
    }

    default boolean applyAcceleration(PipeContext ctx) {
        return false;
    }

    default RoutePlan route(PipeContext ctx, com.logistics.pipe.runtime.TravelingItem item, List<Direction> options) {
        return RoutePlan.pass();
    }

    default boolean discardWhenNoRoute(PipeContext ctx) {
        return false;
    }

    default boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        return true;
    }

    default void onConnectionsChanged(PipeContext ctx, List<Direction> options) {}

    default void onWrenchUse(PipeContext ctx, ItemUsageContext usage) {}

    default int comparatorOutput(PipeContext ctx) {
        return 0;
    }

    default boolean hasComparatorOutput() {
        return false;
    }

    /**
     * Return false to prevent this pipe from connecting to inventories.
     */
    default boolean allowsInventoryConnections() {
        return true;
    }

    /**
     * Called randomly on the client for display effects like particles.
     * Modules can override this to add visual effects.
     */
    default void randomDisplayTick(PipeContext ctx, Random random) {
    }

    /**
     * Get the NBT state key for this module.
     * Defaults to lowercase class simple name (e.g., "mergermodule").
     * Override to provide custom key for backwards compatibility.
     */
    default String getStateKey() {
        return this.getClass().getSimpleName().toLowerCase();
    }
}
