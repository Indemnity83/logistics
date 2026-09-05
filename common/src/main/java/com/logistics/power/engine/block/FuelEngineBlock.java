package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.FuelEngineBlockEntity;
import com.logistics.LogisticsPower;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Fuel Engine - burns liquid fuel with water coolant to generate up to 40 RF/t.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>FACING determines output direction; POWERED gates operation (redstone-controlled)</li>
 *   <li>Two fluid inputs: a fuel tank and a separate water-coolant tank</li>
 *   <li>Overheats if cooling can't keep up; a wrench resets thermal shutdown</li>
 * </ul>
 */
public class FuelEngineBlock extends AbstractEngineBlock<FuelEngineBlockEntity> {
    public FuelEngineBlock(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult onWrench(Level world, BlockPos pos, Player player) {
        if (world.getBlockEntity(pos) instanceof FuelEngineBlockEntity engine && engine.isOverheated()) {
            if (!world.isClientSide()) {
                engine.resetOverheat();
            }
            return InteractionResult.SUCCESS;
        }
        return super.onWrench(world, pos, player);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LogisticsPower.ENTITY.FUEL_ENGINE_BLOCK_ENTITY, FuelEngineBlockEntity::tick);
    }
}
