package com.logistics.pipe;

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
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
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

import java.util.ArrayList;
import java.util.List;
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
public class ChassisPipe extends Pipe {
    /** Maximum number of module slots across all chassis marks. */
    public static final int MAX_SLOTS = 8;

    /** NBT key used to store chassis slot configuration in the pipe's module state. */
    public static final String STATE_KEY = "chassispipe";

    private final int maxSlots;

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
        for (Module m : getDynamicModules(ctx)) {
            if (moduleClass.isInstance(m)) return moduleClass.cast(m);
        }
        return super.getModule(moduleClass);
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
        var state = ctx.moduleState(STATE_KEY);
        RegistryOps<net.minecraft.nbt.Tag> ops = ctx.world().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (int slot = 0; slot < maxSlots; slot++) {
            Tag tag = state.get(String.valueOf(slot));
            if (tag == null) continue;
            ItemStack.CODEC.parse(ops, tag).result()
                    .map(ItemStack::getItem)
                    .filter(item -> item instanceof ModuleItem)
                    .map(item -> ((ModuleItem) item).createModule())
                    .ifPresent(modules::add);
        }
        return modules;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    // Routing / ticking — dynamic modules run BEFORE fixed modules
    // -------------------------------------------------------------------------

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        for (Module m : getDynamicModules(ctx)) {
            if (m instanceof RoutingModule router) {
                RoutePlan plan = router.route(ctx, item, options);
                if (plan.getType() != RoutePlan.Type.PASS) return plan;
            }
        }
        return super.route(ctx, item, options);
    }

    @Override
    public void onTick(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;
        for (Module m : getDynamicModules(ctx)) {
            if (m instanceof TickingModule ticking) {
                ticking.onTick(ctx);
            }
        }
        super.onTick(ctx);
    }

    @Override
    public long dispatch(PipeContext ctx, BlockPos requester, ItemVariant item, long amount, UUID deliveryId) {
        if (ctx.world().isClientSide()) return 0;
        for (Module m : getDynamicModules(ctx)) {
            if (m instanceof DispatchableModule dispatchable) {
                long d = dispatchable.onDispatch(ctx, requester, item, amount, deliveryId);
                if (d != 0) return d;
            }
        }
        return super.dispatch(ctx, requester, item, amount, deliveryId);
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
        for (Module m : getDynamicModules(ctx)) {
            if (m instanceof RandomTickModule rt) {
                rt.randomTick(ctx, random);
            }
        }
        super.randomTick(ctx, random);
    }

    @Override
    public TravelingItem handleTransfer(PipeContext ctx, TravelingItem item, Direction direction) {
        TravelingItem current = item;
        for (Module m : getDynamicModules(ctx)) {
            if (m instanceof TransferHandlerModule handler) {
                current = handler.onTransferToStorage(ctx, current, direction);
                if (current == null) return null;
            }
        }
        return super.handleTransfer(ctx, current, direction);
    }

    @Override
    public boolean onExternalInsert(PipeContext ctx, ItemStack stack, Direction from) {
        for (Module m : getDynamicModules(ctx)) {
            if (m.onExternalInsert(ctx, stack, from)) return true;
        }
        return super.onExternalInsert(ctx, stack, from);
    }

    @Override
    public boolean canAcceptFrom(PipeContext ctx, Direction from, ItemStack stack) {
        for (Module m : getDynamicModules(ctx)) {
            if (!m.canAcceptFrom(ctx, from, stack)) return false;
        }
        return super.canAcceptFrom(ctx, from, stack);
    }

    @Override
    public void onConnectionsChanged(PipeContext ctx, List<Direction> connected) {
        for (Module m : getDynamicModules(ctx)) {
            m.onConnectionsChanged(ctx, connected);
        }
        super.onConnectionsChanged(ctx, connected);
    }

    // -------------------------------------------------------------------------
    // Rendering — merge dynamic module decorations with fixed modules
    // -------------------------------------------------------------------------

    @Override
    public List<CoreDecoration> getCoreDecorations(PipeContext ctx) {
        List<CoreDecoration> result = new ArrayList<>(super.getCoreDecorations(ctx));
        for (Module m : getDynamicModules(ctx)) {
            result.addAll(m.getCoreDecorations(ctx));
        }
        return result;
    }

    @Override
    public List<ResourceId> getPipeDecorations(PipeContext ctx, Direction direction) {
        List<ResourceId> result = new ArrayList<>(super.getPipeDecorations(ctx, direction));
        for (Module m : getDynamicModules(ctx)) {
            result.addAll(m.getPipeDecorations(ctx, direction));
        }
        return result;
    }

    @Override
    public Integer getArmTint(PipeContext ctx, Direction direction) {
        for (Module m : getDynamicModules(ctx)) {
            Integer tint = m.getArmTint(ctx, direction);
            if (tint != null) return tint;
        }
        return super.getArmTint(ctx, direction);
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
