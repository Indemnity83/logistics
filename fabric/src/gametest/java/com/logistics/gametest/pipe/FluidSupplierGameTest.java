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
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.pipe.modules.FluidSupplierModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * Tick-based game tests for the Fluid Supplier Pipe: a logistics pipe placed on a machine tank that
 * keeps that tank stocked by ordering fluid packets from the item logistics network, draining them into
 * an internal buffer, and depositing paid fluid into the machine tank.
 *
 * <p>Layout (y=1): [refinery] ← [fluid_supplier_pipe] ← [transport_pipe] ← [fluid_provider_pipe]
 * with a glass tank (water source) above the provider and a power junction above the transport pipe.
 *
 * <p>Run in-game: /test run logistics-gametest.fluidsuppliergametest.&lt;methodname&gt;
 */
public class FluidSupplierGameTest {

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

        GlassTankBlockEntity source = (GlassTankBlockEntity) context.getBlockEntity(SOURCE_TANK);
        source.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(sourceFillMb));
    }

    /** Place a Power Junction with a specific stored energy (capped at capacity). */
    private static PowerJunctionBlockEntity placeJunction(GameTestHelper context, long energy) {
        context.setBlock(JUNCTION, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = (PowerJunctionBlockEntity) context.getBlockEntity(JUNCTION);
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
        PipeBlockEntity pipe = (PipeBlockEntity) context.getBlockEntity(SUPPLIER);
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
        return ((PipeBlockEntity) context.getBlockEntity(SUPPLIER)).createContext();
    }

    private static void configureSupplier(GameTestHelper context, long targetMb) {
        FluidSupplierModule module = supplierModule(context);
        PipeContext ctx = supplierCtx(context);
        module.setFilterFluid(ctx, Fluids.WATER);
        module.setTargetMb(ctx, targetMb);
    }

    /** mB of water currently in the machine (refinery input) tank. */
    private static long machineWaterMb(GameTestHelper context) {
        RefineryBlockEntity refinery = (RefineryBlockEntity) context.getBlockEntity(REFINERY);
        IFluidStorage storage = refinery.fluidStorage(null);
        long total = 0;
        for (IFluidView view : storage.contents()) {
            if (view.resource().getFluid() == Fluids.WATER) {
                total += view.amount();
            }
        }
        return FluidUnits.toMillibuckets(total);
    }

    private static long quantum() {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_QUANTUM_MB);
    }

    private static long pendingPackets(GameTestHelper context) {
        var network = com.logistics.pipe.network.NetworkRegistry.getNetwork(
                context.getLevel(), context.absolutePos(SUPPLIER));
        if (network == null) return 0;
        ItemStack packet = new ItemStack(LogisticsPipe.ITEM.FLUID_PACKET);
        packet.set(LogisticsPipe.DATA.FLUID_PACKET, new FluidPacket(Fluids.WATER, quantum()));
        return network.getOrderedAmountFor(context.absolutePos(SUPPLIER), packet);
    }

    // ==================== Tests ====================

    /**
     * Target rounding: target 300 mB with a 250 mB quantum. The supplier orders two packets (500 mB),
     * deposits 300 mB into the machine tank (stopping exactly at the target), and retains the remaining
     * 200 mB in its buffer — then stops ordering.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 260)
    public void testTargetRoundingRetainsRemainderInBuffer(GameTestHelper context) {
        long quantum = quantum();
        long target = quantum + 50; // 300 with the default 250 quantum
        long remainder = 2 * quantum - target; // 200

        placeNetwork(context, quantum * 20);
        placeJunction(context, PowerJunctionBlockEntity.CAPACITY);

        context.runAfterDelay(15, () -> configureSupplier(context, target));

        context.succeedWhen(() -> {
            long tankMb = machineWaterMb(context);
            long bufferMb = supplierModule(context).getBufferMb(supplierCtx(context));
            context.assertTrue(tankMb == target,
                    "Machine tank should stop at target " + target + " mB, was " + tankMb);
            context.assertTrue(bufferMb == remainder,
                    "Buffer should retain " + remainder + " mB, was " + bufferMb);
            context.assertTrue(pendingPackets(context) == 0,
                    "Supplier should stop ordering once satisfied");
        });
    }

    /**
     * No over-reserve: across many ticks the supplier never has more fluid held plus on order than its
     * buffer can hold (held + pending ≤ capacity). Target exceeds a single batch so ordering stays busy.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testNeverOrdersMoreThanBufferCanHold(GameTestHelper context) {
        long quantum = quantum();
        long capacityMb = (long) FluidSupplierModule.BATCH_CAP * quantum;

        placeNetwork(context, quantum * 40);
        placeJunction(context, PowerJunctionBlockEntity.CAPACITY);

        context.runAfterDelay(15, () -> configureSupplier(context, quantum * 12));

        // Poll the invariant every 10 ticks; fail immediately if it is ever violated.
        for (long tick = 25; tick <= 170; tick += 10) {
            context.runAfterDelay(tick, () -> {
                long heldMb = supplierModule(context).getBufferMb(supplierCtx(context));
                long onOrderMb = pendingPackets(context) * quantum;
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
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 400)
    public void testInsufficientEnergyHoldsFluidThenDepositsWhenPowered(GameTestHelper context) {
        long quantum = quantum();
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
        long target = quantum * FluidSupplierModule.BATCH_CAP; // 1000: one full batch

        placeNetwork(context, quantum * 20);
        // Exactly enough for the provider to mint BATCH_CAP packets, nothing for the supplier to pay.
        PowerJunctionBlockEntity junction = placeJunction(context, rf * FluidSupplierModule.BATCH_CAP);

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
            context.assertTrue(tankMb == target,
                    "Tank should fill to " + target + " mB after power, was " + tankMb);
            context.assertTrue(bufferMb == 0,
                    "Buffer should be empty after depositing, was " + bufferMb);
        });
    }

    /**
     * Dry restock: an empty machine tank with a set filter restocks up to exactly the target.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 260)
    public void testDryRestockFillsToTarget(GameTestHelper context) {
        long quantum = quantum();
        long target = quantum * FluidSupplierModule.BATCH_CAP; // 1000

        placeNetwork(context, quantum * 20);
        placeJunction(context, PowerJunctionBlockEntity.CAPACITY);

        context.runAfterDelay(15, () -> configureSupplier(context, target));

        context.succeedWhen(() -> {
            long tankMb = machineWaterMb(context);
            context.assertTrue(tankMb == target,
                    "Empty tank should restock to target " + target + " mB, was " + tankMb);
        });
    }
}
