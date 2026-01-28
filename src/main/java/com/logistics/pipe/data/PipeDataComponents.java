package com.logistics.pipe.data;

import com.logistics.LogisticsMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class PipeDataComponents {
    private PipeDataComponents() {}

    /**
     * Stores weathering state (oxidation stage and waxed status) for copper pipes.
     */
    public static final DataComponentType<WeatheringState> WEATHERING_STATE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "pipe/weathering_state"),
            DataComponentType.<WeatheringState>builder()
                    .persistent(WeatheringState.CODEC)
                    .build());

    public static void initialize() {
        LogisticsMod.LOGGER.debug("Registering pipe data components");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        BuiltInRegistries.DATA_COMPONENT_TYPE.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "weathering_state"),
                BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(WEATHERING_STATE));
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
