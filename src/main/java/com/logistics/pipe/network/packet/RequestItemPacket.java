package com.logistics.pipe.network.packet;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.RequesterModule;
import com.logistics.pipe.ui.PipeModuleHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Packet sent from client to server to request an item from the network.
 */
public record RequestItemPacket(BlockPos pipePos, ItemStack stack, int amount)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestItemPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsPipe.resource("request_item").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestItemPacket> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            RequestItemPacket::pipePos,
            ItemStack.STREAM_CODEC,
            RequestItemPacket::stack,
            ByteBufCodecs.INT,
            RequestItemPacket::amount,
            RequestItemPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Register this packet type with Fabric networking.
     */
    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TYPE, (packet, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                BlockPos pos = packet.pipePos();
                double dx = player.getX() - (pos.getX() + 0.5);
                double dy = player.getY() - (pos.getY() + 0.5);
                double dz = player.getZ() - (pos.getZ() + 0.5);
                if (dx * dx + dy * dy + dz * dz > 64.0 * 64.0) return;
                if (player.level() instanceof ServerLevel level) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof PipeBlockEntity pipeEntity) {
                        PipeModuleHelper.withModule(
                                pipeEntity,
                                RequesterModule.class,
                                (ctx, module) -> module.requestItem(ctx, packet.stack(), packet.amount()));
                    }
                }
            });
        });
    }
}
