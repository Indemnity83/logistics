package com.logistics.pipe;

import com.logistics.LogisticsMod;
import com.logistics.pipe.modules.Module;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.block.PipeBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class Pipe {
    private final List<Module> modules;
    private PipeBlock pipeBlock;

    protected Pipe(Module... modules) {
        this.modules = List.of(modules);
    }

    /**
     * Called by PipeBlock during registration to establish back-reference.
     * This allows the Pipe to derive model identifiers from the block's registry name.
     */
    public void setPipeBlock(PipeBlock block) {
        this.pipeBlock = block;
    }

    /**
     * Get the registry name of this pipe (e.g., "copper_transport_pipe").
     */
    public String getPipeName() {
        if (pipeBlock == null) {
            throw new IllegalStateException("Pipe has not been registered yet");
        }
        return Registries.BLOCK.getId(pipeBlock).getPath();
    }

    /**
     * Get the model identifier for the core part of this pipe.
     */
    public Identifier getCoreModelId() {
        return Identifier.of(LogisticsMod.MOD_ID, "block/" + getPipeName() + "_core");
    }

    /**
     * Collect core decoration models from pipe modules.
     */
    public List<CoreDecoration> getCoreDecorations(PipeContext ctx) {
        List<CoreDecoration> models = new ArrayList<>();
        for (Module module : modules) {
            models.addAll(module.getCoreDecorations(ctx));
        }
        return models;
    }

    /**
     * Get the model identifier for an arm part in the given direction.
     * Delegates to modules first to allow them to override with custom models (like feature faces).
     * Falls back to the default arm model if no module provides an override.
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm
     */
    public Identifier getPipeArm(PipeContext ctx, Direction direction) {
        for (Module module : modules) {
            Identifier override = module.getPipeArm(ctx, direction);
            if (override != null) {
                return override;
            }
        }

        return Identifier.of(LogisticsMod.MOD_ID, getModelBasePath(direction));
    }

    /**
     * Get decoration model identifiers for an arm in the given direction.
     * Decorations are additive parts rendered alongside the core arm model, such as:
     * - the default extension when the pipe is connected to an inventory on that face
     * - module-provided overlays / feature faces / extras
     *
     * TODO: Extensions are currently treated as decorations.
     *  This can result in both a default extension and a module-provided
     *  extension being rendered. This is intentional for now.
     *  Long-term, extension should be handled by the arm model itself.
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm
     * @return a (possibly empty) list of decoration model identifiers
     */
    public List<Identifier> getPipeDecorations(PipeContext ctx, Direction direction) {
        List<Identifier> models = new ArrayList<>();

        if (ctx.isInventoryConnection(direction)) {
            models.add(Identifier.of(LogisticsMod.MOD_ID, getModelBasePath(direction) + "_extension"));
        }

        for (Module module : modules) {
            models.addAll(module.getPipeDecorations(ctx, direction)); // empty list => no-op
        }

        return models;
    }

    /**
     * Core overlay model with an optional tint color.
     */
    public record CoreDecoration(Identifier modelId, int color) {}

    public String getModelBasePath(Direction direction) {
        return "block/" + getPipeName() + "_" + direction.name().toLowerCase();
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> moduleClass) {
        for (Module module : modules) {
            if (moduleClass.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }

    public float getAccelerationRate(PipeContext ctx) {
        for (Module module : modules) {
            float accel = module.getAcceleration(ctx);
            if (accel > 0f) {
                return accel;
            }
        }
        return 0f;
    }

    public float getDrag(PipeContext ctx) {
        for (Module module : modules) {
            float drag = module.getDrag(ctx);
            if (drag > 0f) {
                return drag;
            }
        }
        return PipeConfig.DRAG_COEFFICIENT;
    }

    public float getMaxSpeed(PipeContext ctx) {
        for (Module module : modules) {
            float max = module.getMaxSpeed(ctx);
            if (max > 0f) {
                return max;
            }
        }
        return PipeConfig.PIPE_MAX_SPEED;
    }

    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        for (Module module : modules) {
            RoutePlan plan = module.route(ctx, item, options);
            if (plan.getType() != RoutePlan.Type.PASS) {
                return plan;
            }
        }

        return RoutePlan.pass();
    }

    public boolean canAcceptFrom(PipeContext ctx, Direction from, net.minecraft.item.ItemStack stack) {
        // Default behavior: pipes only accept items from other pipes (not from inventories/hoppers)
        // This prevents free automation and preserves the extraction energy cost
        if (!ctx.isNeighborPipe(from)) {
            return false;
        }

        // Check all modules for additional acceptance criteria
        for (Module module : modules) {
            if (!module.canAcceptFrom(ctx, from, stack)) {
                return false;
            }
        }
        return true;
    }

    public boolean discardWhenNoRoute(PipeContext ctx) {
        for (Module module : modules) {
            if (module.discardWhenNoRoute(ctx)) {
                return true;
            }
        }
        return false;
    }

    public void onWrenchUse(PipeContext ctx, net.minecraft.item.ItemUsageContext usage) {
        for (Module module : modules) {
            module.onWrenchUse(ctx, usage);
        }
    }

    public net.minecraft.util.ActionResult onUseWithItem(PipeContext ctx, net.minecraft.item.ItemUsageContext usage) {
        for (Module module : modules) {
            net.minecraft.util.ActionResult result = module.onUseWithItem(ctx, usage);
            if (result != net.minecraft.util.ActionResult.PASS) {
                return result;
            }
        }
        return net.minecraft.util.ActionResult.PASS;
    }

    public void onConnectionsChanged(PipeContext ctx, List<Direction> connected) {
        for (Module module : modules) {
            module.onConnectionsChanged(ctx, connected);
        }
    }

    public void onTick(PipeContext ctx) {
        for (Module module : modules) {
            module.onTick(ctx);
        }
    }

    public boolean hasComparatorOutput() {
        for (Module module : modules) {
            if (module.hasComparatorOutput()) {
                return true;
            }
        }
        return false;
    }

    public int getComparatorOutput(PipeContext ctx) {
        int output = 0;
        for (Module module : modules) {
            output = Math.max(output, module.comparatorOutput(ctx));
        }
        return output;
    }

    public void randomDisplayTick(PipeContext ctx, Random random) {
        for (Module module : modules) {
            module.randomDisplayTick(ctx, random);
        }
    }

    public PipeBlock.ConnectionType filterConnection(@Nullable PipeContext ctx, Direction direction, Block neighborBlock,
                                                     PipeBlock.ConnectionType candidate) {
        if (candidate == PipeBlock.ConnectionType.NONE) {
            return PipeBlock.ConnectionType.NONE;
        }
        for (Module module : modules) {
            if (!module.allowsConnection(ctx, direction, this, neighborBlock)) {
                return PipeBlock.ConnectionType.NONE;
            }
        }
        return candidate;
    }
}
