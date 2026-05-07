package com.logistics.pipe.network.packet;

import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Consumer;

final class PacketValidation {
    private PacketValidation() {}

    static boolean isPlayerInRange(ServerPlayer player, BlockPos pos, double maxDistance) {
        // distanceToSqr avoids Math.sqrt(); squaring maxDistance gives the same comparison
        return player.distanceToSqr(pos.getCenter()) <= maxDistance * maxDistance;
    }

    static boolean isPlayerOutOfRange(ServerPlayer player, BlockPos pos, double maxDistance) {
        return !isPlayerInRange(player, pos, maxDistance);
    }

    static void ifPlayerCanReach(ServerPlayer player, BlockPos pos, Consumer<PipeBlockEntity> action) {
        if (isPlayerOutOfRange(player, pos, 64)) return;
        if (player.level() instanceof ServerLevel level) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PipeBlockEntity pipeEntity) {
                action.accept(pipeEntity);
            }
        }
    }
}
