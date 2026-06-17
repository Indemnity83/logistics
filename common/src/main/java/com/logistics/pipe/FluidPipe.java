package com.logistics.pipe;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.modules.FluidExtractorModule;
import com.logistics.pipe.modules.FluidMergerModule;
import com.logistics.pipe.modules.FluidTransportModule;
import com.logistics.pipe.modules.FluidVoidModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * A fluid transport pipe definition — the fluid-family sibling of {@link com.logistics.pipe.ItemPipe}.
 * Composes a {@link FluidTransportModule} (rate + handler policy) plus optional marker modules
 * ({@link FluidMergerModule}, {@link FluidExtractorModule}, {@link FluidVoidModule}) and any cosmetic
 * modules (weathering, marking). The fluid block entity reads the typed policy methods below instead of
 * branching on a kind enum; cosmetic/connection/item-component behavior is inherited from {@link Pipe}.
 *
 * <p>Fluid transport itself (cellular {@code TravelingFluid} movement) stays in {@code FluidPipeBlockEntity};
 * this class only describes per-pipe policy.
 */
public final class FluidPipe extends Pipe {

    public FluidPipe(Module... modules) {
        super(modules);
    }

    @Override
    public FluidPipe withEnergy() {
        super.withEnergy();
        return this;
    }

    // ==================== Transport policy ====================

    /** Transfer rate in mB/tick, from the composed {@link FluidTransportModule}. */
    public long transferRate(LogisticsConfig.FluidPipeConfig cfg) {
        FluidTransportModule transport = getModule(FluidTransportModule.class);
        return transport != null ? transport.transferRate(cfg) : cfg.baseTransferRate;
    }

    /** Buffer capacity in mB — a shared config value across all kinds. */
    public long capacity(LogisticsConfig.FluidPipeConfig cfg) {
        return cfg.baseCapacity;
    }

    /** Whether this pipe connects to external (non-pipe) fluid handlers. */
    public boolean connectsToHandlers() {
        FluidTransportModule transport = getModule(FluidTransportModule.class);
        return transport == null || transport.connectsToHandlers();
    }

    /** True for the insertion pipe: fill adjacent tanks before spilling to other pipes. */
    public boolean prioritizesHandlers() {
        FluidTransportModule transport = getModule(FluidTransportModule.class);
        return transport != null && transport.prioritizesHandlers();
    }

    /** True for the merger check valve (directional, single wrench-set output face). */
    public boolean isMerger() {
        return getModule(FluidMergerModule.class) != null;
    }

    /** True for the powered extractor (pulls from a wrench-set handler face). */
    public boolean isExtractor() {
        return getModule(FluidExtractorModule.class) != null;
    }

    /** True for the void pipe (destroys fluid instead of moving it). */
    public boolean isVoid() {
        return getModule(FluidVoidModule.class) != null;
    }

    /** Whether this pipe uses a single wrench-selected feature face (merger output or extractor pull). */
    public boolean usesFeatureFace() {
        return isMerger() || isExtractor();
    }

    /**
     * Asset base name for the renderer (the leaf under {@code block/fluid/}), derived from the registered
     * block's name. The registry path carries the domain folder (e.g. {@code fluid/copper_fluid_pipe}), so
     * strip it — the renderer already prepends {@code block/fluid/}.
     */
    public String modelBase() {
        String name = getPipeName();
        int slash = name.lastIndexOf('/');
        return slash >= 0 ? name.substring(slash + 1) : name;
    }

    // ==================== Connection veto + render overrides ====================

    /** Returns false if any module vetoes a connection in {@code direction} (e.g. differently-marked pipes). */
    public boolean allowsConnection(@Nullable PipeContext ctx, Direction direction, Block neighborBlock) {
        for (Module module : getStaticModules()) {
            if (!module.allowsConnection(ctx, direction, neighborBlock)) {
                return false;
            }
        }
        return true;
    }

    /** The first module-provided core model override, or {@code null} to use the kind's default core. */
    @Nullable
    public ResourceId coreModelOverride(PipeContext ctx) {
        for (Module module : getStaticModules()) {
            ResourceId override = module.getCoreModel(ctx);
            if (override != null) {
                return override;
            }
        }
        return null;
    }

    /** The first module-provided arm model override for {@code direction}, or {@code null} for the default. */
    @Nullable
    public ResourceId armModelOverride(PipeContext ctx, Direction direction) {
        for (Module module : getStaticModules()) {
            ResourceId override = module.getPipeArm(ctx, direction);
            if (override != null) {
                return override;
            }
        }
        return null;
    }
}
