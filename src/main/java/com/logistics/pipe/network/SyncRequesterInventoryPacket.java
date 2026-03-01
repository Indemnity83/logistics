package com.logistics.pipe.network;

import com.logistics.LogisticsPipe;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server-to-client packet that syncs available network items to the requester screen.
 * Includes both display items (with counts capped at 64) and full amounts.
 */
public record SyncRequesterInventoryPacket(BlockPos pipePos, List<ItemStack> items, List<Long> amounts)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRequesterInventoryPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsPipe.resource("sync_requester_inventory").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRequesterInventoryPacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncRequesterInventoryPacket::pipePos,
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC, SyncRequesterInventoryPacket::items,
                    ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), SyncRequesterInventoryPacket::amounts,
                    SyncRequesterInventoryPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Register this packet type (server-to-client).
     * Client-side receiver is registered in LogisticsPipeClient.
     */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }
}
