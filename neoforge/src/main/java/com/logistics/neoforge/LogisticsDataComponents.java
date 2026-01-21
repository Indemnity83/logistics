package com.logistics.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class LogisticsDataComponents {
    private LogisticsDataComponents() {}

    public static final DataComponentType<WeatheringState> WEATHERING_STATE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(LogisticsMod.MOD_ID, "weathering_state"),
            DataComponentType.<WeatheringState>builder()
                    .persistent(WeatheringState.CODEC)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.debug("Registering data components");
    }

    public record WeatheringState(int oxidationStage, boolean waxed) {
        public static final Codec<WeatheringState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.INT.fieldOf("oxidation_stage").forGetter(WeatheringState::oxidationStage),
                        Codec.BOOL.optionalFieldOf("waxed", false).forGetter(WeatheringState::waxed))
                .apply(instance, WeatheringState::new));

        public static final WeatheringState DEFAULT = new WeatheringState(0, false);

        public boolean isDefault() {
            return oxidationStage == 0 && !waxed;
        }
    }
}
