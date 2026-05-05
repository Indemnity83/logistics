package com.logistics.pipe;

import com.logistics.LogisticsMod;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.CoreDecoration;
import com.logistics.core.lib.pipe.DispatchableModule;
import com.logistics.core.lib.pipe.ItemAcceptingModule;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.RandomTickModule;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.RoutingModule;
import com.logistics.core.lib.pipe.TransferHandlerModule;
import com.logistics.core.lib.pipe.TravelingItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public class Pipe {
    private final List<Module> modules;
    private PipeBlock pipeBlock;
    private boolean hasEnergy = false;

    protected Pipe(Module... modules) {
        this.modules = List.of(modules);
    }

    protected List<Module> getStaticModules() {
        return modules;
    }

    /**
     * Marks this pipe as having energy storage capability.
     * Only pipes with this flag will have energy storage created.
     */
    public Pipe withEnergy() {
        this.hasEnergy = true;
        return this;
    }

    /**
     * Returns whether this pipe has energy storage capability.
     */
    public boolean hasEnergy() {
        return hasEnergy;
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
    public ResourceId getCoreModelId(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            ResourceId override = module.getCoreModel(ctx);
            if (override != null) {
                return override;
            }
        }
        return LogisticsMod.modId("block/" + getPipeName() + "_core");
    }

    /**
     * Collect core decoration models from pipe modules.
     */
    public List<CoreDecoration> getCoreDecorations(PipeContext ctx) {
        List<CoreDecoration> models = new ArrayList<>();
        for (Module module : getModules(ctx)) {
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
    public ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        for (Module module : getModules(ctx)) {
            ResourceId override = module.getPipeArm(ctx, direction);
            if (override != null) {
                return override;
            }
        }

        String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return LogisticsMod.modId("block/" + getPipeName() + suffix);
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
    public List<ResourceId> getPipeDecorations(PipeContext ctx, Direction direction) {
        List<ResourceId> models = new ArrayList<>();
        for (Module module : getModules(ctx)) {
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
        for (Module module : getModules(ctx)) {
            Integer tint = module.getArmTint(ctx, direction);
            if (tint != null) {
                return tint;
            }
        }
        return null;
    }

    public boolean hasRandomTicks() {
        for (Module module : modules) {
            if (module instanceof RandomTickModule) {
                return true;
            }
        }
        return false;
    }

    public void randomTick(PipeContext ctx, RandomSource random) {
        for (Module module : getModules(ctx)) {
            if (module instanceof RandomTickModule randomTick) {
                randomTick.randomTick(ctx, random);
            }
        }
    }

    // --- Item component helpers ---

    /**
     * Add item components from all modules when the block is broken.
     * Also adds custom model data component if any module provides model data strings.
     */
    public void addItemComponents(DataComponentMap.Builder builder, PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            module.addItemComponents(builder, ctx);
        }

        // Aggregate custom model data strings from all modules
        List<String> modelStrings = new ArrayList<>();
        for (Module module : getModules(ctx)) {
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
        for (Module module : getModules(ctx)) {
            module.readItemComponents(components, ctx);
        }
    }

    /**
     * Get the item name suffix from the first module that provides one.
     */
    public String getItemNameSuffix(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
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
    public String getModelBasePath(Direction direction) {
        return "block/" + getPipeName() + "_" + direction.name().toLowerCase();
    }

    protected <T extends Module> T getModule(Class<T> moduleClass) {
        for (Module module : modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module);
            }
        }
        return null;
    }

    /**
     * Returns a module of the given type, considering dynamic modules when a block entity
     * is available. Subclasses (e.g. ChassisPipe) override this to support runtime-installed
     * modules that cannot be found in the static module list.
     */
    public <T extends Module> T getModule(Class<T> moduleClass, PipeBlockEntity entity) {
        return getModule(moduleClass);
    }

    public <T extends Module> T getModule(
            Class<T> moduleClass, PipeBlockEntity entity, @Nullable String stateKey) {
        if (stateKey == null) {
            return getModule(moduleClass, entity);
        }

        for (Module module : getModules(entity)) {
            if (moduleClass.isInstance(module) && stateKey.equals(module.getStateKey())) {
                return moduleClass.cast(module);
            }
        }
        return null;
    }

    public boolean matchesSinkFilter(PipeContext ctx, ItemStack stack) {
        for (Module module : getModules(ctx)) {
            if (module instanceof ItemAcceptingModule sink && sink.acceptsItem(ctx, stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all modules for this pipe. For chassis pipes, includes dynamically-installed modules.
     * Used when iterating all modules (e.g., to find all ItemAcceptingModules).
     */
    public List<Module> getModules(PipeBlockEntity entity) {
        return modules;
    }

    /**
     * Returns all modules for this pipe using the context's block entity.
     * Dispatches polymorphically so chassis pipe overrides are respected in all base-class methods.
     */
    private List<Module> getModules(PipeContext ctx) {
        if (ctx.blockEntity() instanceof PipeBlockEntity pbe) {
            return getModules(pbe);
        }
        return modules;
    }

    public float getAccelerationRate(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            float accel = module.getAcceleration(ctx);
            if (accel > 0f) {
                return accel;
            }
        }
        return 0f;
    }

    public float getDrag(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            float drag = module.getDrag(ctx);
            if (drag > 0f) {
                return drag;
            }
        }
        return LogisticsConfig.get().pipe.drag;
    }

    public float getMaxSpeed(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            float max = module.getMaxSpeed(ctx);
            if (max > 0f) {
                return max;
            }
        }
        return LogisticsConfig.get().pipe.maxSpeed;
    }

    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        for (Module module : getModules(ctx)) {
            if (module instanceof RoutingModule router) {
                RoutePlan plan = router.route(ctx, item, options);
                if (plan.getType() != RoutePlan.Type.PASS) {
                    return plan;
                }
            }
        }

        return RoutePlan.pass();
    }

    /**
     * Give modules a chance to handle transfer of a TravelingItem into an adjacent non-pipe
     * storage. Returns null if a module fully handled the transfer (PipeRuntime skips generic
     * insertion), or the item (possibly modified) if no module handled it.
     */
    @Nullable
    public TravelingItem handleTransfer(PipeContext ctx, TravelingItem item, Direction direction) {
        TravelingItem current = item;
        for (Module module : getModules(ctx)) {
            if (module instanceof TransferHandlerModule handler) {
                current = handler.onTransferToStorage(ctx, current, direction);
                if (current == null) return null;
            }
        }
        return current;
    }

    /**
     * Called when an item is inserted from an external (non-pipe) source before the default
     * single TravelingItem is created. Delegates to modules in order; stops at the first that
     * returns true (handled).
     */
    public boolean onExternalInsert(PipeContext ctx, net.minecraft.world.item.ItemStack stack, Direction from) {
        for (Module module : getModules(ctx)) {
            if (module.onExternalInsert(ctx, stack, from)) return true;
        }
        return false;
    }

    public boolean canAcceptFrom(PipeContext ctx, Direction from, net.minecraft.world.item.ItemStack stack) {
        // Default behavior: pipes only accept items from other pipes (not from inventories/hoppers)
        // This prevents free automation and preserves the extraction energy cost
        if (!ctx.isNeighborPipe(from)) {
            // Allow non-pipe insertion if any module explicitly permits it
            boolean allowed = false;
            for (Module module : getModules(ctx)) {
                if (module.canAcceptFromNonPipe(ctx, from)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) return false;
        }

        // Check all modules for additional acceptance criteria
        for (Module module : getModules(ctx)) {
            if (!module.canAcceptFrom(ctx, from, stack)) {
                return false;
            }
        }
        return true;
    }

    public InteractionResult onUseWithItem(PipeContext ctx, net.minecraft.world.item.context.UseOnContext usage) {
        for (Module module : getModules(ctx)) {
            InteractionResult result = module.onUseWithItem(ctx, usage);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onUseWithoutItem(PipeContext ctx, net.minecraft.world.item.context.UseOnContext usage) {
        for (Module module : getModules(ctx)) {
            InteractionResult result = module.onUseWithoutItem(ctx, usage);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public InteractionResult onWrench(PipeContext ctx, Player player) {
        for (Module module : getModules(ctx)) {
            InteractionResult result = module.onWrench(ctx, player);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public void onConnectionsChanged(PipeContext ctx, List<Direction> connected) {
        for (Module module : getModules(ctx)) {
            module.onConnectionsChanged(ctx, connected);
        }
    }

    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;
        for (Module module : getModules(ctx)) {
            if (module instanceof TickingModule ticking) {
                ticking.onTick(ctx);
            }
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
        for (Module module : getModules(ctx)) {
            output = Math.max(output, module.comparatorOutput(ctx));
        }
        return output;
    }

    public void randomDisplayTick(PipeContext ctx, RandomSource random) {
        for (Module module : getModules(ctx)) {
            module.randomDisplayTick(ctx, random);
        }
    }

    public PipeConnection.Type filterConnection(
            @Nullable PipeContext ctx, Direction direction, Block neighborBlock, PipeConnection.Type candidate) {
        if (candidate == PipeConnection.Type.NONE) {
            return PipeConnection.Type.NONE;
        }
        for (Module module : (ctx != null ? getModules(ctx) : modules)) {
            if (!module.allowsConnection(ctx, direction, neighborBlock)) {
                return PipeConnection.Type.NONE;
            }
        }
        return candidate;
    }

    /**
     * Dispatch items to a requester by asking the first module that can fulfill to extract.
     * Called by MinecraftWorldView.dispatch() during the network tick.
     *
     * @return actual amount dispatched (&gt;0), -1 if the provider is temporarily busy (deferred),
     *         or 0 if no module could fulfill
     */
    public long dispatch(PipeContext ctx, BlockPos requester, ItemVariant item, long amount, UUID deliveryId) {
        if (ctx.world().isClientSide()) return 0;
        for (Module module : getModules(ctx)) {
            if (module instanceof DispatchableModule dispatchable) {
                long dispatched = dispatchable.onDispatch(ctx, requester, item, amount, deliveryId);
                if (dispatched != 0) return dispatched; // propagate both success (>0) and deferred (<0)
            }
        }
        return 0;
    }

    // Energy capability check for low-tier energy sources
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        // Only accept low-tier energy if this pipe has energy storage
        if (!hasEnergy) {
            return false;
        }
        // Delegate to modules for additional logic
        for (Module module : getModules(ctx)) {
            if (module.acceptsLowTierEnergyFrom(ctx, from)) {
                return true;
            }
        }
        return false;
    }
}
