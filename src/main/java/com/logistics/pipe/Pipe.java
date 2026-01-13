package com.logistics.pipe;

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
        return Identifier.of("logistics", "block/" + getPipeName() + "_core");
    }

    /**
     * Get the model identifier for an arm part in the given direction.
     * Delegates to modules first to allow them to override with custom models (like feature faces).
     * Falls back to the default arm model if no module provides an override.
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm
     * @param extended whether this is the extended version (for inventory connections)
     */
    public Identifier getArmModelId(PipeContext ctx, Direction direction, boolean extended) {
        // Ask modules if they want to override this arm's model
        for (Module module : modules) {
            Identifier override = module.getArmModelId(ctx, direction, extended);
            if (override != null) {
                return override;
            }
        }

        // Default arm model
        String suffix = extended ? "_extension" : "";
        return Identifier.of("logistics", "block/" + getPipeName() + "_" + direction.name().toLowerCase() + suffix);
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

    public float getTargetSpeed(PipeContext ctx) {
        for (Module module : modules) {
            float speed = module.getTargetSpeed(ctx);
            if (speed > 0f) {
                return speed;
            }
        }
        return PipeConfig.BASE_PIPE_SPEED;
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

    public float getMaxSpeed(PipeContext ctx) {
        for (Module module : modules) {
            float max = module.getMaxSpeed(ctx);
            if (max > 0f) {
                return max;
            }
        }
        return 0f;
    }

    public boolean canAccelerate(PipeContext ctx) {
        for (Module module : modules) {
            if (module.applyAcceleration(ctx)) {
                return true;
            }
        }
        return false;
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
