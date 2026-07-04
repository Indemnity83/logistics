package com.logistics.automation.sawmill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Recipe display for the Sawmill, used by the recipe book to render recipe entries. */
public record SawmillRecipeDisplay(
    SlotDisplay ingredient,
    SlotDisplay result,
    SlotDisplay craftingStation,
    int energy,
    float experience
) implements RecipeDisplay {

    public static final MapCodec<SawmillRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SlotDisplay.CODEC.fieldOf("ingredient").forGetter(SawmillRecipeDisplay::ingredient),
        SlotDisplay.CODEC.fieldOf("result").forGetter(SawmillRecipeDisplay::result),
        SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(SawmillRecipeDisplay::craftingStation),
        Codec.INT.fieldOf("energy").forGetter(SawmillRecipeDisplay::energy),
        Codec.FLOAT.fieldOf("experience").forGetter(SawmillRecipeDisplay::experience)
    ).apply(i, SawmillRecipeDisplay::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SawmillRecipeDisplay> STREAM_CODEC =
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC, SawmillRecipeDisplay::ingredient,
            SlotDisplay.STREAM_CODEC, SawmillRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC, SawmillRecipeDisplay::craftingStation,
            ByteBufCodecs.VAR_INT, SawmillRecipeDisplay::energy,
            ByteBufCodecs.FLOAT, SawmillRecipeDisplay::experience,
            SawmillRecipeDisplay::new
        );

    public static final RecipeDisplay.Type<SawmillRecipeDisplay> TYPE =
        new RecipeDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeDisplay.Type<SawmillRecipeDisplay> type() {
        return TYPE;
    }
}
