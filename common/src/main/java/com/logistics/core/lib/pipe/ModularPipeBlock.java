package com.logistics.core.lib.pipe;

/**
 * A block whose behavior is defined by a composed {@link ModularPipe}. Implemented by both the item
 * pipe block and the fluid pipe block so family-aware modules (weathering, marking) can discover a
 * neighbour's modules and restrict themselves to their own {@link PipeFamily}.
 */
public interface ModularPipeBlock {
    /** The module composition backing this block. */
    ModularPipe modularPipe();

    /** Which payload family this pipe belongs to. Modules only ever match their own family. */
    PipeFamily family();
}
