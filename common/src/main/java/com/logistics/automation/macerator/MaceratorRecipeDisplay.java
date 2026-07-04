package com.logistics.automation.macerator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/**
 * Recipe display for the Macerator, used by the recipe book to render recipe entries.
 */
public record MaceratorRecipeDisplay(
    SlotDisplay ingredient,
    SlotDisplay result,
    SlotDisplay craftingStation,
    int energy,
    float experience
) implements RecipeDisplay {

    public static final MapCodec<MaceratorRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SlotDisplay.CODEC.fieldOf("ingredient").forGetter(MaceratorRecipeDisplay::ingredient),
        SlotDisplay.CODEC.fieldOf("result").forGetter(MaceratorRecipeDisplay::result),
        SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(MaceratorRecipeDisplay::craftingStation),
        Codec.INT.fieldOf("energy").forGetter(MaceratorRecipeDisplay::energy),
        Codec.FLOAT.fieldOf("experience").forGetter(MaceratorRecipeDisplay::experience)
    ).apply(i, MaceratorRecipeDisplay::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MaceratorRecipeDisplay> STREAM_CODEC =
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC, MaceratorRecipeDisplay::ingredient,
            SlotDisplay.STREAM_CODEC, MaceratorRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC, MaceratorRecipeDisplay::craftingStation,
            ByteBufCodecs.VAR_INT, MaceratorRecipeDisplay::energy,
            ByteBufCodecs.FLOAT, MaceratorRecipeDisplay::experience,
            MaceratorRecipeDisplay::new
        );

    public static final RecipeDisplay.Type<MaceratorRecipeDisplay> TYPE =
        new RecipeDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeDisplay.Type<MaceratorRecipeDisplay> type() {
        return TYPE;
    }
}
