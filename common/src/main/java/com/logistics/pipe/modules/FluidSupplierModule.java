package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.core.item.LogisticsBucketItem;
import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.PipeHud;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.RoutingModule;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.pipe.TransferHandlerModule;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.pipe.network.NetDbg;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.ui.FluidSupplierScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fluid supplier module — a logistics pipe placed on a machine with a fluid tank that keeps that tank
 * stocked to a target level by ordering {@link FluidPacket fluid packets} from the item logistics
 * network, draining them into an internal fluid buffer, and depositing from the buffer into the tank.
 *
 * <p>Consumption path: fluid packets can't be refused on arrival (the runtime always inserts into the
 * neighbor's storage), so the supplier claims them instead. {@link #route} reroutes any packet destined
 * for this pipe toward the target face (the adjacent block exposing an {@link IFluidStorage}), and
 * {@link #onTransferToStorage} intercepts the packet as it exits toward that face, adding its volume to
 * the buffer and consuming the item so it never enters the machine's item slots. Acceptance never fails
 * because ordering reserves buffer capacity up front.
 *
 * <p>Energy is a deferred paid/unpaid split. On accept a packet's fluid enters the buffer but is counted
 * as {@code unpaid}; each tick the module pays down unpaid packets one at a time as network energy
 * allows (charging {@code fluid_endpoint_rf_per_packet} RF each). Only <em>paid</em> fluid may deposit
 * into the tank; unpaid fluid waits loss-free. This makes acceptance energy-independent (never re-acked)
 * while still gating actual delivery on power.
 *
 * <p>The target tank is discovered by scanning all six faces for an {@link IFluidStorage}, mirroring how
 * {@link FluidProviderModule} locates its adjacent tank.
 */
public class FluidSupplierModule implements Module, TickingModule, RoutingModule, TransferHandlerModule {

    private static final String FILTER_FLUID = "filter_fluid";
    private static final String TARGET_MB = "target_mb";
    private static final String BUFFER_MB = "buffer_mb";
    private static final String UNPAID_PACKETS = "unpaid_packets";
    private static final String TICKS_SINCE_CHECK = "ticks_since_check";

    private static final int CHECK_INTERVAL = 20;
    /** Buffer holds one full order batch; ordering caps each cycle at this many packets. */
    public static final int BATCH_CAP = 4;
    /**
     * Upper bound on the configurable target (mB). Kept within signed-short range so the target and
     * buffer readouts sync cleanly through the menu's {@code ContainerData} (see the GUI screen).
     */
    public static final long MAX_TARGET_MB = 32_000;

    // ==================== Ticking ====================

    @Override
    public boolean connectsToFluidStorage(PipeContext ctx) {
        return true; // connect to the adjacent machine tank so a connection arm renders toward it
    }

    @Override
    public void onTick(PipeContext ctx) {
        payDownUnpaid(ctx);
        depositToTank(ctx);

        int ticks = ctx.getInt(this, TICKS_SINCE_CHECK, 0) + 1;
        if (ticks >= CHECK_INTERVAL) {
            ctx.saveInt(this, TICKS_SINCE_CHECK, 0);
            checkAndOrder(ctx);
        } else {
            ctx.saveInt(this, TICKS_SINCE_CHECK, ticks);
        }
    }

    /** Pay down unpaid packets one at a time as energy allows. Never charges on a failed consume. */
    private void payDownUnpaid(PipeContext ctx) {
        int unpaid = ctx.getInt(this, UNPAID_PACKETS, 0);
        if (unpaid <= 0) return;

        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
        int paid = 0;
        while (unpaid > 0 && ctx.consumeEnergy(rf)) {
            unpaid--;
            paid++;
        }
        if (paid > 0) {
            ctx.saveInt(this, UNPAID_PACKETS, unpaid);
            ctx.markDirty();
        }
    }

    /** Deposit paid buffer fluid into the target tank, target-aware (never overfills past the target). */
    private void depositToTank(PipeContext ctx) {
        long quantum = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        if (quantum <= 0) return;

        long bufferMb = ctx.getLong(this, BUFFER_MB, 0);
        int unpaid = ctx.getInt(this, UNPAID_PACKETS, 0);
        long paidMb = bufferMb - unpaid * quantum;
        if (paidMb <= 0) return;

        Fluid filter = getFilterFluid(ctx);
        if (filter == null) return;

        Direction targetDir = findTargetFace(ctx);
        if (targetDir == null) return;
        IFluidStorage storage = FluidStorageLookup.find(ctx.world(), ctx.pos().relative(targetDir), targetDir.getOpposite());
        if (storage == null) return;

        long targetMb = ctx.getLong(this, TARGET_MB, 0);
        long tankMb = measureTank(storage, filter);
        SimpleFluidKey key = SimpleFluidKey.of(filter);
        long roomMb = FluidUnits.toMillibuckets(storage.insert(key, FluidUnits.mb(paidMb), true));

        long depositMb = Math.min(paidMb, Math.min(targetMb - tankMb, roomMb));
        if (depositMb <= 0) return;

        long insertedNative = storage.insert(key, FluidUnits.mb(depositMb), false);
        long insertedMb = FluidUnits.toMillibuckets(insertedNative);
        if (insertedMb > 0) {
            ctx.saveLong(this, BUFFER_MB, bufferMb - insertedMb);
            ctx.markDirtyAndSync();
            NetDbg.out("[FluidSupplier @ {}] Deposited {} mB into tank ({} mB buffer left)",
                    ctx.pos(), insertedMb, bufferMb - insertedMb);
        }
    }

    /** Order fluid packets to top up the tank, reserving buffer capacity so acceptance never fails. */
    private void checkAndOrder(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        Fluid filter = getFilterFluid(ctx);
        if (filter == null) return;

        long targetMb = ctx.getLong(this, TARGET_MB, 0);
        if (targetMb <= 0) return;

        long quantum = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        if (quantum <= 0) return;

        Direction targetDir = findTargetFace(ctx);
        if (targetDir == null) return;
        IFluidStorage storage = FluidStorageLookup.find(ctx.world(), ctx.pos().relative(targetDir), targetDir.getOpposite());
        if (storage == null) return;

        long tankMb = measureTank(storage, filter);
        long bufferCapacityMb = (long) BATCH_CAP * quantum;

        ItemStack packet = packetStack(filter, quantum);
        long pendingMb = network.getOrderedAmountFor(ctx.pos(), packet) * quantum;
        long heldMb = ctx.getLong(this, BUFFER_MB, 0);

        long availableResMb = Math.max(0, bufferCapacityMb - heldMb - pendingMb);
        long neededMb = Math.max(0, targetMb - tankMb - heldMb - pendingMb);

        long neededPackets = ceilDiv(neededMb, quantum);
        long capacityPackets = availableResMb / quantum;
        long orderCount = Math.min(neededPackets, Math.min(capacityPackets, BATCH_CAP));

        if (orderCount > 0) {
            network.placeOrder(ItemStorageLookup.of(packet), orderCount, ctx.pos(), FulfillmentMode.PARTIAL);
            NetDbg.out("[FluidSupplier @ {}] Ordered {} packet(s) of {} (tank={} held={} pending={} target={})",
                    ctx.pos(), orderCount, filter, tankMb, heldMb, pendingMb, targetMb);
        }
    }

    // ==================== Routing / consumption ====================

    @Override
    public RoutePlan route(PipeContext ctx, TravelingItem item, List<Direction> options) {
        BlockPos destination = item.getDestination();
        if (destination != null && destination.equals(ctx.pos())) {
            Direction targetDir = findTargetFace(ctx);
            if (targetDir != null && options.contains(targetDir)) {
                return RoutePlan.reroute(targetDir);
            }
        }
        return RoutePlan.pass();
    }

    @Override
    public @Nullable TravelingItem onTransferToStorage(PipeContext ctx, TravelingItem item, Direction direction) {
        if (ctx.world().isClientSide()) return item;

        Direction targetDir = findTargetFace(ctx);
        if (targetDir == null || direction != targetDir) return item;

        ItemStack stack = item.getStack();
        if (stack.getItem() != LogisticsPipe.ITEM.FLUID_PACKET) return item;

        FluidPacket data = stack.get(LogisticsPipe.DATA.FLUID_PACKET);
        Fluid filter = getFilterFluid(ctx);
        long quantum = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        if (data == null || filter == null || data.fluid() != filter || data.quantumMb() != quantum) {
            return item; // not ours — let it fall through to the default insertion
        }

        BlockPos destination = item.getDestination();
        if (destination == null || !destination.equals(ctx.pos())) {
            return item; // addressed to another pipe — don't claim it or mis-notify its delivery
        }

        int count = stack.getCount();
        ctx.saveLong(this, BUFFER_MB, ctx.getLong(this, BUFFER_MB, 0) + (long) count * quantum);
        ctx.saveInt(this, UNPAID_PACKETS, ctx.getInt(this, UNPAID_PACKETS, 0) + count);
        ctx.markDirtyAndSync();

        IItemKey key = ItemStorageLookup.of(stack);
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            if (item.getDeliveryId() != null && item.getDestination() != null) {
                network.notifyDelivery(item.getDeliveryId(), item.getDestination(), key, count);
            } else {
                network.notifyDelivery(ctx.pos(), key, count);
            }
        }
        NetDbg.out("[FluidSupplier @ {}] Accepted {} packet(s) of {} into buffer", ctx.pos(), count, filter);
        return null; // consumed — never enters the machine's item slots
    }

    // ==================== Interaction ====================

    @Override
    public InteractionResult onUseWithItem(PipeContext ctx, UseOnContext usage) {
        Fluid fluid = fluidFromBucket(usage.getItemInHand());
        if (fluid == null) return InteractionResult.PASS;

        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;

        Player player = usage.getPlayer();
        if (isFilterLocked(ctx)) {
            message(player, Component.translatable("message.logistics.fluid_supplier.locked"));
            return InteractionResult.SUCCESS;
        }

        if (getFilterFluid(ctx) != fluid) {
            setFilterFluid(ctx, fluid);
            message(player, Component.translatable("message.logistics.fluid_supplier.filter_set", fluidName(fluid)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onUseWithoutItem(PipeContext ctx, UseOnContext usage) {
        Player player = usage.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (getFilterFluid(ctx) == null && ctx.getLong(this, TARGET_MB, 0) <= 0) return InteractionResult.PASS;

        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (isFilterLocked(ctx)) {
            message(player, Component.translatable("message.logistics.fluid_supplier.locked"));
            return InteractionResult.SUCCESS;
        }
        ctx.remove(this, FILTER_FLUID);
        ctx.saveLong(this, TARGET_MB, 0);
        ctx.markDirtyAndSync();
        message(player, Component.translatable("message.logistics.fluid_supplier.cleared"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onWrench(PipeContext ctx, Player player) {
        if (ctx.world().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        Level world = ctx.world();
        BlockPos pos = ctx.pos();
        String moduleStateKey = ctx.moduleStateKey(this);
        serverPlayer.openMenu(new SimpleMenuProvider(
                (syncId, inventory, playerEntity) -> {
                    PipeBlockEntity pipeEntity =
                            world.getBlockEntity(pos) instanceof PipeBlockEntity entity ? entity : null;
                    return new FluidSupplierScreenHandler(syncId, inventory, pipeEntity, moduleStateKey);
                },
                Component.translatable("screen.logistics.fluid_supplier")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHud(PipeContext ctx, PipeHud hud) {
        Fluid filter = getFilterFluid(ctx);
        Component fluidLabel = filter == null
                ? Component.translatable("message.logistics.fluid_supplier.no_fluid")
                : fluidName(filter);
        hud.line(ModuleHud.summary("gui.logistics.fluid_supplier.fluid", fluidLabel));
        if (hud.showDetails()) {
            hud.line(ModuleHud.detail(Component.translatable("message.logistics.fluid_supplier.status",
                    fluidLabel, ctx.getLong(this, TARGET_MB, 0), ctx.getLong(this, BUFFER_MB, 0))));
        }
    }

    // ==================== State accessors (also used by tests) ====================

    /** Set the target fluid to keep stocked. */
    public void setFilterFluid(PipeContext ctx, @Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            ctx.remove(this, FILTER_FLUID);
        } else {
            ctx.saveString(this, FILTER_FLUID, BuiltInRegistries.FLUID.getKey(fluid).toString());
        }
        ctx.markDirtyAndSync();
    }

    /** The configured target fluid, or {@code null} if none is set. */
    @Nullable
    public Fluid getFilterFluid(PipeContext ctx) {
        String id = ctx.getString(this, FILTER_FLUID, "");
        if (id.isEmpty()) return null;
        ResourceId location = ResourceId.tryParse(id);
        if (location == null) return null;
        return BuiltInRegistries.FLUID.get(location.toIdentifier()).map(ref -> ref.value()).orElse(null);
    }

    /** Set the target amount (mB) to keep in the machine tank, clamped to {@code [0, MAX_TARGET_MB]}. */
    public void setTargetMb(PipeContext ctx, long targetMb) {
        ctx.saveLong(this, TARGET_MB, Math.max(0, Math.min(MAX_TARGET_MB, targetMb)));
        ctx.markDirtyAndSync();
    }

    public long getTargetMb(PipeContext ctx) {
        return ctx.getLong(this, TARGET_MB, 0);
    }

    public long getBufferMb(PipeContext ctx) {
        return ctx.getLong(this, BUFFER_MB, 0);
    }

    public int getUnpaidPackets(PipeContext ctx) {
        return ctx.getInt(this, UNPAID_PACKETS, 0);
    }

    // ==================== Helpers ====================

    /** Locked while any fluid is held or on order, so the filter can't change mid-flight. */
    public boolean isFilterLocked(PipeContext ctx) {
        if (ctx.getLong(this, BUFFER_MB, 0) > 0 || ctx.getInt(this, UNPAID_PACKETS, 0) > 0) {
            return true;
        }
        ILogisticsNetwork network = ctx.network();
        Fluid filter = getFilterFluid(ctx);
        if (network == null || filter == null) return false;
        long quantum = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        return network.getOrderedAmountFor(ctx.pos(), packetStack(filter, quantum)) > 0;
    }

    /** First adjacent face exposing a fluid storage — the machine tank we keep stocked. */
    @Nullable
    private Direction findTargetFace(PipeContext ctx) {
        for (Direction dir : Direction.values()) {
            IFluidStorage storage = FluidStorageLookup.find(ctx.world(), ctx.pos().relative(dir), dir.getOpposite());
            if (storage != null) {
                return dir;
            }
        }
        return null;
    }

    /** Total mB of the filter fluid currently in the target storage. */
    private long measureTank(IFluidStorage storage, Fluid filter) {
        long total = 0;
        for (IFluidView view : storage.contents()) {
            if (view.resource().getFluid() == filter) {
                total += view.amount();
            }
        }
        return FluidUnits.toMillibuckets(total);
    }

    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    private static ItemStack packetStack(Fluid fluid, long quantumMb) {
        ItemStack s = new ItemStack(LogisticsPipe.ITEM.FLUID_PACKET);
        s.set(LogisticsPipe.DATA.FLUID_PACKET, new FluidPacket(fluid, quantumMb));
        return s;
    }

    private static Component fluidName(Fluid fluid) {
        return Component.literal(BuiltInRegistries.FLUID.getKey(fluid).getPath());
    }

    /**
     * Read the fluid held by a filled bucket. Handles vanilla water/lava buckets and the mod's
     * {@link LogisticsBucketItem}; returns {@code null} for anything else.
     */
    @Nullable
    private static Fluid fluidFromBucket(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        if (stack.getItem() instanceof LogisticsBucketItem bucket) {
            ResourceId id = ResourceId.in(LogisticsConfigHost.MOD_ID, "core/" + bucket.fluidName());
            return BuiltInRegistries.FLUID.get(id.toIdentifier()).map(ref -> ref.value()).orElse(null);
        }
        return null;
    }

    private static void message(@Nullable Player player, Component text) {
        if (player != null) {
            player.displayClientMessage(text, true);
        }
    }
}
