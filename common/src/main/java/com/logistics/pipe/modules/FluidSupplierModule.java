package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.core.item.LogisticsBucketItem;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.fluids.FluidDisplay;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fluid supplier module — a logistics pipe placed on a machine with a fluid tank that keeps that tank
 * stocked to a target level by ordering fluid packets from the fluid logistics network (see
 * {@code FluidOrderBook}), draining them into an internal fluid buffer, and depositing from the buffer
 * into the tank.
 *
 * <p>Consumption path: fluid packets can't be refused on arrival (the runtime always inserts into the
 * neighbor's storage), so the supplier claims them instead. {@link #route} reroutes any packet destined
 * for this pipe toward the target face (the adjacent block exposing an {@link IFluidStorage}), and
 * {@link #onTransferToStorage} intercepts the packet as it exits toward that face, adding its volume to
 * the buffer and consuming the item so it never enters the machine's item slots. Acceptance never fails
 * because ordering reserves buffer capacity up front, and accepts any positive-mB packet for the filter
 * fluid regardless of size — packets are no longer quantized, so there's nothing to match exactly.
 *
 * <p>Energy is a deferred paid/unpaid split, tracked per **physical packet** in a FIFO queue (not a
 * uniform-size count): on accept, a packet's fluid enters the buffer and its exact mB is pushed onto the
 * unpaid queue; each tick the module pays down the queue head, one packet at a time, as network energy
 * allows (charging {@code fluid_endpoint_rf_per_packet} RF per packet regardless of its mB). Only fluid
 * whose packets have been paid off may deposit into the tank ({@code buffer - sum(unpaid queue)}); the
 * unpaid remainder waits loss-free. This makes acceptance energy-independent (never re-acked) while
 * still gating actual delivery on power, and — because the queue tracks each packet's real mB rather
 * than deriving it from the current max-packet-size config — stays correct even if that config changes
 * mid-flight.
 *
 * <p>The target tank is discovered by scanning all six faces for an {@link IFluidStorage}, mirroring how
 * {@link FluidProviderModule} locates its adjacent tank.
 */
public class FluidSupplierModule implements Module, TickingModule, RoutingModule, TransferHandlerModule {

    private static final String FILTER_FLUID = "filter_fluid";
    private static final String TARGET_MB = "target_mb";
    private static final String BUFFER_MB = "buffer_mb";
    private static final String UNPAID_PACKETS_MB = "unpaid_packets_mb";
    private static final String TICKS_SINCE_CHECK = "ticks_since_check";
    private static final String FULFILLMENT_MODE = "fulfillment_mode";
    private static final String MIN_THRESHOLD = "min_threshold";

    private static final int CHECK_INTERVAL = 20;
    /** Buffer holds one full order batch; ordering caps each cycle at this many max-size packets. */
    public static final int BATCH_CAP = 4;
    /**
     * Upper bound on the configurable target (mB). Kept within signed-short range so the target and
     * buffer readouts sync cleanly through the menu's {@code ContainerData} (see the GUI screen).
     */
    public static final long MAX_TARGET_MB = 32_000;

    /**
     * How large a deficit must be before the supplier bothers placing an order — throttles request
     * granularity independent of {@link FulfillmentMode}, mirroring LogisticsPipes' fluid supplier.
     */
    public enum MinThreshold {
        NONE(0), ONE_BUCKET(1000), TWO_BUCKET(2000), FIVE_BUCKET(5000);

        private final long mb;

        MinThreshold(long mb) {
            this.mb = mb;
        }

        public long mb() {
            return mb;
        }
    }

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

    /** Pay down unpaid packets one at a time (oldest first) as energy allows. Never charges on failure. */
    private void payDownUnpaid(PipeContext ctx) {
        List<Long> unpaid = loadUnpaidQueue(ctx);
        if (unpaid.isEmpty()) return;

        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
        boolean changed = false;
        while (!unpaid.isEmpty() && ctx.consumeEnergy(rf)) {
            unpaid.remove(0);
            changed = true;
        }
        if (changed) {
            saveUnpaidQueue(ctx, unpaid);
            ctx.markDirty();
        }
    }

    /** Deposit paid buffer fluid into the target tank, target-aware (never overfills past the target). */
    private void depositToTank(PipeContext ctx) {
        long bufferMb = ctx.getLong(this, BUFFER_MB, 0);
        long paidMb = bufferMb - unpaidMb(ctx);
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

    /** Order fluid to top up the tank, reserving buffer capacity so acceptance never fails. */
    private void checkAndOrder(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        Fluid filter = getFilterFluid(ctx);
        if (filter == null) return;

        long targetMb = ctx.getLong(this, TARGET_MB, 0);
        if (targetMb <= 0) return;

        long maxMb = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB);
        if (maxMb <= 0) return;

        Direction targetDir = findTargetFace(ctx);
        if (targetDir == null) return;
        IFluidStorage storage = FluidStorageLookup.find(ctx.world(), ctx.pos().relative(targetDir), targetDir.getOpposite());
        if (storage == null) return;

        long tankMb = measureTank(storage, filter);
        long bufferCapacityMb = (long) BATCH_CAP * maxMb;

        long pendingMb = network.getFluidOrderedMbFor(ctx.pos(), filter);
        long heldMb = ctx.getLong(this, BUFFER_MB, 0);

        long availableResMb = Math.max(0, bufferCapacityMb - heldMb - pendingMb);
        long neededMb = Math.max(0, targetMb - tankMb - heldMb - pendingMb);
        if (neededMb <= 0) return;
        if (neededMb < getMinThreshold(ctx).mb()) return;

        // Simulated insertion caps orders at the tank's real physical room.
        SimpleFluidKey key = SimpleFluidKey.of(filter);
        long roomMb = FluidUnits.toMillibuckets(storage.insert(key, FluidUnits.mb(neededMb), true));

        long orderMb = Math.min(neededMb, Math.min(availableResMb, roomMb));

        if (orderMb > 0) {
            network.placeFluidOrder(filter, orderMb, ctx.pos(), getFulfillmentMode(ctx));
            NetDbg.out("[FluidSupplier @ {}] Ordered {} mB of {} (tank={} held={} pending={} target={})",
                    ctx.pos(), orderMb, filter, tankMb, heldMb, pendingMb, targetMb);
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
        if (data == null || filter == null || data.fluid() != filter) {
            return item; // not ours — let it fall through to the default insertion
        }

        BlockPos destination = item.getDestination();
        if (destination == null || !destination.equals(ctx.pos())) {
            return item; // addressed to another pipe — don't claim it or mis-notify its delivery
        }

        // Packets never stack (count is always 1), but multiply defensively rather than assume it.
        long amountMb = (long) stack.getCount() * data.amountMb();
        ctx.saveLong(this, BUFFER_MB, ctx.getLong(this, BUFFER_MB, 0) + amountMb);
        appendUnpaid(ctx, amountMb);
        ctx.markDirtyAndSync();

        ILogisticsNetwork network = ctx.network();
        if (network != null && item.getDeliveryId() != null) {
            network.notifyFluidDelivery(item.getDeliveryId(), destination, filter, amountMb);
        }
        NetDbg.out("[FluidSupplier @ {}] Accepted packet ({} mB) of {} into buffer", ctx.pos(), amountMb, filter);
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
            hud.line(ModuleHud.detail(Component.translatable(
                    "gui.logistics.mode." + getFulfillmentMode(ctx).name().toLowerCase(Locale.ROOT))));
            hud.line(ModuleHud.detail(Component.translatable("gui.logistics.fluid_supplier.min_threshold."
                    + getMinThreshold(ctx).name().toLowerCase(Locale.ROOT))));
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

    /** Total mB across all physical packets received but not yet paid off. */
    public long getUnpaidMb(PipeContext ctx) {
        return unpaidMb(ctx);
    }

    /** Number of distinct physical packets received but not yet paid off. */
    public int getUnpaidPacketCount(PipeContext ctx) {
        return loadUnpaidQueue(ctx).size();
    }

    /** The current fulfillment mode (Partial accepts whatever's currently available; Full waits). */
    public FulfillmentMode getFulfillmentMode(PipeContext ctx) {
        return FulfillmentMode.values()[getFulfillmentModeOrdinal(ctx)];
    }

    /**
     * The current fulfillment mode as an integer (for GUI sync). Clamped into range in case stored NBT
     * is stale or corrupted — {@link #getFulfillmentMode} indexes directly with this during pipe tick,
     * so an out-of-range value here would otherwise throw.
     */
    public int getFulfillmentModeOrdinal(PipeContext ctx) {
        int raw = ctx.getInt(this, FULFILLMENT_MODE, FulfillmentMode.PARTIAL.ordinal());
        return Math.max(0, Math.min(FulfillmentMode.values().length - 1, raw));
    }

    public void setFulfillmentMode(PipeContext ctx, FulfillmentMode mode) {
        ctx.saveInt(this, FULFILLMENT_MODE, mode.ordinal());
        ctx.markDirtyAndSync();
    }

    /** Set the fulfillment mode from ordinal (for GUI). */
    public void setFulfillmentModeFromOrdinal(PipeContext ctx, int ordinal) {
        if (ordinal < 0 || ordinal >= FulfillmentMode.values().length) {
            throw new IllegalArgumentException("Invalid fulfillment mode ordinal: " + ordinal);
        }
        setFulfillmentMode(ctx, FulfillmentMode.values()[ordinal]);
    }

    /** The current minimum-deficit threshold before an order is placed at all. */
    public MinThreshold getMinThreshold(PipeContext ctx) {
        return MinThreshold.values()[getMinThresholdOrdinal(ctx)];
    }

    /**
     * The current minimum threshold as an integer (for GUI sync). Clamped into range in case stored
     * NBT is stale or corrupted — {@link #getMinThreshold} indexes directly with this during pipe tick,
     * so an out-of-range value here would otherwise throw.
     */
    public int getMinThresholdOrdinal(PipeContext ctx) {
        int raw = ctx.getInt(this, MIN_THRESHOLD, MinThreshold.NONE.ordinal());
        return Math.max(0, Math.min(MinThreshold.values().length - 1, raw));
    }

    public void setMinThreshold(PipeContext ctx, MinThreshold threshold) {
        ctx.saveInt(this, MIN_THRESHOLD, threshold.ordinal());
        ctx.markDirtyAndSync();
    }

    /** Set the minimum threshold from ordinal (for GUI). */
    public void setMinThresholdFromOrdinal(PipeContext ctx, int ordinal) {
        if (ordinal < 0 || ordinal >= MinThreshold.values().length) {
            throw new IllegalArgumentException("Invalid min threshold ordinal: " + ordinal);
        }
        setMinThreshold(ctx, MinThreshold.values()[ordinal]);
    }

    // ==================== Helpers ====================

    /** Locked while any fluid is held or on order, so the filter can't change mid-flight. */
    public boolean isFilterLocked(PipeContext ctx) {
        if (ctx.getLong(this, BUFFER_MB, 0) > 0 || !loadUnpaidQueue(ctx).isEmpty()) {
            return true;
        }
        ILogisticsNetwork network = ctx.network();
        Fluid filter = getFilterFluid(ctx);
        if (network == null || filter == null) return false;
        return network.getFluidOrderedMbFor(ctx.pos(), filter) > 0;
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

    /** FIFO of individual physical packet amounts (mB) received but not yet paid off, oldest first. */
    private List<Long> loadUnpaidQueue(PipeContext ctx) {
        long[] raw = NbtCompat.getLongArray(ctx.moduleState(this), UNPAID_PACKETS_MB, new long[0]);
        List<Long> queue = new ArrayList<>(raw.length);
        for (long mb : raw) queue.add(mb);
        return queue;
    }

    private void saveUnpaidQueue(PipeContext ctx, List<Long> queue) {
        long[] raw = new long[queue.size()];
        for (int i = 0; i < queue.size(); i++) raw[i] = queue.get(i);
        ctx.moduleState(this).putLongArray(UNPAID_PACKETS_MB, raw);
    }

    private void appendUnpaid(PipeContext ctx, long amountMb) {
        List<Long> queue = loadUnpaidQueue(ctx);
        queue.add(amountMb);
        saveUnpaidQueue(ctx, queue);
    }

    /** Total unpaid mB across the queue, derived fresh each call — never cached, so it can't drift. */
    private long unpaidMb(PipeContext ctx) {
        long total = 0;
        for (long mb : loadUnpaidQueue(ctx)) total += mb;
        return total;
    }

    /** Public so {@code FluidSupplierScreenHandler} can format the same "filter set" message. */
    public static Component fluidName(Fluid fluid) {
        return FluidDisplay.name(fluid);
    }

    /**
     * Read the fluid held by a filled bucket. Handles vanilla water/lava buckets and the mod's
     * {@link LogisticsBucketItem}; returns {@code null} for anything else. Public so
     * {@code FluidSupplierScreenHandler} can reuse this to resolve a GUI-carried item.
     */
    @Nullable
    public static Fluid fluidFromBucket(ItemStack stack) {
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
