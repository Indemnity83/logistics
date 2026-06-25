package com.logistics.core.machine.component;

import net.minecraft.world.level.ChunkPos;

/**
 * The set of chunks a machine wants kept loaded: a single center chunk plus the boundary chunks
 * around its work area. Supplied by the machine to a {@link ChunkLoadingComponent}, which knows
 * nothing about what the machine does inside the area.
 */
public interface ChunkArea {

    ChunkPos center();

    Iterable<ChunkPos> boundary();
}
