package com.logistics.power.block.entity;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Energy buffer that accepts power from generators and distributes it to adjacent
 * machines and connected logistics pipe networks.
 *
 * <p>Capacity, per-side I/O, and active push rate are configurable (see the {@code power/battery}
 * config). Defaults: 100,000 RF storage capacity; 1,000 RF/t insert/extract per side (the
 * {@code max_io}-controlled limits passed to the constructor); and a 200 RF/t active push rate into
 * each adjacent machine (from {@link AbstractBatteryBlockEntity#maxOutputPerSide()}).
 *
 * <p>When placed adjacent to a logistics pipe network, this battery registers itself
 * as an energy source for that network. Logistics pipe modules (requester, provider, etc.)
 * draw energy directly from the battery, so no per-pipe energy buffer is needed.
 */
public class BatteryBlockEntity extends AbstractBatteryBlockEntity {

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.BATTERY_BLOCK_ENTITY, pos, state,
                capacity(),
                LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_MAX_IO),
                LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_MAX_IO));
    }

    /** Configured total RF storage — used for item fill-bar rendering and tests. */
    public static long capacity() {
        return LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_CAPACITY);
    }

    @Override
    protected long maxOutputPerSide() {
        return LogisticsConfigHost.get(LogisticsPower.CONFIG.BATTERY_OUTPUT_PER_SIDE);
    }
}
