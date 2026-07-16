package com.logistics.automation.jei;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.alloysmelter.AlloySmelterRecipe;
import com.logistics.automation.alloysmelter.AlloySmelterRecipeSerializer;
import com.logistics.automation.crucible.CrucibleRecipe;
import com.logistics.automation.crucible.CrucibleRecipeSerializer;
import com.logistics.automation.macerator.MaceratorRecipeSerializer;
import com.logistics.automation.macerator.MaceratorRecipeWrapper;
import com.logistics.automation.refinery.RefineryRecipe;
import com.logistics.automation.refinery.RefineryRecipeSerializer;
import com.logistics.automation.sawmill.SawmillRecipe;
import com.logistics.automation.sawmill.SawmillRecipeSerializer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;

/**
 * Server-to-client sync of the mod's machine recipes so JEI can show them on multiplayer clients.
 * Modern Minecraft no longer syncs full Recipe objects (only displays), which lack the
 * byproduct/fluid data these categories render, so the server ships them explicitly on join.
 */
public record SyncMachineRecipesPacket(
        List<MaceratorRecipeWrapper> macerator,
        List<SawmillRecipe> sawmill,
        List<CrucibleRecipe> crucible,
        List<AlloySmelterRecipe> alloySmelter,
        List<RefineryRecipe> refinery)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMachineRecipesPacket> TYPE =
            new CustomPacketPayload.Type<>(LogisticsAutomation.resource("sync_machine_recipes").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMachineRecipesPacket> CODEC =
            StreamCodec.composite(
                    MaceratorRecipeSerializer.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncMachineRecipesPacket::macerator,
                    SawmillRecipeSerializer.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncMachineRecipesPacket::sawmill,
                    CrucibleRecipeSerializer.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncMachineRecipesPacket::crucible,
                    AlloySmelterRecipeSerializer.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncMachineRecipesPacket::alloySmelter,
                    RefineryRecipeSerializer.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncMachineRecipesPacket::refinery,
                    SyncMachineRecipesPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Reads the server's loaded recipes and partitions the five machine recipe types into one packet. */
    public static SyncMachineRecipesPacket from(MinecraftServer server) {
        List<MaceratorRecipeWrapper> macerator = new ArrayList<>();
        List<SawmillRecipe> sawmill = new ArrayList<>();
        List<CrucibleRecipe> crucible = new ArrayList<>();
        List<AlloySmelterRecipe> alloySmelter = new ArrayList<>();
        List<RefineryRecipe> refinery = new ArrayList<>();

        server.getRecipeManager().getRecipes().forEach(holder -> {
            var recipe = holder.value();
            if (recipe instanceof MaceratorRecipeWrapper r) {
                macerator.add(r);
            } else if (recipe instanceof SawmillRecipe r) {
                sawmill.add(r);
            } else if (recipe instanceof CrucibleRecipe r) {
                crucible.add(r);
            } else if (recipe instanceof AlloySmelterRecipe r) {
                alloySmelter.add(r);
            } else if (recipe instanceof RefineryRecipe r) {
                refinery.add(r);
            }
        });

        return new SyncMachineRecipesPacket(macerator, sawmill, crucible, alloySmelter, refinery);
    }
}
