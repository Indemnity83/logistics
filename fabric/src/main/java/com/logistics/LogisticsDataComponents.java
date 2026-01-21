package com.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class LogisticsDataComponents {
    private LogisticsDataComponents() {}

    /**
     * Stores weathering state (oxidation stage and waxed status) for copper pipes.
     */
    public static final ComponentType<WeatheringState> WEATHERING_STATE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "weathering_state"),
            ComponentType.<WeatheringState>builder()
                    .codec(WeatheringState.CODEC)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.debug("Registering data components");
    }

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
