package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.FluidDispatchableModule;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.pipe.network.NetDbg;
import com.logistics.pipe.network.NetworkRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A logistics pipe that mints fluid packets on demand from an adjacent tank.
 *
 * <p>Dispatched packets are capped at {@code fluid_packet_max_mb} but have no minimum — a dispatch
 * ships as many max-size packets as fit plus, if the amount doesn't divide evenly, one smaller tail
 * packet, instead of refunding the remainder. Every physical packet is its own {@link TravelingItem}
 * (fluid packets never stack — see {@code FluidPacketItem}) and costs one flat endpoint-RF charge
 * regardless of its mB payload. Dispatch is transactional and energy-atomic: unpayable fluid is
 * returned to the tank.
 *
 * <p>Network identity is the {@link Fluid} alone (see {@code FluidResourceKey}/{@code FluidOrderBook})
 * — never the physical packet, which always carries a real, positive {@code amountMb}.
 *
 * <p>The tank is discovered by scanning all six faces for an {@link IFluidStorage}.
 */
public class FluidProviderModule implements Module, TickingModule, FluidDispatchableModule {
    private static final String TICKS_SINCE_SCAN = "ticks_since_scan";

    private static final int SCAN_INTERVAL = 6;   // Refresh supply every 6 ticks (~3x/second)
    private static final int SUPPLY_PRIORITY = 1;  // Real stock; lower = preferred (mirrors ProviderModule)
    private static final int MAX_PACKETS_PER_DISPATCH = 64;

    // ==================== Module Interface ====================

    @Override
    public boolean connectsToFluidStorage(PipeContext ctx) {
        return true; // connect to the adjacent tank so a connection arm renders toward it
    }

    @Override
    public void onTick(PipeContext ctx) {
        int ticks = ctx.getInt(this, TICKS_SINCE_SCAN, 0) + 1;
        ctx.saveInt(this, TICKS_SINCE_SCAN, ticks);
        if (ticks >= SCAN_INTERVAL) {
            ctx.saveInt(this, TICKS_SINCE_SCAN, 0);
            scanAndUpdateSupply(ctx);
        }
    }

    private void scanAndUpdateSupply(PipeContext ctx) {
        ILogisticsNetwork network = NetworkRegistry.getOrCreateNetwork(ctx.world(), ctx.pos());
        if (network == null) return;

        Tank tank = findTank(ctx);
        if (tank == null) {
            network.removeFluidSupply(ctx.pos());
            return;
        }

        long tankMb = FluidUnits.toMillibuckets(tank.amountNative());
        if (tankMb <= 0) {
            network.removeFluidSupply(ctx.pos());
            return;
        }

        network.registerFluidSupply(ctx.pos(), tank.fluid().getFluid(), tankMb, SUPPLY_PRIORITY);
    }

    // ==================== Dispatch ====================

    @Override
    public long onFluidDispatch(PipeContext ctx, BlockPos requester, Fluid fluid, long amountMb, UUID deliveryId) {
        if (ctx.world().isClientSide()) return 0;
        if (amountMb <= 0) return 0;

        long maxMb = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB);
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
        if (maxMb <= 0) return 0;

        Tank tank = findTank(ctx);
        if (tank == null || tank.fluid().getFluid() != fluid) return 0;

        long tankMb = FluidUnits.toMillibuckets(tank.amountNative());
        long dispatchCap = Math.multiplyExact((long) MAX_PACKETS_PER_DISPATCH, maxMb);
        long deliverMb = Math.min(amountMb, Math.min(tankMb, dispatchCap));
        if (deliverMb <= 0) return 0;

        // ---- Extract fluid first (real), so energy is never charged without fluid on hand ----
        long gotNative = tank.storage().extract(tank.fluid(), FluidUnits.mb(deliverMb), false);
        long extractedMb = FluidUnits.toMillibuckets(gotNative);
        if (extractedMb <= 0) {
            if (gotNative > 0) tank.storage().insert(tank.fluid(), gotNative, false);
            return 0;
        }

        List<Long> chunks = FluidPacketChunker.chunk(extractedMb, maxMb);
        int totalPackets = chunks.size();

        // ---- Charge energy atomically, one flat RF unit per physical packet; consumeEnergy is
        // non-destructive on failure, so probe downward for the largest affordable prefix (dropping
        // the tail chunk first, then whole chunks from the end) and refund the rest. ----
        int payableCount = FluidPacketChunker.probeAffordablePrefixCount(
                totalPackets, count -> ctx.consumeEnergy((long) count * rf));

        long keptMb = 0;
        for (int i = 0; i < payableCount; i++) keptMb += chunks.get(i);

        long refundMb = extractedMb - keptMb;
        if (refundMb > 0) {
            tank.storage().insert(tank.fluid(), FluidUnits.mb(refundMb), false);
        }
        if (keptMb <= 0) return 0;

        // ---- Mint one TravelingItem per physical packet (packets never stack — see
        // FluidPacketItem), all sharing this dispatch's deliveryId so the network sums them
        // correctly toward the order. ----
        for (int i = 0; i < payableCount; i++) {
            ItemStack out = packetStack(fluid, chunks.get(i));
            TravelingItem traveling = new TravelingItem(
                    out,
                    tank.dir().getOpposite(),
                    LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED),
                    requester);
            traveling.setDeliveryId(deliveryId);
            ctx.pipeAccess().forceAddItem(traveling, tank.dir());
        }

        NetDbg.out("[FluidProvider @ {}] Dispatched {} packet(s) ({} mB total) of {} → {}",
                ctx.pos(), payableCount, keptMb, fluid, requester);
        return keptMb;
    }

    // ==================== Lifecycle ====================

    @Override
    public void onDetach(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            network.removeFluidSupply(ctx.pos());
        }
    }

    // ==================== Helpers ====================

    /** A fluid tank found adjacent to this pipe, with the face it was found on and its live contents. */
    private record Tank(IFluidStorage storage, Direction dir, IFluidKey fluid, long amountNative) {}

    /**
     * Scan all six faces for a fluid storage holding a single fluid. Returns the first non-empty tank.
     * A fluid-only tank is not an item {@code INVENTORY} connection, so we probe directly.
     */
    @Nullable
    private Tank findTank(PipeContext ctx) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = ctx.pos().relative(dir);
            IFluidStorage storage = FluidStorageLookup.find(ctx.world(), neighborPos, dir.getOpposite());
            if (storage == null) continue;
            for (IFluidView view : storage.contents()) {
                if (view.amount() > 0 && !view.resource().isBlank()) {
                    return new Tank(storage, dir, view.resource(), view.amount());
                }
            }
        }
        return null;
    }

    private static ItemStack packetStack(Fluid fluid, long amountMb) {
        ItemStack s = new ItemStack(LogisticsPipe.ITEM.FLUID_PACKET);
        s.set(LogisticsPipe.DATA.FLUID_PACKET, new FluidPacket(fluid, amountMb));
        return s;
    }
}
