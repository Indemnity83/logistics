package com.logistics.power.engine.reaction;

import com.logistics.LogisticsPower;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;

/** Server-to-client synchronization of Reaction Engine recipes for JEI. */
public record ReactionRecipeSyncPacket(List<ReactionRecipe> recipes) implements CustomPacketPayload {

    public static final Type<ReactionRecipeSyncPacket> TYPE =
        new Type<>(LogisticsPower.resource("sync_reaction_recipes").toIdentifier());

    public static final StreamCodec<RegistryFriendlyByteBuf, ReactionRecipeSyncPacket> CODEC =
        StreamCodec.composite(
            ReactionRecipeSerializer.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ReactionRecipeSyncPacket::recipes,
            ReactionRecipeSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static ReactionRecipeSyncPacket from(MinecraftServer server) {
        List<ReactionRecipe> recipes = server.getRecipeManager().getRecipes().stream()
            .map(holder -> holder.value())
            .filter(ReactionRecipe.class::isInstance)
            .map(ReactionRecipe.class::cast)
            .toList();
        return new ReactionRecipeSyncPacket(recipes);
    }
}
