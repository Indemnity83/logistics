package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.core.lib.fluids.FluidStorageLookup;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.network.ILogisticsNetwork;
import com.logistics.core.lib.pipe.DispatchableModule;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.TickingModule;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.pipe.network.NetDbg;
import com.logistics.pipe.network.NetworkRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A logistics pipe that mints fluid packets on demand from an adjacent tank.
 *
 * <p>Each dispatched packet carries a fixed quantum of fluid (see the {@code fluid_packet_quantum_mb}
 * config) baked into its data component. Dispatch is transactional and energy-atomic: unpayable fluid is
 * returned to the tank.
 *
 * <p>The tank is discovered by scanning all six faces for an {@link IFluidStorage}.
 */
public class FluidProviderModule implements Module, TickingModule, DispatchableModule {
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

        long quantumMb = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);

        Tank tank = findTank(ctx);
        if (tank == null) {
            clearSupply(ctx, network);
            return;
        }

        long tankMb = FluidUnits.toMillibuckets(tank.amountNative());
        if (tankMb < quantumMb) {
            clearSupply(ctx, network);
            return;
        }

        Fluid fluid = tank.fluid().getFluid();
        IItemKey packetKey = ItemStorageLookup.of(packetStack(fluid, quantumMb));

        Map<IItemKey, Long> supply = new HashMap<>();
        supply.put(packetKey, tankMb / quantumMb);
        network.registerSupply(ctx.pos(), supply, SUPPLY_PRIORITY);

        network.registerProviderCheck(ctx.pos(), (amount, checker) -> {
            long q = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
            long r = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
            Tank now = findTank(ctx);
            if (now == null) return List.of(packetKey);
            if (!packetKey.matches(packetStack(now.fluid().getFluid(), q))) return List.of(packetKey);
            long availableMb = FluidUnits.toMillibuckets(now.amountNative());
            boolean fluidOk = availableMb >= amount * q;
            // No network energy-quantity API exists; isPowered() is a best-effort gate. The real
            // per-packet energy cap is enforced atomically in onDispatch.
            boolean energyOk = r <= 0 || network.isPowered();
            return (fluidOk && energyOk) ? List.of() : List.of(packetKey);
        });
    }

    private void clearSupply(PipeContext ctx, ILogisticsNetwork network) {
        network.registerSupply(ctx.pos(), new HashMap<>(), SUPPLY_PRIORITY);
        network.unregisterProviderCheck(ctx.pos());
    }

    // ==================== Dispatch ====================

    @Override
    public long onDispatch(PipeContext ctx, BlockPos requester, IItemKey item, long amount, UUID deliveryId) {
        if (ctx.world().isClientSide()) return 0;
        if (amount <= 0) return 0;

        long quantumMb = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
        if (quantumMb <= 0) return 0;

        Tank tank = findTank(ctx);
        if (tank == null) return 0;

        Fluid fluid = tank.fluid().getFluid();
        ItemStack packetStack = packetStack(fluid, quantumMb);
        // Only fulfill requests for our exact packet (same fluid + quantum).
        if (!item.matches(packetStack)) return 0;

        long tankMb = FluidUnits.toMillibuckets(tank.amountNative());
        long count = Math.min(amount, Math.min(tankMb / quantumMb, MAX_PACKETS_PER_DISPATCH));
        if (count <= 0) return 0;

        // ---- Extract fluid first (real), so energy is never charged without fluid on hand ----
        long gotNative = tank.storage().extract(tank.fluid(), FluidUnits.mb(count * quantumMb), false);
        long extractedCount = FluidUnits.toMillibuckets(gotNative) / quantumMb;
        // Return any non-whole-packet remainder to the tank immediately.
        long keepNative = FluidUnits.mb(extractedCount * quantumMb);
        if (gotNative > keepNative) {
            tank.storage().insert(tank.fluid(), gotNative - keepNative, false);
        }
        count = extractedCount;
        if (count <= 0) {
            if (gotNative > 0) tank.storage().insert(tank.fluid(), gotNative, false);
            return 0;
        }

        // ---- Charge energy atomically; consumeEnergy is all-or-nothing and non-destructive on
        // failure, so probe downward for the largest affordable count and refund unpayable fluid. ----
        long payable = count;
        while (payable > 0 && !ctx.consumeEnergy(payable * rf)) {
            payable--;
        }
        if (payable < count) {
            long refundNative = FluidUnits.mb((count - payable) * quantumMb);
            if (refundNative > 0) tank.storage().insert(tank.fluid(), refundNative, false);
            count = payable;
        }
        if (count <= 0) return 0;

        // ---- Inject the minted packet toward the requester (packet enters from the tank face). ----
        ItemStack out = packetStack.copyWithCount((int) count);
        TravelingItem traveling = new TravelingItem(
                out,
                tank.dir().getOpposite(),
                LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED),
                requester);
        traveling.setDeliveryId(deliveryId);
        ctx.pipeAccess().forceAddItem(traveling, tank.dir());

        NetDbg.out("[FluidProvider @ {}] Dispatched {} packet(s) of {} ({} mB) → {}",
                ctx.pos(), count, fluid, count * quantumMb, requester);
        return count;
    }

    // ==================== Lifecycle ====================

    @Override
    public void onDetach(PipeContext ctx) {
        if (ctx.world().isClientSide()) return;
        ILogisticsNetwork network = ctx.network();
        if (network != null) {
            network.registerSupply(ctx.pos(), new HashMap<>(), SUPPLY_PRIORITY);
            network.unregisterProviderCheck(ctx.pos());
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

    private static ItemStack packetStack(Fluid fluid, long quantumMb) {
        ItemStack s = new ItemStack(LogisticsPipe.ITEM.FLUID_PACKET);
        s.set(LogisticsPipe.DATA.FLUID_PACKET, new FluidPacket(fluid, quantumMb));
        return s;
    }
}
