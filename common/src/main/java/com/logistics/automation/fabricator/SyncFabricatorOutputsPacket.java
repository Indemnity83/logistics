package com.logistics.automation.fabricator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server-to-client sync of a fabricator's currently-craftable outputs. Parallel lists: recipe id
 * (as a string), result stack, and selection state (0 available, 1 queued, 2 active) per output.
 */
public record SyncFabricatorOutputsPacket(
        BlockPos pos, List<String> recipeIds, List<ItemStack> results, List<Integer> states)
        implements CustomPacketPayload {

    public SyncFabricatorOutputsPacket {
        if (recipeIds.size() != results.size() || recipeIds.size() != states.size()) {
            throw new IllegalArgumentException("fabricator output lists must be the same length: "
                    + recipeIds.size() + " ids, " + results.size() + " results, " + states.size() + " states");
        }
    }

    public static final CustomPacketPayload.Type<SyncFabricatorOutputsPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsAutomation.resource("sync_fabricator_outputs").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFabricatorOutputsPacket> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncFabricatorOutputsPacket::pos,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncFabricatorOutputsPacket::recipeIds,
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC, SyncFabricatorOutputsPacket::results,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), SyncFabricatorOutputsPacket::states,
                    SyncFabricatorOutputsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Reassemble the parallel lists into {@link FabricatorProcessorComponent.Output}s, skipping unparseable ids. */
    public List<FabricatorProcessorComponent.Output> toOutputs() {
        List<FabricatorProcessorComponent.Output> outputs = new ArrayList<>(recipeIds.size());
        for (int i = 0; i < recipeIds.size(); i++) {
            ResourceId id = ResourceId.tryParse(recipeIds.get(i));
            if (id != null) {
                outputs.add(new FabricatorProcessorComponent.Output(id, results.get(i), states.get(i)));
            }
        }
        return outputs;
    }
}
