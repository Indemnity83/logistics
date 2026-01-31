package com.logistics.pipe.data;

import com.logistics.LogisticsMod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class PipeDataComponents {
    private PipeDataComponents() {}

    /**
     * Stores weathering state (oxidation stage and waxed status) for copper pipes.
     */
    public static final ComponentType<WeatheringState> WEATHERING_STATE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(LogisticsMod.MOD_ID, "pipe/weathering_state"),
            ComponentType.<WeatheringState>builder()
                    .codec(WeatheringState.CODEC)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.debug("Registering pipe data components");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        Registries.DATA_COMPONENT_TYPE.addAlias(
                Identifier.of(LogisticsMod.MOD_ID, "weathering_state"),
                Registries.DATA_COMPONENT_TYPE.getId(WEATHERING_STATE));
    }

    /**
     * Immutable record storing weathering state.
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
