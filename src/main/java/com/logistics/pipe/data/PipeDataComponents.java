package com.logistics.pipe.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Domain data structures for pipe components.
 */
public final class PipeDataComponents {
    private PipeDataComponents() {}

    /**
     * Immutable record storing weathering state for copper pipes.
     */
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
