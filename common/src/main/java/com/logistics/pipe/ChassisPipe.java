package com.logistics.pipe;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsConfigHost.Configs;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.*;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.core.lib.pipe.RandomTickModule;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.RoutingModule;
import com.logistics.core.lib.pipe.TransferHandlerModule;
import com.logistics.core.lib.pipe.TravelingItem;
import net.minecraft.util.RandomSource;
import com.logistics.pipe.ui.ChassisScreenHandler;
import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A Pipe subclass for chassis logistics pipes.
 * Supports a configurable number of dynamically-installed module slots.
 *
 * <p>The chassis pipe composes two layers of modules:
 * <ul>
 *   <li><b>Fixed modules</b> (set at construction): e.g. NetworkRouterModule</li>
 *   <li><b>Dynamic modules</b> (installed in-game via the chassis GUI): resolved at runtime</li>
 * </ul>
 *
 * <p>Dynamic modules are consulted before fixed modules for routing, ticking, and rendering.
 */
public class ChassisPipe extends ItemPipe {
    /** Maximum number of module slots across all chassis marks. */
    public static final int MAX_SLOTS = 8;

    /** NBT key used to store chassis slot configuration in the pipe's module state. */
    public static final String STATE_KEY = "chassispipe";

    private final int maxSlots;

    public static String moduleStateKey(ItemStack stack, Module module) {
        return ModuleItem.moduleStateKey(stack, module);
    }

    public record DynamicModule(Module module, String stateKey) {
        public PipeContext scopedContext(PipeContext ctx) {
            return ctx.withModuleStateKey(module, stateKey());
        }
    }

    public ChassisPipe(int maxSlots, Module... fixedModules) {
        super(fixedModules);
        this.maxSlots = maxSlots;
    }

    /** Returns the number of active module slots for this chassis mark (1, 2, 3, 4, or 8). */
    public int getMaxSlots() {
        return maxSlots;
    }

    @Override
    public <T extends Module> T getModule(Class<T> moduleClass, PipeBlockEntity entity) {
        PipeContext ctx = entity.createContext();
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Module module = entry.module();
            if (moduleClass.isInstance(module)) return moduleClass.cast(module);
        }
        return super.getModule(moduleClass);
    }

    @Override
    public <T extends Module> T getModule(
            Class<T> moduleClass, PipeBlockEntity entity, @Nullable String stateKey) {
        if (stateKey == null) {
            return getModule(moduleClass, entity);
        }

        PipeContext ctx = entity.createContext();
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Module module = entry.module();
            if (moduleClass.isInstance(module) && stateKey.equals(entry.stateKey())) {
                return moduleClass.cast(module);
            }
        }
        return super.getModule(moduleClass, entity, stateKey);
    }

    @Override
    public List<Module> getModules(PipeBlockEntity entity) {
        PipeContext ctx = entity.createContext();
        List<Module> all = new ArrayList<>(getDynamicModules(ctx));
        all.addAll(super.getModules(entity));
        return all;
    }

    /**
     * Returns the dynamically-installed modules for this chassis pipe.
     * Reads each slot's ItemStack from NBT; slots holding a {@link ModuleItem}
     * contribute an active module instance.
     */
    protected List<Module> getDynamicModules(PipeContext ctx) {
        List<Module> modules = new ArrayList<>();
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            modules.add(entry.module());
        }
        return modules;
    }

    /**
     * Loads the module from a chassis slot, assigning a persistent module ID if one is missing.
     * Returns empty if the slot is unoccupied or does not hold a {@link ModuleItem}.
     */
    public static Optional<DynamicModule> loadSlot(PipeContext ctx, int slotIndex, RegistryOps<Tag> ops) {
        var state = ctx.moduleState(STATE_KEY);
        String slotKey = String.valueOf(slotIndex);
        Tag tag = state.get(slotKey);
        if (tag == null) return Optional.empty();

        return ItemStack.CODEC.parse(ops, tag).result().flatMap(stack -> {
            if (!(stack.getItem() instanceof ModuleItem moduleItem)) return Optional.empty();
            boolean missingModuleId = ModuleItem.getModuleId(stack).isBlank();
            Module module = moduleItem.createModule();
            String stateKey = moduleStateKey(stack, module);
            // Assigning the persistent module id is a server-side mutation; never write/sync from a
            // client read (e.g. a HUD enumerating modules). Client state already carries the synced id.
            if (missingModuleId && !ctx.world().isClientSide()) {
                ItemStack.CODEC.encodeStart(ops, stack).result()
                        .ifPresent(encoded -> state.put(slotKey, encoded));
                ctx.markDirtyAndSync();
            }
            return Optional.of(new DynamicModule(module, stateKey));
        });
    }

    private List<DynamicModule> getDynamicModuleEntries(PipeContext ctx) {
        List<DynamicModule> modules = new ArrayList<>();
        RegistryOps<Tag> ops = ctx.world().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (int slot = 0; slot < maxSlots; slot++) {
            loadSlot(ctx, slot, ops).ifPresent(modules::add);
        }
        return modules;
    }

    /** Returns the module item stacks currently installed in this chassis, for display (e.g. a HUD). */
    public List<ItemStack> getInstalledModuleStacks(PipeContext ctx) {
        List<ItemStack> stacks = new ArrayList<>();
        var state = ctx.moduleState(STATE_KEY);
        RegistryOps<Tag> ops = ctx.world().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (int slot = 0; slot < maxSlots; slot++) {
            Tag tag = state.get(String.valueOf(slot));
            if (tag == null) continue;
            ItemStack.CODEC.parse(ops, tag).result().ifPresent(stack -> {
                if (stack.getItem() instanceof ModuleItem) stacks.add(stack);
            });
        }
        return stacks;
    }

    /**
     * Chassis HUD: on the details key, let each installed module contribute its own config lines (scoped
     * to its state). The always-shown row of installed module icons is rendered by the loader Jade
     * provider (icons are a Jade UI element, not a text component). Fixed modules (e.g. the network
     * router) carry no display state and contribute nothing.
     */
    @Override
    public void appendHud(PipeContext ctx, PipeHud hud) {
        if (hud.showDetails()) {
            for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
                entry.module().appendHud(entry.scopedContext(ctx), hud);
            }
            for (Module module : getStaticModules()) {
                module.appendHud(ctx, hud);
            }
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // Routing / ticking — dynamic modules run BEFORE fixed modules
    // -------------------------------------------------------------------------

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Module module = entry.module();
            if (module instanceof RoutingModule router) {
                RoutePlan plan = router.route(entry.scopedContext(ctx), item, options);
                if (plan.getType() != RoutePlan.Type.PASS) return plan;
            }
        }
        for (Module module : getStaticModules()) {
            if (module instanceof RoutingModule router) {
                RoutePlan plan = router.route(ctx, item, options);
                if (plan.getType() != RoutePlan.Type.PASS) return plan;
            }
        }
        return RoutePlan.pass();
    }

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Module module = entry.module();
            if (module instanceof TickingModule ticking) {
                ticking.onTick(entry.scopedContext(ctx));
            }
        }
        for (Module module : getStaticModules()) {
            if (module instanceof TickingModule ticking) {
                ticking.onTick(ctx);
            }
        }
    }

    @Override
    public long dispatch(PipeContext ctx, BlockPos requester, IItemKey item, long amount, UUID deliveryId) {
        if (ctx.world().isClientSide()) return 0;
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Module module = entry.module();
            if (module instanceof DispatchableModule dispatchable) {
                long d = dispatchable.onDispatch(entry.scopedContext(ctx), requester, item, amount, deliveryId);
                if (d != 0) return d;
            }
        }
        for (Module module : getStaticModules()) {
            if (module instanceof DispatchableModule dispatchable) {
                long dispatched = dispatchable.onDispatch(ctx, requester, item, amount, deliveryId);
                if (dispatched != 0) return dispatched;
            }
        }
        return 0;
    }

    /**
     * Returns {@code true} whenever this chassis has slots, because any slot could hold a
     * {@link RandomTickModule}. The overhead of scheduling random ticks on empty chassis pipes is
     * negligible; the alternative would require reading NBT here without a PipeBlockEntity param.
     */
    @Override
    public boolean hasRandomTicks() {
        return maxSlots > 0 || super.hasRandomTicks();
    }

    @Override
    public void randomTick(PipeContext ctx, RandomSource random) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Module module = entry.module();
            if (module instanceof RandomTickModule rt) {
                rt.randomTick(entry.scopedContext(ctx), random);
            }
        }
        for (Module module : getStaticModules()) {
            if (module instanceof RandomTickModule randomTick) {
                randomTick.randomTick(ctx, random);
            }
        }
    }

    @Override
    public TravelingItem handleTransfer(PipeContext ctx, TravelingItem item, Direction direction) {
        TravelingItem current = item;
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Module module = entry.module();
            if (module instanceof TransferHandlerModule handler) {
                current = handler.onTransferToStorage(entry.scopedContext(ctx), current, direction);
                if (current == null) return null;
            }
        }
        for (Module module : getStaticModules()) {
            if (module instanceof TransferHandlerModule handler) {
                current = handler.onTransferToStorage(ctx, current, direction);
                if (current == null) return null;
            }
        }
        return current;
    }

    @Override
    public boolean matchesSinkFilter(PipeContext ctx, ItemStack stack) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Module module = entry.module();
            if (module instanceof ItemAcceptingModule sink && sink.acceptsItem(entry.scopedContext(ctx), stack)) {
                return true;
            }
        }
        for (Module module : getStaticModules()) {
            if (module instanceof ItemAcceptingModule sink && sink.acceptsItem(ctx, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onExternalInsert(PipeContext ctx, ItemStack stack, Direction from) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            if (entry.module().onExternalInsert(entry.scopedContext(ctx), stack, from)) return true;
        }
        for (Module module : getStaticModules()) {
            if (module.onExternalInsert(ctx, stack, from)) return true;
        }
        return false;
    }

    @Override
    public boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        if (!ctx.isNeighborPipe(from)) {
            boolean allowed = false;
            for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
                if (entry.module().canAcceptFromNonPipe(entry.scopedContext(ctx), from)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                for (Module module : getStaticModules()) {
                    if (module.canAcceptFromNonPipe(ctx, from)) {
                        allowed = true;
                        break;
                    }
                }
            }
            if (!allowed) return false;
        }

        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            if (!entry.module().canAcceptFrom(entry.scopedContext(ctx), from, stack)) return false;
        }
        for (Module module : getStaticModules()) {
            if (!module.canAcceptFrom(ctx, from, stack)) return false;
        }
        return true;
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> connected) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            entry.module().onConnectionsChanged(entry.scopedContext(ctx), connected);
        }
        for (Module module : getStaticModules()) {
            module.onConnectionsChanged(ctx, connected);
        }
    }

    // -------------------------------------------------------------------------
    // Rendering — merge dynamic module decorations with fixed modules
    // -------------------------------------------------------------------------

    @Override
    public List<CoreDecoration> getCoreDecorations(PipeContext ctx) {
        List<CoreDecoration> result = new ArrayList<>();
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            result.addAll(entry.module().getCoreDecorations(entry.scopedContext(ctx)));
        }
        for (Module module : getStaticModules()) {
            result.addAll(module.getCoreDecorations(ctx));
        }
        return result;
    }

    @Override
    public List<ResourceId> getPipeDecorations(PipeContext ctx, Direction direction) {
        List<ResourceId> result = new ArrayList<>();
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            result.addAll(entry.module().getPipeDecorations(entry.scopedContext(ctx), direction));
        }
        for (Module module : getStaticModules()) {
            result.addAll(module.getPipeDecorations(ctx, direction));
        }
        return result;
    }

    @Override
    public Integer getArmTint(PipeContext ctx, Direction direction) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            Integer tint = entry.module().getArmTint(entry.scopedContext(ctx), direction);
            if (tint != null) return tint;
        }
        for (Module module : getStaticModules()) {
            Integer tint = module.getArmTint(ctx, direction);
            if (tint != null) return tint;
        }
        return null;
    }

    @Override
    public ResourceId getCoreModelId(PipeContext ctx) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            ResourceId override = entry.module().getCoreModel(entry.scopedContext(ctx));
            if (override != null) return override;
        }
        for (Module module : getStaticModules()) {
            ResourceId override = module.getCoreModel(ctx);
            if (override != null) return override;
        }
        return LogisticsMod.modId("block/" + getPipeName() + "_core");
    }

    @Override
    public ResourceId getPipeArm(PipeContext ctx, Direction direction) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            ResourceId override = entry.module().getPipeArm(entry.scopedContext(ctx), direction);
            if (override != null) return override;
        }
        for (Module module : getStaticModules()) {
            ResourceId override = module.getPipeArm(ctx, direction);
            if (override != null) return override;
        }

        String suffix = ctx.isInventoryConnection(direction) ? "_arm_extended" : "_arm";
        return LogisticsMod.modId("block/" + getPipeName() + suffix);
    }

    @Override
    public float getAccelerationRate(PipeContext ctx) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            float accel = entry.module().getAcceleration(entry.scopedContext(ctx));
            if (accel > 0f) return accel;
        }
        for (Module module : getStaticModules()) {
            float accel = module.getAcceleration(ctx);
            if (accel > 0f) return accel;
        }
        return 0f;
    }

    @Override
    public float getDrag(PipeContext ctx) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            float drag = entry.module().getDrag(entry.scopedContext(ctx));
            if (drag > 0f) return drag;
        }
        for (Module module : getStaticModules()) {
            float drag = module.getDrag(ctx);
            if (drag > 0f) return drag;
        }
        return LogisticsConfigHost.get(Configs.PIPE_DRAG);
    }

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            float max = entry.module().getMaxSpeed(entry.scopedContext(ctx));
            if (max > 0f) return max;
        }
        for (Module module : getStaticModules()) {
            float max = module.getMaxSpeed(ctx);
            if (max > 0f) return max;
        }
        return LogisticsConfigHost.get(Configs.PIPE_MAX_SPEED);
    }

    @Override
    public InteractionResult onUseWithItem(PipeContext ctx, UseOnContext usage) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            InteractionResult result = entry.module().onUseWithItem(entry.scopedContext(ctx), usage);
            if (result != InteractionResult.PASS) return result;
        }
        for (Module module : getStaticModules()) {
            InteractionResult result = module.onUseWithItem(ctx, usage);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onUseWithoutItem(PipeContext ctx, UseOnContext usage) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            InteractionResult result = entry.module().onUseWithoutItem(entry.scopedContext(ctx), usage);
            if (result != InteractionResult.PASS) return result;
        }
        for (Module module : getStaticModules()) {
            InteractionResult result = module.onUseWithoutItem(ctx, usage);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getComparatorOutput(PipeContext ctx) {
        int output = 0;
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            output = Math.max(output, entry.module().comparatorOutput(entry.scopedContext(ctx)));
        }
        for (Module module : getStaticModules()) {
            output = Math.max(output, module.comparatorOutput(ctx));
        }
        return output;
    }

    @Override
    public void randomDisplayTick(PipeContext ctx, RandomSource random) {
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            entry.module().randomDisplayTick(entry.scopedContext(ctx), random);
        }
        for (Module module : getStaticModules()) {
            module.randomDisplayTick(ctx, random);
        }
    }

    @Override
    public PipeConnection.Type filterConnection(
            @Nullable PipeContext ctx, Direction direction, Block neighborBlock, PipeConnection.Type candidate) {
        if (candidate == PipeConnection.Type.NONE) return PipeConnection.Type.NONE;
        if (ctx != null) {
            for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
                if (!entry.module().allowsConnection(entry.scopedContext(ctx), direction, neighborBlock)) {
                    return PipeConnection.Type.NONE;
                }
            }
        }
        for (Module module : getStaticModules()) {
            if (!module.allowsConnection(ctx, direction, neighborBlock)) return PipeConnection.Type.NONE;
        }
        return candidate;
    }

    @Override
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        if (!hasEnergy()) return false;
        for (DynamicModule entry : getDynamicModuleEntries(ctx)) {
            if (entry.module().acceptsLowTierEnergyFrom(entry.scopedContext(ctx), from)) return true;
        }
        for (Module module : getStaticModules()) {
            if (module.acceptsLowTierEnergyFrom(ctx, from)) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // GUI — open chassis screen on wrench
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        var world = ctx.world();
        var pos = ctx.pos();
        int slots = maxSlots;
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ChassisScreenHandler(
                        syncId, inv, slots,
                        world.getBlockEntity(pos) instanceof PipeBlockEntity pe ? pe : null),
                world.getBlockState(pos).getBlock().getName()));
        return InteractionResult.SUCCESS;
    }
}
