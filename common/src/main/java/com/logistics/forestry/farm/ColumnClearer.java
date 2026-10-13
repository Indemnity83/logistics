package com.logistics.forestry.farm;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Picks the next block a farm clears from one plot's column, without touching the world.
 *
 * <p>A column runs from the plot up {@code height} blocks. The highest block in it that is neither
 * air nor the farm's own growth goes first, one per call, so clearing works top-down and never
 * leaves a gravity block unsupported. A fluid, source or flowing, is drained rather than broken.
 * A block entity or an unbreakable block is never cleared: the column stays blocked until it goes.
 * Own growth is passed over; the farm's logic harvests it.
 */
public final class ColumnClearer {

    /** What a block in a column is, as far as clearing goes. */
    public enum Kind {
        /** Air; nothing to clear. */
        EMPTY,
        /** The farm's own crop or tree; harvested by the logic, never cleared. */
        OWN_GROWTH,
        /** A fluid block, source or flowing; drained without drops. */
        FLUID,
        /** Anything else that may be broken, its drops kept. */
        BREAKABLE,
        /** A block entity or an unbreakable block; never cleared. */
        PROTECTED
    }

    /** The only world access clearing has: what each position is. */
    @FunctionalInterface
    public interface ColumnView {
        Kind kind(BlockPos pos);
    }

    /** What to do with a column next. */
    public sealed interface Clearing {
        /** Nothing left to clear; the logic may work the plot. */
        Clearing CLEAR = new Clear();

        record Clear() implements Clearing {}

        /** Break the block at {@code pos}, keeping its drops. */
        record Break(BlockPos pos) implements Clearing {
            public Break {
                pos = pos.immutable();
            }
        }

        /** Remove the fluid at {@code pos}. */
        record Drain(BlockPos pos) implements Clearing {
            public Drain {
                pos = pos.immutable();
            }
        }

        /** The block at {@code pos} must never be cleared, so the plot cannot be worked. */
        record Blocked(BlockPos pos) implements Clearing {
            public Blocked {
                pos = pos.immutable();
            }
        }
    }

    private ColumnClearer() {}

    /** The next step for the column of {@code height} blocks standing on {@code plot}. */
    public static Clearing next(ColumnView view, BlockPos plot, int height) {
        for (int dy = height - 1; dy >= 0; dy--) {
            BlockPos pos = plot.above(dy);
            Kind kind = Objects.requireNonNull(view.kind(pos));
            switch (kind) {
                case EMPTY, OWN_GROWTH -> {}
                case FLUID -> {
                    return new Clearing.Drain(pos);
                }
                case BREAKABLE -> {
                    return new Clearing.Break(pos);
                }
                case PROTECTED -> {
                    return new Clearing.Blocked(pos);
                }
            }
        }
        return Clearing.CLEAR;
    }

    /**
     * Classifies {@code state}. {@code destroySpeed} is the block's hardness where it stands, below
     * 0 for unbreakable blocks; {@code ownGrowth} is whether the farm's logic claims it.
     */
    public static Kind classify(BlockState state, float destroySpeed, boolean ownGrowth) {
        if (state.isAir()) {
            return Kind.EMPTY;
        }
        if (ownGrowth) {
            return Kind.OWN_GROWTH;
        }
        if (state.hasBlockEntity() || destroySpeed < 0) {
            return Kind.PROTECTED;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            return Kind.FLUID;
        }
        return Kind.BREAKABLE;
    }
}
