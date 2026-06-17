package com.logistics.core.lib.pipe;

import org.jetbrains.annotations.Nullable;

/**
 * A block whose behavior is defined by a composed {@link ModularPipe}. Implemented by both the item
 * pipe block and the fluid pipe block so family-aware modules (weathering, marking) can discover a
 * neighbour's modules and restrict themselves to their own {@link PipeFamily}.
 */
public interface ModularPipeBlock {
    /**
     * The module composition backing this block, or {@code null} if this block has no pipe definition
     * (e.g. an item pipe block created without one). Callers must null-check before use.
     */
    @Nullable ModularPipe modularPipe();

    /** Which payload family this pipe belongs to. Modules only ever match their own family. */
    PipeFamily family();
}
