package com.logistics.pipe.modules;

import com.logistics.pipe.PipeContext;
import net.minecraft.util.RandomSource;

/**
 * Role interface for modules that need random block ticks.
 *
 * <p>Eliminates the fragile {@code hasRandomTicks()} / {@code randomTick()} pair on
 * {@link Module} — implementing this interface is the sole opt-in mechanism.
 * {@link com.logistics.pipe.Pipe#hasRandomTicks()} returns true if any module implements
 * this interface.
 */
public interface RandomTickModule extends Module {
    /**
     * Called on random block ticks. Only invoked if the pipe block is registered for random ticks,
     * which happens automatically when any module in the pipe implements this interface.
     *
     * @param ctx    the pipe context
     * @param random the random source
     */
    void randomTick(PipeContext ctx, RandomSource random);
}
