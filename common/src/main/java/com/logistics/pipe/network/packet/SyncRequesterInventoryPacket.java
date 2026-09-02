package com.logistics.pipe.network.packet;

import com.logistics.LogisticsPipe;
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
 *
 * <p>{@code containerId} addresses the payload to one open menu. The receiving client applies it
 * only when it matches the menu it currently has open, so a stale or misdirected payload can never
 * repoint a requester screen at another player's pipe.
 */
public record SyncRequesterInventoryPacket(
        int containerId, BlockPos pipePos, List<ItemStack> items, List<Long> amounts)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncRequesterInventoryPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsPipe.resource("sync_requester_inventory").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRequesterInventoryPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SyncRequesterInventoryPacket::containerId,
                    BlockPos.STREAM_CODEC, SyncRequesterInventoryPacket::pipePos,
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC, SyncRequesterInventoryPacket::items,
                    ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), SyncRequesterInventoryPacket::amounts,
                    SyncRequesterInventoryPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
