package com.logistics.gametest.pipe;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.automation.refinery.RefineryBlockEntity;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.modules.FluidSupplierModule;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared fluid supplier GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's
 * own registration mechanism: Fabric's {@code @GameTest}-annotated {@code FluidSupplierGameTest}
 * delegates to these methods, and NeoForge's {@code FluidSupplierGameTestRegistration} references
 * them directly as {@code Consumer<GameTestHelper>} method references.
 *
 * <p>Tick-based tests for the Fluid Supplier Pipe: a logistics pipe placed on a machine tank that
 * keeps that tank stocked by ordering fluid from the fluid logistics network ({@code FluidOrderBook}),
 * draining delivered packets into an internal buffer, and depositing paid fluid into the machine tank.
 *
 * <p>Layout (y=1): [refinery] ← [fluid_supplier_pipe] ← [transport_pipe] ← [fluid_provider_pipe]
 * with a glass tank (water source) above the provider and a power junction above the transport pipe.
 */
public class FluidSupplierGameTestBody {

    private static final BlockPos PROVIDER = new BlockPos(0, 1, 0);
    private static final BlockPos SOURCE_TANK = new BlockPos(0, 2, 0);
    private static final BlockPos TRANSPORT = new BlockPos(1, 1, 0);
    private static final BlockPos JUNCTION = new BlockPos(1, 2, 0);
    private static final BlockPos SUPPLIER = new BlockPos(2, 1, 0);
    private static final BlockPos REFINERY = new BlockPos(3, 1, 0);

    // ==================== Setup helpers ====================

    private static void placeNetwork(GameTestHelper context, long sourceFillMb) {
        context.setBlock(REFINERY, LogisticsAutomation.BLOCK.REFINERY);
        context.setBlock(SUPPLIER, LogisticsPipe.BLOCK.FLUID_SUPPLIER_LOGISTICS_PIPE);
        context.setBlock(TRANSPORT, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(PROVIDER, LogisticsPipe.BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE);
        context.setBlock(SOURCE_TANK, LogisticsPipe.BLOCK.GLASS_TANK);

        GlassTankBlockEntity source = context.getBlockEntity(SOURCE_TANK, GlassTankBlockEntity.class);
        source.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(sourceFillMb));
    }

    /** Place a Power Junction with a specific stored energy (capped at capacity). */
    private static PowerJunctionBlockEntity placeJunction(GameTestHelper context, long energy) {
        context.setBlock(JUNCTION, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = context.getBlockEntity(JUNCTION, PowerJunctionBlockEntity.class);
        chargeTo(junction, energy);
        return junction;
    }

    private static void chargeTo(PowerJunctionBlockEntity junction, long energy) {
        IEnergyStorage es = junction.energyStorage(null);
        long remaining = energy;
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted <= 0) break;
            remaining -= inserted;
        }
    }

    private static FluidSupplierModule supplierModule(GameTestHelper context) {
        PipeBlockEntity pipe = context.getBlockEntity(SUPPLIER, PipeBlockEntity.class);
        if (pipe == null || !(pipe.getBlockState().getBlock() instanceof PipeBlock pipeBlock)
                || pipeBlock.getPipe() == null) {
            throw new IllegalStateException("Fluid supplier pipe missing at supplier position");
        }
        FluidSupplierModule module = pipeBlock.getPipe().getModule(FluidSupplierModule.class, pipe);
        if (module == null) {
            throw new IllegalStateException("FluidSupplierModule missing from pipe at supplier position");
        }
        return module;
    }

    private static PipeContext supplierCtx(GameTestHelper context) {
        return context.getBlockEntity(SUPPLIER, PipeBlockEntity.class).createContext();
    }

    private static void configureSupplier(GameTestHelper context, long targetMb) {
        FluidSupplierModule module = supplierModule(context);
        PipeContext ctx = supplierCtx(context);
        module.setFilterFluid(ctx, Fluids.WATER);
        module.setTargetMb(ctx, targetMb);
    }

    /** mB of water currently in the machine (refinery input) tank. */
    private static long machineWaterMb(GameTestHelper context) {
        RefineryBlockEntity refinery = context.getBlockEntity(REFINERY, RefineryBlockEntity.class);
        IFluidStorage storage = refinery.fluidStorage(null);
        long total = 0;
        for (IFluidView view : storage.contents()) {
            if (view.resource().getFluid() == Fluids.WATER) {
                total += view.amount();
            }
        }
        return FluidUnits.toMillibuckets(total);
    }

    private static long maxMb() {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB);
    }

    /** Outstanding mB (queued + in-transit) for water ordered by the supplier — already mB, no conversion. */
    private static long pendingMb(GameTestHelper context) {
        PipeNetwork network = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(SUPPLIER));
        if (network == null) return 0;
        return network.getFluidOrderedMbFor(context.absolutePos(SUPPLIER), Fluids.WATER);
    }

    // ==================== Tests ====================

    /**
     * Exact fill, no rounding: the supplier orders exactly the deficit (not rounded up to any packet
     * multiple), fills the machine tank to exactly the target, and leaves an empty buffer. This
     * directly replaces the old quantum-rounding behavior (packets no longer have a minimum size).
     */
    public static void testDryRestockFillsToTarget(GameTestHelper context) {
        // Capped by the refinery's real input tank capacity — a "full batch" (maxMb * BATCH_CAP) can
        // exceed what any given machine can physically hold once maxMb is large (e.g. the default 5000
        // mB packet max already exceeds the refinery's 4000 mB input tank on its own).
        long inputCapacityMb = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_INPUT_TANK_MB);
        long target = Math.min(maxMb() * FluidSupplierModule.BATCH_CAP, inputCapacityMb);

        placeNetwork(context, maxMb() * 20);
        placeJunction(context, PowerJunctionBlockEntity.CAPACITY);

        context.runAfterDelay(15, () -> configureSupplier(context, target));

        context.succeedWhen(() -> {
            long tankMb = machineWaterMb(context);
            if (tankMb != target) {
                throw context.assertionException("Empty tank should restock to target " + target + " mB, was " + tankMb);
            }
            long bufferMb = supplierModule(context).getBufferMb(supplierCtx(context));
            if (bufferMb != 0) {
                throw context.assertionException("Buffer should be empty once the target is reached, was " + bufferMb);
            }
        });
    }

    /**
     * A non-round deficit (e.g. 137 mB) is ordered and fulfilled exactly — proving "no minimum packet
     * size" end to end at the consumer, not just rounded down to the nearest packet multiple.
     */
    public static void testExactNonRoundDeficitIsOrderedAndFilled(GameTestHelper context) {
        long target = 137; // deliberately not a multiple of anything packet-sized

        placeNetwork(context, maxMb() * 5);
        placeJunction(context, PowerJunctionBlockEntity.CAPACITY);

        context.runAfterDelay(15, () -> configureSupplier(context, target));

        // Shortly after the order is placed, the full 137 mB deficit should already be accounted for
        // across pending + buffered + tank — not partially ordered or rounded down to a packet multiple.
        // The final succeedWhen below checks the exact number lands only in the tank, with zero buffer.
        context.runAfterDelay(21, () -> {
            long pending = pendingMb(context);
            long buffered = supplierModule(context).getBufferMb(supplierCtx(context));
            if (pending + buffered + machineWaterMb(context) < target) {
                context.fail("Deficit should be ordered promptly; pending=" + pending);
            }
        });

        context.succeedWhen(() -> {
            long tankMb = machineWaterMb(context);
            if (tankMb != target) {
                throw context.assertionException("Tank should reach the exact non-round target " + target + " mB, was " + tankMb);
            }
            long bufferMb = supplierModule(context).getBufferMb(supplierCtx(context));
            if (bufferMb != 0) {
                throw context.assertionException("Buffer should be empty, no stray remainder, was " + bufferMb);
            }
        });
    }

    // Note: "exactly-once acknowledgement across several physical packets sharing one delivery id" is
    // deliberately NOT tested end-to-end against the refinery here — with the default 5000 mB packet
    // max exceeding the refinery's 4000 mB input tank, a single order into this machine can never
    // require more than one physical packet, so "two full + one tail" isn't reachable through this
    // destination. That scenario is covered instead by `FluidProviderGameTest
    // #testMixedFullPlusTailDispatchChargesPerPhysicalPacket` (a plain chest destination, unconstrained
    // by machine tank capacity) and by `FluidOrderBookTest`'s multi-packet/multi-tick unit coverage.

    /**
     * No over-reserve: across many ticks the supplier never has more fluid held plus on order than its
     * buffer can hold (held + pending ≤ capacity). Target exceeds a single batch so ordering stays busy.
     */
    public static void testNeverOrdersMoreThanBufferCanHold(GameTestHelper context) {
        long capacityMb = (long) FluidSupplierModule.BATCH_CAP * maxMb();

        placeNetwork(context, maxMb() * 40);
        placeJunction(context, PowerJunctionBlockEntity.CAPACITY);

        context.runAfterDelay(15, () -> configureSupplier(context, maxMb() * 12));

        // Poll the invariant every 10 ticks; fail immediately if it is ever violated.
        for (long tick = 25; tick <= 170; tick += 10) {
            context.runAfterDelay(tick, () -> {
                long heldMb = supplierModule(context).getBufferMb(supplierCtx(context));
                long onOrderMb = pendingMb(context);
                if (heldMb + onOrderMb > capacityMb) {
                    context.fail("Over-reserved: held " + heldMb + " + pending " + onOrderMb
                            + " mB exceeds buffer capacity " + capacityMb + " mB");
                }
            });
        }
        context.runAfterDelay(180, context::succeed);
    }

    /**
     * Insufficient energy: the network holds only enough energy for the provider to mint packets, none
     * left for the supplier to "pay" for them. Delivered packets are accepted into the buffer but no
     * fluid reaches the machine tank. Once power is restored the buffer is paid down and deposited, with
     * no fluid lost along the way.
     */
    public static void testInsufficientEnergyHoldsFluidThenDepositsWhenPowered(GameTestHelper context) {
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
        // Capped by the refinery's real input tank capacity — see testDryRestockFillsToTarget.
        long inputCapacityMb = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_INPUT_TANK_MB);
        long target = Math.min(maxMb() * FluidSupplierModule.BATCH_CAP, inputCapacityMb);
        // How many physical packets a single order for `target` mB actually chunks into.
        long packetsForTarget = target / maxMb() + (target % maxMb() > 0 ? 1 : 0);

        placeNetwork(context, maxMb() * 20);
        // Exactly enough for the provider to mint every packet for this target, nothing for the
        // supplier to pay.
        PowerJunctionBlockEntity junction = placeJunction(context, rf * packetsForTarget);

        context.runAfterDelay(15, () -> configureSupplier(context, target));

        // Phase 1: packets get accepted (buffer fills) but stay unpaid, so the tank never fills.
        context.runAfterDelay(150, () -> {
            long tankMb = machineWaterMb(context);
            long bufferMb = supplierModule(context).getBufferMb(supplierCtx(context));
            if (tankMb != 0) {
                context.fail("Unpaid fluid must not reach the machine tank, but tank held " + tankMb + " mB");
                return;
            }
            if (bufferMb <= 0) {
                context.fail("Delivered packets should be held in the buffer while unpaid, buffer was " + bufferMb);
                return;
            }
            // Restore power so the supplier can pay down the buffer and deposit.
            chargeTo(junction, PowerJunctionBlockEntity.CAPACITY);
        });

        // Phase 2: with power restored, the tank fills to target and the buffer empties (no loss).
        context.succeedWhen(() -> {
            long tankMb = machineWaterMb(context);
            long bufferMb = supplierModule(context).getBufferMb(supplierCtx(context));
            if (tankMb != target) {
                throw context.assertionException("Tank should fill to " + target + " mB after power, was " + tankMb);
            }
            if (bufferMb != 0) {
                throw context.assertionException("Buffer should be empty after depositing, was " + bufferMb);
            }
        });
    }

    /**
     * No real room: the machine tank is already full to its physical capacity, but the configured
     * target is set higher than that capacity. The supplier must stop ordering rather than
     * perpetually re-requesting fluid the tank can never actually hold.
     */
    public static void testStopsOrderingWhenTankHasNoRealRoom(GameTestHelper context) {
        long inputCapacityMb = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_INPUT_TANK_MB);

        placeNetwork(context, maxMb() * 40);
        placeJunction(context, PowerJunctionBlockEntity.CAPACITY);

        context.runAfterDelay(5, () -> {
            RefineryBlockEntity refinery = context.getBlockEntity(REFINERY, RefineryBlockEntity.class);
            IFluidStorage input = refinery.fluidStorage(null);
            input.insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(inputCapacityMb), false);
        });

        // Target well above the tank's real capacity, so the arithmetic shortfall never closes.
        context.runAfterDelay(15, () -> configureSupplier(context, inputCapacityMb * 2));

        // Poll for a while; ordering must stay at zero the whole time since the tank has no real room.
        for (long tick = 30; tick <= 180; tick += 20) {
            long checkTick = tick;
            context.runAfterDelay(checkTick, () -> {
                if (pendingMb(context) != 0) {
                    context.fail("Supplier should not order fluid the full tank has no room for, tick " + checkTick);
                }
            });
        }
        context.runAfterDelay(190, context::succeed);
    }

    // Note: "changing fluid_packet_max_mb mid-flight doesn't corrupt accounting" is intentionally NOT
    // covered by a gametest here. Gametest batches run many tests concurrently against the same JVM's
    // static config state (confirmed via debug logging: a config mutation in one test measurably
    // corrupted unrelated tests' packet chunking mid-run) — there is no safe way to mutate a global
    // config value from within a single gametest without risking cross-test interference. The invariant
    // itself is instead proven structurally: `PipeDataComponentsTest` verifies every `FluidPacket`
    // always carries its own real, positive `amountMb`, and `FluidSupplierModule.onTransferToStorage`
    // (see source) accepts any packet matching the filter fluid without comparing against the *current*
    // config value at all — acceptance and bookkeeping are architecturally independent of live config.
}
