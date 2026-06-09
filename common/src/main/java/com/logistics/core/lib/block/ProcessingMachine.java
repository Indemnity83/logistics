package com.logistics.core.lib.block;

/**
 * A machine that processes an input over time toward a recipe-defined duration (macerator, kiln, …).
 * Lives in {@code core.lib} so HUD/integration code can read processing state uniformly without depending
 * on a specific machine's domain.
 */
public interface ProcessingMachine {

    /** Current processing progress as a 0..1 fraction; 0 when idle. */
    float processProgress();

    /** True while a recipe is being processed, even if currently stalled on power. */
    boolean isProcessing();
}
