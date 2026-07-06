package com.logistics.pipe;
import com.logistics.LogisticsPipe;

import com.logistics.core.lib.pipe.DispatchableModule;
import com.logistics.core.lib.pipe.IModuleHost;
import com.logistics.core.lib.pipe.ItemAcceptingModule;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.RoutingModule;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.pipe.TransferHandlerModule;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An item transport pipe: a {@link Pipe} that routes {@link TravelingItem}s, dispatches network orders,
 * and applies item physics (acceleration/drag/max-speed). Item-transport behavior is composed from
 * {@link Module}s (routing, dispatch, ticking, transfer handling); {@link ChassisPipe} extends this to
 * support runtime-installed modules.
 */
public class ItemPipe extends Pipe {

    public ItemPipe(Module... modules) {
        super(modules);
    }

    @Override
    public ItemPipe withEnergy() {
        super.withEnergy();
        return this;
    }

    /**
     * Returns a module of the given type, considering dynamic modules when a block entity is available.
     * Subclasses (e.g. {@link ChassisPipe}) override this to support runtime-installed modules.
     */
    public <T extends Module> T getModule(Class<T> moduleClass, PipeBlockEntity entity) {
        return getModule(moduleClass);
    }

    @Override
    public <T extends Module> T getModule(Class<T> moduleClass, IModuleHost host) {
        if (host instanceof PipeBlockEntity pbe) {
            return getModule(moduleClass, pbe);
        }
        return getModule(moduleClass);
    }

    public <T extends Module> T getModule(Class<T> moduleClass, PipeBlockEntity entity, @Nullable String stateKey) {
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

    /**
     * Returns all modules for this pipe. For chassis pipes, includes dynamically-installed modules.
     */
    public List<Module> getModules(PipeBlockEntity entity) {
        return getStaticModules();
    }

    /**
     * Dispatches polymorphically so chassis-pipe overrides are respected in all base-class methods.
     */
    @Override
    protected List<Module> getModules(PipeContext ctx) {
        if (ctx.blockEntity() instanceof PipeBlockEntity pbe) {
            return getModules(pbe);
        }
        return getStaticModules();
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
        return LogisticsPipe.PIPE_DRAG.get();
    }

    public float getMaxSpeed(PipeContext ctx) {
        for (Module module : getModules(ctx)) {
            float max = module.getMaxSpeed(ctx);
            if (max > 0f) {
                return max;
            }
        }
        return LogisticsPipe.PIPE_MAX_SPEED.get();
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
     * Give modules a chance to handle transfer of a TravelingItem into an adjacent non-pipe storage.
     * Returns null if a module fully handled the transfer, or the item (possibly modified) otherwise.
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
     * Called when an item is inserted from an external (non-pipe) source before the default single
     * TravelingItem is created. Stops at the first module that returns true (handled).
     */
    public boolean onExternalInsert(PipeContext ctx, ItemStack stack, Direction from) {
        for (Module module : getModules(ctx)) {
            if (module.onExternalInsert(ctx, stack, from)) return true;
        }
        return false;
    }

    public boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        // Default behavior: pipes only accept items from other pipes (not from inventories/hoppers),
        // preserving the extraction energy cost. Modules may explicitly permit non-pipe insertion.
        if (!ctx.isNeighborPipe(from)) {
            boolean allowed = false;
            for (Module module : getModules(ctx)) {
                if (module.canAcceptFromNonPipe(ctx, from)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) return false;
        }

        for (Module module : getModules(ctx)) {
            if (!module.canAcceptFrom(ctx, from, stack)) {
                return false;
            }
        }
        return true;
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

    public boolean matchesSinkFilter(PipeContext ctx, ItemStack stack) {
        for (Module module : getModules(ctx)) {
            if (module instanceof ItemAcceptingModule sink && sink.acceptsItem(ctx, stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatch items to a requester by asking the first module that can fulfill to extract.
     *
     * @return actual amount dispatched (&gt;0), -1 if the provider is temporarily busy (deferred),
     *         or 0 if no module could fulfill
     */
    public long dispatch(PipeContext ctx, BlockPos requester, IItemKey item, long amount, UUID deliveryId) {
        if (ctx.world().isClientSide()) return 0;
        for (Module module : getModules(ctx)) {
            if (module instanceof DispatchableModule dispatchable) {
                long dispatched = dispatchable.onDispatch(ctx, requester, item, amount, deliveryId);
                if (dispatched != 0) return dispatched; // propagate both success (>0) and deferred (<0)
            }
        }
        return 0;
    }

    // Energy capability check for low-tier energy sources.
    public boolean acceptsLowTierEnergyFrom(PipeContext ctx, Direction from) {
        if (!hasEnergy()) {
            return false;
        }
        for (Module module : getModules(ctx)) {
            if (module.acceptsLowTierEnergyFrom(ctx, from)) {
                return true;
            }
        }
        return false;
    }
}
