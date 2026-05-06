package com.logistics.pipe.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

final class PacketValidation {
    private PacketValidation() {}

    static boolean isPlayerInRange(ServerPlayer player, BlockPos pos, double maxDistance) {
        // distanceToSqr avoids Math.sqrt(); squaring maxDistance gives the same comparison
        return player.distanceToSqr(pos.getCenter()) <= maxDistance * maxDistance;
    }

    static boolean isPlayerOutOfRange(ServerPlayer player, BlockPos pos) {
        return isPlayerOutOfRange(player, pos, 64);
    }

    static boolean isPlayerOutOfRange(ServerPlayer player, BlockPos pos, double maxDistance) {
        return !isPlayerInRange(player, pos, maxDistance);
    }
}
