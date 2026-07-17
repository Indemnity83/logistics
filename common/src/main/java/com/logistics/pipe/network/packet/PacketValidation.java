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
        // distanceToSqr avoids Math.sqrt(); squaring maxDistance gives the same comparison.
        // Use the block-center coordinates directly (matches the other distance checks in the
        // codebase, e.g. AdvancedExtractorScreenHandler/RequesterModule).
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                <= maxDistance * maxDistance;
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
