package com.logistics.pipe.network.packet;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SatelliteModule;
import com.logistics.pipe.ui.PipeModuleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Packet sent from client to server to update the satellite ID of a Satellite Logistics Pipe.
 */
public record SetSatelliteIdPacket(BlockPos pipePos, String satelliteId)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetSatelliteIdPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsPipe.resource("set_satellite_id").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, SetSatelliteIdPacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SetSatelliteIdPacket::pipePos,
                    ByteBufCodecs.STRING_UTF8,
                    SetSatelliteIdPacket::satelliteId,
                    SetSatelliteIdPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        BlockPos pos = pipePos();
        if (PacketValidation.isPlayerOutOfRange(player, pos)) return;
        if (player.level() instanceof ServerLevel level) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PipeBlockEntity pipeEntity) {
                PipeModuleHelper.withModule(
                        pipeEntity,
                        SatelliteModule.class,
                        (ctx, module) -> module.setSatelliteId(ctx, satelliteId()));
            }
        }
    }
}
