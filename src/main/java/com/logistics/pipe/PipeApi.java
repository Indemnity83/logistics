package com.logistics.pipe;

import com.logistics.LogisticsPipe;
import com.logistics.api.TransportApi;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PipeApi implements TransportApi {
    @Override
    public boolean isTransportBlock(BlockState state) {
        return state.getBlock() instanceof PipeBlock;
    }

    @Override
    public boolean tryInsert(ServerLevel world, BlockPos targetPos, ItemStack stack, Direction from) {
        BlockEntity aboveEntity = world.getBlockEntity(targetPos);
        if (aboveEntity instanceof PipeBlockEntity pipeEntity) {
            TravelingItem travelingItem = new TravelingItem(stack.copy(), from, LogisticsPipe.CONFIG.ITEM_MIN_SPEED);
            pipeEntity.addItem(travelingItem, from.getOpposite(), false);
            return true;
        }
        return false;
    }

    @Override
    public boolean forceInsert(ServerLevel world, BlockPos targetPos, ItemStack stack, Direction from) {
        BlockEntity aboveEntity = world.getBlockEntity(targetPos);
        if (aboveEntity instanceof PipeBlockEntity pipeEntity) {
            TravelingItem travelingItem = new TravelingItem(stack.copy(), from, LogisticsPipe.CONFIG.ITEM_MIN_SPEED);
            pipeEntity.addItem(travelingItem, from.getOpposite(), true);
            return true;
        }
        return false;
    }
}
