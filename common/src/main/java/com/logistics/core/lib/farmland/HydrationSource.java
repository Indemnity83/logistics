package com.logistics.core.lib.farmland;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Something that keeps farmland wet without water nearby, registered with {@link FarmlandHydration}.
 * Vanilla farmland asks whether it is near water on each of its random ticks: wet, it goes to or
 * stays at full moisture; dry, it loses a level. A source is asked only once real water was not
 * found, and only about positions inside its {@link #bounds()}.
 */
public interface HydrationSource {

    /** The block that owns this source; on NeoForge, unloading its chunk ends the source. */
    BlockPos anchor();

    /** The blocks this source may wet, inclusive; must not change while it is registered. */
    BoundingBox bounds();

    /**
     * Whether the farmland at {@code pos} counts as near water now. Asked once per check, so it may
     * pay for the answer; must not change the world.
     */
    boolean hydrate(ServerLevel level, BlockPos pos);
}
