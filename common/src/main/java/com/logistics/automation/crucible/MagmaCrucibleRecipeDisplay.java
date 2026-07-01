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
 * Recipe display for the Magma Crucible; the result slot shows a bucket of the output fluid.
 */
public record MagmaCrucibleRecipeDisplay(
    SlotDisplay ingredient,
    SlotDisplay result,
    SlotDisplay craftingStation,
    int energyRequired,
    float experience
) implements RecipeDisplay {

    public static final MapCodec<MagmaCrucibleRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SlotDisplay.CODEC.fieldOf("ingredient").forGetter(MagmaCrucibleRecipeDisplay::ingredient),
        SlotDisplay.CODEC.fieldOf("result").forGetter(MagmaCrucibleRecipeDisplay::result),
        SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(MagmaCrucibleRecipeDisplay::craftingStation),
        Codec.INT.fieldOf("energy").forGetter(MagmaCrucibleRecipeDisplay::energyRequired),
        Codec.FLOAT.fieldOf("experience").forGetter(MagmaCrucibleRecipeDisplay::experience)
    ).apply(i, MagmaCrucibleRecipeDisplay::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagmaCrucibleRecipeDisplay> STREAM_CODEC =
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC, MagmaCrucibleRecipeDisplay::ingredient,
            SlotDisplay.STREAM_CODEC, MagmaCrucibleRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC, MagmaCrucibleRecipeDisplay::craftingStation,
            ByteBufCodecs.VAR_INT, MagmaCrucibleRecipeDisplay::energyRequired,
            ByteBufCodecs.FLOAT, MagmaCrucibleRecipeDisplay::experience,
            MagmaCrucibleRecipeDisplay::new
        );

    public static final RecipeDisplay.Type<MagmaCrucibleRecipeDisplay> TYPE =
        new RecipeDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeDisplay.Type<MagmaCrucibleRecipeDisplay> type() {
        return TYPE;
    }
}
