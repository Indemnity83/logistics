package com.logistics.pipe;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.pipe.PipeConnection;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.modules.Module;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.RoutePlan;
import com.logistics.pipe.runtime.TravelingItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

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
     * Get the registry name of this pipe (e.g., "pipe/copper_transport_pipe").
     */
    public String getPipeName() {
        if (pipeBlock == null) {
            throw new IllegalStateException("Pipe has not been registered yet");
        }
        return BuiltInRegistries.BLOCK.getKey(pipeBlock).getPath();
    }

    /**
     * Get the model identifier for the core part of this pipe.
     * Delegates to modules first to allow state-dependent overrides (e.g., powered gold pipe).
     */
    public Identifier getCoreModelId(PipeContext ctx) {
        for (Module module : modules) {
            Identifier override = module.getCoreModel(ctx);
            if (override != null) {
                return override;
            }
        }
        return Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/" + getPipeName() + "_core");
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
     * Get the arm model identifier for the given direction.
     * Delegates to modules first to allow them to override with custom models (like feature faces).
     * Falls back to the base arm model if no module provides an override.
     * The returned model is NORTH-facing and should be rotated at render time.
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm
     * @return the arm model identifier
     */
    public Identifier getPipeArm(PipeContext ctx, Direction direction) {
        for (Module module : modules) {
            Identifier override = module.getPipeArm(ctx, direction);
            if (override != null) {
                return override;
            }
        }

        String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, "block/" + getPipeName() + suffix);
    }

    /**
     * Get decoration model identifiers for an arm in the given direction.
     * Decorations are additive parts rendered alongside the core arm model,
     * such as module-provided overlays.
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm
     * @return a (possibly empty) list of decoration model identifiers
     */
    public List<Identifier> getPipeDecorations(PipeContext ctx, Direction direction) {
        List<Identifier> models = new ArrayList<>();
        for (Module module : modules) {
            models.addAll(module.getPipeDecorations(ctx, direction));
        }
        return models;
    }

    /**
     * Get the tint color for the arm in the given direction.
     * Delegates to modules to allow directional coloring (e.g., filter pipe overlays).
     *
     * @param ctx the pipe context
     * @param direction the direction of the arm
     * @return the tint color (0xRRGGBB), or null for no tint (white)
     */
    @Nullable public Integer getArmTint(PipeContext ctx, Direction direction) {
        for (Module module : modules) {
            Integer tint = module.getArmTint(ctx, direction);
            if (tint != null) {
                return tint;
            }
        }
        return null;
    }

    public boolean hasRandomTicks() {
        for (Module module : modules) {
            if (module.hasRandomTicks()) {
                return true;
            }
        }
        return false;
    }

    public void randomTick(PipeContext ctx, RandomSource random) {
        for (Module module : modules) {
            module.randomTick(ctx, random);
        }
    }

    // --- Item component helpers ---

    /**
     * Add item components from all modules when the block is broken.
     * Also adds custom model data component if any module provides model data strings.
     */
    public void addItemComponents(DataComponentMap.Builder builder, PipeContext ctx) {
        for (Module module : modules) {
            module.addItemComponents(builder, ctx);
        }

        // Aggregate custom model data strings from all modules
        List<String> modelStrings = new ArrayList<>();
        for (Module module : modules) {
            modelStrings.addAll(module.getCustomModelDataStrings(ctx));
        }
        if (!modelStrings.isEmpty()) {
            builder.set(
                    DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(List.of(), List.of(), modelStrings, List.of()));
        }
    }

    /**
     * Read item components into all modules when the block is placed.
     */
    public void readItemComponents(DataComponentGetter components, PipeContext ctx) {
        for (Module module : modules) {
            module.readItemComponents(components, ctx);
        }
    }

    /**
     * Get the item name suffix from the first module that provides one.
     */
    public String getItemNameSuffix(PipeContext ctx) {
        for (Module module : modules) {
            String suffix = module.getItemNameSuffix(ctx);
            if (!suffix.isEmpty()) {
                return suffix;
            }
        }
        return "";
    }

    /**
     * Get the item name suffix from item components.
     * Used for item display names when we don't have a block context.
     */
    public String getItemNameSuffixFromComponents(DataComponentGetter components) {
        for (Module module : modules) {
            String suffix = module.getItemNameSuffixFromComponents(components);
            if (!suffix.isEmpty()) {
                return suffix;
            }
        }
        return "";
    }

    /**
     * Append creative menu variants from all modules.
     */
    public void appendCreativeMenuVariants(
            List<net.minecraft.world.item.ItemStack> stacks, net.minecraft.world.item.ItemStack baseStack) {
        for (Module module : modules) {
            module.appendCreativeMenuVariants(stacks, baseStack);
        }
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

    public boolean canAcceptFrom(PipeContext ctx, Direction from, net.minecraft.world.item.ItemStack stack) {
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

    public InteractionResult onUseWithItem(PipeContext ctx, net.minecraft.world.item.context.UseOnContext usage) {
        for (Module module : modules) {
            InteractionResult result = module.onUseWithItem(ctx, usage);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onWrench(PipeContext ctx, Player player) {
        for (Module module : modules) {
            InteractionResult result = module.onWrench(ctx, player);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
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

    public void randomDisplayTick(PipeContext ctx, RandomSource random) {
        for (Module module : modules) {
            module.randomDisplayTick(ctx, random);
        }
    }

    public PipeConnection.Type filterConnection(
            @Nullable PipeContext ctx, Direction direction, Block neighborBlock, PipeConnection.Type candidate) {
        if (candidate == PipeConnection.Type.NONE) {
            return PipeConnection.Type.NONE;
        }
        for (Module module : modules) {
            if (!module.allowsConnection(ctx, direction, this, neighborBlock)) {
                return PipeConnection.Type.NONE;
            }
        }
        return candidate;
    }

    // --- Energy delegation ---

    public long getEnergyAmount(PipeContext ctx) {
        for (Module module : modules) {
            long amount = module.getEnergyAmount(ctx);
            if (amount > 0) {
                return amount; // Return first non-zero
            }
        }
        return 0;
    }

    public long getEnergyCapacity(PipeContext ctx) {
        for (Module module : modules) {
            long capacity = module.getEnergyCapacity(ctx);
            if (capacity > 0) {
                return capacity; // Return first non-zero
            }
        }
        return 0;
    }

    public long insertEnergy(PipeContext ctx, long maxAmount, boolean simulate) {
        for (Module module : modules) {
            if (module.canInsertEnergy(ctx)) {
                return module.insertEnergy(ctx, maxAmount, simulate);
            }
        }
        return 0;
    }

    public long extractEnergy(PipeContext ctx, long maxAmount, boolean simulate) {
        for (Module module : modules) {
            if (module.canExtractEnergy(ctx)) {
                return module.extractEnergy(ctx, maxAmount, simulate);
            }
        }
        return 0;
    }

    public boolean canInsertEnergy(PipeContext ctx) {
        for (Module module : modules) {
            if (module.canInsertEnergy(ctx)) {
                return true;
            }
        }
        return false;
    }

    public boolean canExtractEnergy(PipeContext ctx) {
        for (Module module : modules) {
            if (module.canExtractEnergy(ctx)) {
                return true;
            }
        }
        return false;
    }

    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        for (Module module : modules) {
            if (module.canInsertEnergy(ctx)) {
                return module.acceptsLowTierEnergyFrom(ctx, from);
            }
        }
        return false; // No energy-capable modules installed
    }
}
