package com.logistics.pipe;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.DestinationPriority;
import com.logistics.core.lib.pipe.FluidPipeBehavior;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.modules.FluidExtractorModule;
import com.logistics.pipe.modules.FluidMergerModule;
import com.logistics.pipe.modules.FluidVoidModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * A fluid transport pipe definition — the fluid-family sibling of {@link com.logistics.pipe.ItemPipe}.
 * Composes single-responsibility {@link FluidPipeBehavior} modules (rate multiplier, bypass, insertion)
 * plus optional mode modules ({@link FluidMergerModule}, {@link FluidExtractorModule},
 * {@link FluidVoidModule}) and cosmetic modules (weathering, marking). Each transport decision is resolved
 * from a default plus module overrides, so the block entity asks a domain question instead of branching on
 * a kind enum; cosmetic/connection/item-component behavior is inherited from {@link Pipe}.
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

    /** Transfer rate in mB/tick: the configured base rate folded through each behavior's rate modifier. */
    public long transferRate() {
        long rate = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PIPE_BASE_TRANSFER_RATE);
        for (Module module : getStaticModules()) {
            if (module instanceof FluidPipeBehavior behavior) {
                rate = behavior.modifyTransferRate(rate);
            }
        }
        return rate;
    }

    /** Buffer capacity in mB — a shared config value across all kinds. */
    public long capacity() {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PIPE_BASE_CAPACITY);
    }

    /** Whether this pipe connects to external (non-pipe) fluid handlers — true unless a behavior vetoes it. */
    public boolean canConnectToFluidHandler() {
        for (Module module : getStaticModules()) {
            if (module instanceof FluidPipeBehavior behavior && !behavior.canConnectToFluidHandler()) {
                return false;
            }
        }
        return true;
    }

    /** Where this pipe prefers to send fluid at a junction — {@code NORMAL} unless a behavior overrides it. */
    public DestinationPriority destinationPriority() {
        DestinationPriority priority = DestinationPriority.NORMAL;
        for (Module module : getStaticModules()) {
            if (module instanceof FluidPipeBehavior behavior) {
                priority = behavior.destinationPriority(priority);
            }
        }
        return priority;
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
     * Asset base name for the renderer (the leaf under {@code block/pipe/}), derived from the registered
     * block's name. The registry path carries the domain folder (e.g. {@code pipe/copper_fluid_pipe}), so
     * strip it — the renderer already prepends {@code block/pipe/}.
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
