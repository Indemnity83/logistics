package com.logistics.forestry.farm;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The one operation a {@link FarmLogic} wants performed on a plot. The {@link FarmComponent}
 * executes it, charging power and materials and enforcing the farm-wide gates; the logic only
 * decides.
 */
public sealed interface PlotAction {

    /** Nothing to do at this plot right now. */
    PlotAction NONE = new None();

    /**
     * Break {@code blocks} in order, collecting the drops. A multi-block list (a tree) is broken
     * across ticks, a few blocks per tick, and must be ordered so that breaking it in order never
     * leaves a block unsupported (top-down).
     */
    record Harvest(List<BlockPos> blocks) implements PlotAction {
        public Harvest {
            blocks = blocks.stream().map(BlockPos::immutable).toList();
        }
    }

    /**
     * Lay one soil item from the soil slots at {@code pos}. Whatever stands there is broken first,
     * its drops kept, and a fluid is simply overwritten; a block entity or an unbreakable block is
     * left standing and reported as blocked.
     */
    record PlaceSoil(BlockPos pos) implements PlotAction {
        public PlaceSoil {
            pos = pos.immutable();
        }
    }

    /** Turn the block at {@code pos} into {@code result} in place, e.g. dirt into dry farmland. Costs only RF. */
    record Till(BlockPos pos, BlockState result) implements PlotAction {
        public Till {
            pos = pos.immutable();
            Objects.requireNonNull(result);
        }
    }

    /**
     * Plant one {@code germling} at each of {@code positions}, all or none: several positions plant
     * an aligned group such as a 2×2 tree. An empty {@code germling} means the plot needs planting
     * but nothing suitable is available, which the farm reports as no germlings.
     */
    record Plant(List<BlockPos> positions, ItemStack germling) implements PlotAction {
        public Plant {
            positions = positions.stream().map(BlockPos::immutable).toList();
            germling = germling.isEmpty() ? ItemStack.EMPTY : germling.copyWithCount(1);
        }
    }

    /** See {@link #NONE}. */
    record None() implements PlotAction {}

    /** The plot needs work the farm cannot do; reported instead of retrying the plot forever. */
    record Blocked(Reason reason) implements PlotAction {
        public Blocked {
            Objects.requireNonNull(reason);
        }
    }

    enum Reason {
        /** Too dark for what would be planted or grown here. */
        TOO_DARK,
        /** Something in the way, e.g. a solid block over farmland. */
        OBSTRUCTED
    }

    static Harvest harvest(BlockPos pos) {
        return new Harvest(List.of(pos));
    }

    static Plant plant(BlockPos pos, ItemStack germling) {
        return new Plant(List.of(pos), germling);
    }

    static Blocked blocked(Reason reason) {
        return new Blocked(reason);
    }
}
