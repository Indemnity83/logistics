package com.logistics.automation.fabricator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-to-server toggle of a fabricator output's selection. Validates the player is near the
 * machine, then flips the recipe (identified by its id string) in the block entity's build queue.
 */
public record ToggleFabricatorSelectionPacket(BlockPos pos, String recipeId) implements CustomPacketPayload {

    private static final double MAX_REACH = 64.0;

    public static final CustomPacketPayload.Type<ToggleFabricatorSelectionPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsAutomation.resource("toggle_fabricator_selection").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleFabricatorSelectionPacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ToggleFabricatorSelectionPacket::pos,
                    ByteBufCodecs.STRING_UTF8, ToggleFabricatorSelectionPacket::recipeId,
                    ToggleFabricatorSelectionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_REACH * MAX_REACH) {
            return;
        }
        ResourceId id = ResourceId.tryParse(recipeId);
        if (id == null) {
            return;
        }
        if (player.level() instanceof ServerLevel level) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SequentialFabricatorBlockEntity fabricator) {
                fabricator.toggleSelection(id);
            }
        }
    }
}
