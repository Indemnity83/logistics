package com.logistics.power.render;

import com.logistics.core.lib.power.EngineHeatTint;
import java.util.Set;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Loader-agnostic block tint source that colors engine blocks by their heat stage.
 *
 * <p>Declaring {@link #relevantProperties()} is essential on 26.1: without it the renderer bakes a
 * single tint for every heat stage and the color never changes in-world (see {@link EngineHeatTint}).
 * Both the Fabric and NeoForge client setup register this same instance.
 */
public final class EngineHeatTintSource implements BlockTintSource {

    public static final EngineHeatTintSource INSTANCE = new EngineHeatTintSource();

    private EngineHeatTintSource() {}

    @Override
    public int color(BlockState state) {
        return EngineHeatTint.color(state);
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return EngineHeatTint.RELEVANT_PROPERTIES;
    }
}
