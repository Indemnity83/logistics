package com.logistics.automation.alloysmelter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Recipe display for the Alloy Smelter, used by the recipe book to render recipe entries. */
public record AlloySmelterRecipeDisplay(
    SlotDisplay inputA,
    SlotDisplay inputB,
    SlotDisplay result,
    SlotDisplay craftingStation,
    int energy
) implements RecipeDisplay {

    public static final MapCodec<AlloySmelterRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SlotDisplay.CODEC.fieldOf("input_a").forGetter(AlloySmelterRecipeDisplay::inputA),
        SlotDisplay.CODEC.fieldOf("input_b").forGetter(AlloySmelterRecipeDisplay::inputB),
        SlotDisplay.CODEC.fieldOf("result").forGetter(AlloySmelterRecipeDisplay::result),
        SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(AlloySmelterRecipeDisplay::craftingStation),
        Codec.INT.fieldOf("energy").forGetter(AlloySmelterRecipeDisplay::energy)
    ).apply(i, AlloySmelterRecipeDisplay::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloySmelterRecipeDisplay> STREAM_CODEC =
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC, AlloySmelterRecipeDisplay::inputA,
            SlotDisplay.STREAM_CODEC, AlloySmelterRecipeDisplay::inputB,
            SlotDisplay.STREAM_CODEC, AlloySmelterRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC, AlloySmelterRecipeDisplay::craftingStation,
            ByteBufCodecs.VAR_INT, AlloySmelterRecipeDisplay::energy,
            AlloySmelterRecipeDisplay::new
        );

    public static final RecipeDisplay.Type<AlloySmelterRecipeDisplay> TYPE =
        new RecipeDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeDisplay.Type<AlloySmelterRecipeDisplay> type() {
        return TYPE;
    }
}
