package com.logistics.power.block.entity;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Energy buffer that accepts power from generators and distributes it to adjacent
 * machines and connected logistics pipe networks.
 *
 * <p>Capacity: 100,000 RF. Max insert/extract: 1,000 RF/t per side.
 *
 * <p>When placed adjacent to a logistics pipe network, this battery registers itself
 * as an energy source for that network. Logistics pipe modules (requester, provider, etc.)
 * draw energy directly from the battery, so no per-pipe energy buffer is needed.
 */
public class BatteryBlockEntity extends AbstractBatteryBlockEntity {
    public static final long CAPACITY = 100_000L;
    public static final long MAX_IO = 1_000L;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.BATTERY_BLOCK_ENTITY, pos, state, CAPACITY, MAX_IO, MAX_IO);
    }
}
