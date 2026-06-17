package com.logistics.core.lib.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-agnostic lookup for a fluid's still sprite and world tint (Pattern A — SPI).
 *
 * <p>On MC 26.1 this is resolved through the unified vanilla {@code FluidModel} set, but 1.21.x has no
 * such API, so the still sprite and tint are loader-specific: Fabric resolves them through
 * {@code FluidRenderHandlerRegistry}, NeoForge through {@code IClientFluidTypeExtensions}. The loader
 * registers its resolver once during client initialization; shared client code (the fluid pipe and tank
 * renderers, via {@link FluidBoxRenderer#resolve}) calls {@link #resolve} without depending on either API.
 */
public final class FluidSpriteLookup {

    @FunctionalInterface
    public interface Resolver {
        @Nullable
        FluidBoxRenderer.Appearance resolve(Fluid fluid, @Nullable Level level, BlockPos pos);
    }

    private static volatile Resolver resolver;

    private FluidSpriteLookup() {}

    /** Register the platform-specific resolver. Called once during client loader initialization. */
    public static void register(Resolver r) {
        if (r == null) throw new NullPointerException("resolver must not be null");
        if (resolver != null) throw new IllegalStateException("FluidSpriteLookup resolver already registered");
        resolver = r;
    }

    /** Resolve a fluid's still sprite and world tint, or {@code null} if it has no renderable sprite. */
    @Nullable
    public static FluidBoxRenderer.Appearance resolve(Fluid fluid, @Nullable Level level, BlockPos pos) {
        if (resolver == null) throw new IllegalStateException("FluidSpriteLookup resolver not registered");
        return resolver.resolve(fluid, level, pos);
    }
}
