package com.logistics.power.block.entity;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.power.block.BatteryBlock;
import com.logistics.power.block.BatteryTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Energy buffer that accepts power from generators and distributes it to adjacent
 * machines and connected logistics pipe networks.
 *
 * <p>Capacity, per-side I/O, and active push rate come from the placed block's
 * {@link BatteryTier} (see the {@code power/battery/<tier>} configs). One block entity type backs
 * all five tiers, so the numbers are read from the block rather than baked into the type.
 *
 * <p>When placed adjacent to a logistics pipe network, this battery registers itself
 * as an energy source for that network. Logistics pipe modules (requester, provider, etc.)
 * draw energy directly from the battery, so no per-pipe energy buffer is needed.
 */
public class BatteryBlockEntity extends AbstractBatteryBlockEntity {

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.BATTERY_BLOCK_ENTITY, pos, state,
                tierOf(state).capacity(),
                tierOf(state).maxIo(),
                tierOf(state).maxIo());
    }

    /**
     * Tier of the battery at this state, falling back to {@link BatteryTier#COPPER} for a state
     * that is somehow not a {@link BatteryBlock} — the same defensive read
     * {@code CableBlockEntity} does for its own tier.
     */
    public static BatteryTier tierOf(BlockState state) {
        return state.getBlock() instanceof BatteryBlock battery ? battery.tier() : BatteryTier.COPPER;
    }

    @Override
    protected long maxOutputPerSide() {
        return tierOf(getBlockState()).outputPerSide();
    }
}
