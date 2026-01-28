package com.logistics.pipe;

import com.logistics.api.TransportApi;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.TravelingItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class PipeApi implements TransportApi {
    @Override
    public boolean isTransportBlock(BlockState state) {
        return state.getBlock() instanceof PipeBlock;
    }

    @Override
    public boolean tryInsert(ServerWorld world, BlockPos targetPos, ItemStack stack, Direction from) {
        BlockEntity aboveEntity = world.getBlockEntity(targetPos);
        if (aboveEntity instanceof PipeBlockEntity pipeEntity) {
            TravelingItem travelingItem = new TravelingItem(stack.copy(), from, PipeConfig.ITEM_MIN_SPEED);
            pipeEntity.addItem(travelingItem, from.getOpposite(), false);
            return true;
        }
        return false;
    }

    @Override
    public boolean forceInsert(ServerWorld world, BlockPos targetPos, ItemStack stack, Direction from) {
        BlockEntity aboveEntity = world.getBlockEntity(targetPos);
        if (aboveEntity instanceof PipeBlockEntity pipeEntity) {
            TravelingItem travelingItem = new TravelingItem(stack.copy(), from, PipeConfig.ITEM_MIN_SPEED);
            pipeEntity.addItem(travelingItem, from.getOpposite(), true);
            return true;
        }
        return false;
    }
}
