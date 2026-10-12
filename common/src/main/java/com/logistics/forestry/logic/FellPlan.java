package com.logistics.forestry.logic;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The outcome of {@link TreeFeller#plan}: either the blocks to break, in break order, or why the
 * tree was skipped. A skipped plan has no blocks; a found plan has no reason.
 */
public record FellPlan(List<BlockPos> blocks, @Nullable Reason reason) {

    public enum Reason {
        /** No wood at the plot or within reach above it. */
        NO_TREE,
        /** More blocks than {@link TreeFeller.Limits#cap()}; too big to be a tree. */
        CAP_EXCEEDED,
        /** Wood without natural canopy; treated as a player build. */
        NO_CANOPY
    }

    public FellPlan {
        blocks = List.copyOf(blocks);
    }

    public static FellPlan found(List<BlockPos> ordered) {
        return new FellPlan(ordered, null);
    }

    public static FellPlan skip(Reason reason) {
        return new FellPlan(List.of(), Objects.requireNonNull(reason));
    }

    public boolean isFound() {
        return reason == null;
    }
}
