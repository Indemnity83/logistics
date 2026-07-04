package com.logistics.automation.crucible;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/**
 * Recipe display for the Crucible; the result slot shows a bucket of the output fluid.
 */
public record CrucibleRecipeDisplay(
    SlotDisplay ingredient,
    SlotDisplay result,
    SlotDisplay craftingStation,
    int energy,
    float experience
) implements RecipeDisplay {

    public static final MapCodec<CrucibleRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SlotDisplay.CODEC.fieldOf("ingredient").forGetter(CrucibleRecipeDisplay::ingredient),
        SlotDisplay.CODEC.fieldOf("result").forGetter(CrucibleRecipeDisplay::result),
        SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(CrucibleRecipeDisplay::craftingStation),
        Codec.INT.fieldOf("energy").forGetter(CrucibleRecipeDisplay::energy),
        Codec.FLOAT.fieldOf("experience").forGetter(CrucibleRecipeDisplay::experience)
    ).apply(i, CrucibleRecipeDisplay::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipeDisplay> STREAM_CODEC =
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC, CrucibleRecipeDisplay::ingredient,
            SlotDisplay.STREAM_CODEC, CrucibleRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC, CrucibleRecipeDisplay::craftingStation,
            ByteBufCodecs.VAR_INT, CrucibleRecipeDisplay::energy,
            ByteBufCodecs.FLOAT, CrucibleRecipeDisplay::experience,
            CrucibleRecipeDisplay::new
        );

    public static final RecipeDisplay.Type<CrucibleRecipeDisplay> TYPE =
        new RecipeDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeDisplay.Type<CrucibleRecipeDisplay> type() {
        return TYPE;
    }
}
