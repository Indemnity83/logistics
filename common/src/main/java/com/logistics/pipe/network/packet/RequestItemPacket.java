package com.logistics.pipe.network.packet;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.RequesterModule;
import com.logistics.pipe.ui.PipeModuleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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

    public void handle(ServerPlayer player) {
        PacketValidation.ifPlayerCanReach(player, pipePos(), this::requestItem);
    }

    private void requestItem(PipeBlockEntity pipeEntity) {
        PipeModuleHelper.withModule(pipeEntity, RequesterModule.class,
                (ctx, module) -> module.requestItem(ctx, stack(), amount()));
    }
}
