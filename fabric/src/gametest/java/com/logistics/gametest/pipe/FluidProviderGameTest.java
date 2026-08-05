package com.logistics.gametest.pipe;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import com.logistics.pipe.modules.SinkModule;
import com.logistics.pipe.network.PipeNetwork;
import com.logistics.pipe.network.NetworkRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;

import java.util.HashSet;
import java.util.Set;

/**
 * Tick-based game tests for Fluid Provider Pipe packet delivery: max-capped, no-minimum packet sizing,
 * per-physical-packet energy accounting, and no packet ever stacking.
 */
public class FluidProviderGameTest {

    /** Fill a Power Junction so the network can pay module energy costs. */
    private static void placeChargedPowerJunction(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = context.getBlockEntity(pos, PowerJunctionBlockEntity.class);
        IEnergyStorage es = junction.energyStorage(null);
        long remaining = PowerJunctionBlockEntity.CAPACITY;
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted <= 0) break;
            remaining -= inserted;
        }
    }

    private static void enableDefaultRoute(GameTestHelper context, BlockPos sinkPos) {
        PipeBlockEntity pipeEntity = context.getBlockEntity(sinkPos, PipeBlockEntity.class);
        if (pipeEntity == null || !(pipeEntity.getBlockState().getBlock() instanceof PipeBlock pipeBlock)
                || pipeBlock.getPipe() == null) {
            throw new IllegalStateException("Basic logistics pipe missing at sink position");
        }
        SinkModule sink = pipeBlock.getPipe().getModule(SinkModule.class, pipeEntity);
        if (sink == null) {
            throw new IllegalStateException("SinkModule missing from pipe at sink position");
        }
        sink.setDefaultRoute(pipeEntity.createContext(), true);
    }

    private static long maxMb() {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PACKET_MAX_MB);
    }

    /** Sum of all FLUID_PACKET item counts across every slot of a chest. */
    private static int packetCount(ChestBlockEntity chest) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(LogisticsPipe.ITEM.FLUID_PACKET)) count += stack.getCount();
        }
        return count;
    }

    /** Every distinct FLUID_PACKET stack in a chest, one entry per slot (packets never stack). */
    private static java.util.List<ItemStack> packetStacks(ChestBlockEntity chest) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(LogisticsPipe.ITEM.FLUID_PACKET)) stacks.add(stack);
        }
        return stacks;
    }

    /** Delivers N max-size packets and charges the configured energy for each one. */
    @GameTest(maxTicks = 160)
    public void testFluidProviderMintsAndDeliversPackets(GameTestHelper context) {
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos tankPos = new BlockPos(0, 2, 0);
        BlockPos transportPos = new BlockPos(1, 1, 0);
        BlockPos sinkPos = new BlockPos(2, 1, 0);
        BlockPos chestPos = new BlockPos(3, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 2, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        placeChargedPowerJunction(context, junctionPos);

        long maxMb = maxMb();
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);
        long fillMb = maxMb * 4;

        GlassTankBlockEntity tank = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
        if (tank == null) {
            context.fail("Glass tank should have a block entity");
            return;
        }
        tank.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(fillMb));

        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);
        long startEnergy = junction.energyStorage(null).getAmount();

        // Wait for the network to form and the provider to advertise supply, then place the order.
        context.runAfterDelay(15, () -> {
            enableDefaultRoute(context, sinkPos);
            PipeNetwork network = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(providerPos));
            if (network == null) {
                context.fail("Network should have formed at the provider by tick 15");
                return;
            }
            network.placeFluidOrder(Fluids.WATER, fillMb, context.absolutePos(sinkPos));

            context.succeedWhen(() -> {
                ChestBlockEntity chest = context.getBlockEntity(chestPos, ChestBlockEntity.class);
                int packetCount = packetCount(chest);
                if (packetCount != 4) {
                    throw context.assertionException("Chest should contain exactly 4 fluid packets, found " + packetCount);
                }

                GlassTankBlockEntity t = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
                long remainingMb = FluidUnits.toMillibuckets(t.amount());
                if (remainingMb != fillMb - maxMb * 4) {
                    throw context.assertionException(
                            "Tank should be drained by exactly 4 max-size packets, remaining mB: " + remainingMb);
                }

                long energyNow = junction.energyStorage(null).getAmount();
                if (energyNow != startEnergy - 4 * rf) {
                    throw context.assertionException("Network energy should be charged exactly 4 times per packet");
                }
            });
        });
    }

    /**
     * No minimum: a tank holding less than one max-size packet's worth still dispatches — its full
     * remainder ships as a single, correctly-sized packet, draining the tank to zero. This is the
     * direct inverse of the old "below one quantum never dispatches" behavior.
     */
    @GameTest(maxTicks = 120)
    public void testFluidProviderDispatchesBelowOnePacketSize(GameTestHelper context) {
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos tankPos = new BlockPos(0, 2, 0);
        BlockPos transportPos = new BlockPos(1, 1, 0);
        BlockPos sinkPos = new BlockPos(2, 1, 0);
        BlockPos chestPos = new BlockPos(3, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 2, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        placeChargedPowerJunction(context, junctionPos);

        long fillMb = maxMb() - 1;

        GlassTankBlockEntity tank = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
        if (tank == null) {
            context.fail("Glass tank should have a block entity");
            return;
        }
        tank.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(fillMb));

        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);
        long startEnergy = junction.energyStorage(null).getAmount();
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);

        context.runAfterDelay(15, () -> {
            enableDefaultRoute(context, sinkPos);
            PipeNetwork network = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(providerPos));
            if (network == null) {
                context.fail("Network should have formed at the provider by tick 15");
                return;
            }
            network.placeFluidOrder(Fluids.WATER, fillMb, context.absolutePos(sinkPos));
        });

        context.succeedWhen(() -> {
            GlassTankBlockEntity t = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
            long remainingMb = FluidUnits.toMillibuckets(t.amount());
            if (remainingMb != 0) {
                throw context.assertionException("Sub-max tank should fully drain, remaining mB: " + remainingMb);
            }

            ChestBlockEntity chest = context.getBlockEntity(chestPos, ChestBlockEntity.class);
            int packetCount = packetCount(chest);
            if (packetCount != 1) {
                throw context.assertionException("Exactly one packet should be dispatched, found " + packetCount);
            }
            java.util.List<ItemStack> stacks = packetStacks(chest);
            FluidPacket data = stacks.get(0).get(LogisticsPipe.DATA.FLUID_PACKET);
            if (data == null || data.amountMb() != fillMb) {
                throw context.assertionException("Dispatched packet should carry the full remainder ("
                        + fillMb + " mB), was " + (data == null ? "null" : data.amountMb()));
            }

            long energyNow = junction.energyStorage(null).getAmount();
            if (energyNow != startEnergy - rf) {
                throw context.assertionException("Exactly one packet's worth of energy should be charged");
            }
        });
    }

    /**
     * Mixed full-plus-tail dispatch charges exactly one endpoint cost per physical packet: a dispatch
     * producing 2 full-size packets + 1 tail packet (3 physical packets) must charge exactly {@code 3 *
     * rf}, and mint exactly three distinct, single-count item stacks (never combined into one stack).
     */
    @GameTest(maxTicks = 160)
    public void testMixedFullPlusTailDispatchChargesPerPhysicalPacket(GameTestHelper context) {
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos tankPos = new BlockPos(0, 2, 0);
        BlockPos transportPos = new BlockPos(1, 1, 0);
        BlockPos sinkPos = new BlockPos(2, 1, 0);
        BlockPos chestPos = new BlockPos(3, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 2, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        placeChargedPowerJunction(context, junctionPos);

        long maxMb = maxMb();
        long tailMb = maxMb / 2;
        long fillMb = 2 * maxMb + tailMb;
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);

        GlassTankBlockEntity tank = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
        if (tank == null) {
            context.fail("Glass tank should have a block entity");
            return;
        }
        tank.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(fillMb));

        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);
        long startEnergy = junction.energyStorage(null).getAmount();

        context.runAfterDelay(15, () -> {
            enableDefaultRoute(context, sinkPos);
            PipeNetwork network = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(providerPos));
            if (network == null) {
                context.fail("Network should have formed at the provider by tick 15");
                return;
            }
            network.placeFluidOrder(Fluids.WATER, fillMb, context.absolutePos(sinkPos));

            context.succeedWhen(() -> {
                ChestBlockEntity chest = context.getBlockEntity(chestPos, ChestBlockEntity.class);
                java.util.List<ItemStack> stacks = packetStacks(chest);
                if (stacks.size() != 3) {
                    throw context.assertionException("Expected exactly 3 physical packets, found " + stacks.size());
                }

                Set<Long> amounts = new HashSet<>();
                for (ItemStack stack : stacks) {
                    if (stack.getCount() != 1) {
                        throw context.assertionException("Every packet must be count=1 (packets never stack), found "
                                + stack.getCount());
                    }
                    FluidPacket data = stack.get(LogisticsPipe.DATA.FLUID_PACKET);
                    if (data == null) {
                        throw context.assertionException("Packet stack missing its FluidPacket component");
                    }
                    amounts.add(data.amountMb());
                }
                if (!amounts.contains(maxMb) || !amounts.contains(tailMb)) {
                    throw context.assertionException(
                            "Expected packets of " + maxMb + " mB (x2) and " + tailMb + " mB (x1), found " + amounts);
                }

                long energyNow = junction.energyStorage(null).getAmount();
                if (energyNow != startEnergy - 3 * rf) {
                    throw context.assertionException(
                            "Should charge exactly 3 * rf for 3 physical packets, charged "
                                    + (startEnergy - energyNow) + " expected " + (3 * rf));
                }
            });
        });
    }

    /**
     * Flat per-physical-packet energy cost, not per-mB: one dispatch of a full-size packet costs one
     * endpoint charge; ten independently-dispatched small deliveries of the same total volume cost ten
     * — proving cost tracks physical packet events, not aggregate mB moved.
     */
    @GameTest(maxTicks = 300)
    public void testTenSmallDeliveriesCostTenTimesOneBigDelivery(GameTestHelper context) {
        BlockPos providerPos = new BlockPos(0, 1, 0);
        BlockPos tankPos = new BlockPos(0, 2, 0);
        BlockPos transportPos = new BlockPos(1, 1, 0);
        BlockPos sinkPos = new BlockPos(2, 1, 0);
        BlockPos chestPos = new BlockPos(3, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 2, 0);

        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(sinkPos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(transportPos, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.FLUID_PROVIDER_LOGISTICS_PIPE);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        placeChargedPowerJunction(context, junctionPos);

        long maxMb = maxMb();
        long smallMb = Math.max(1, maxMb / 50);
        long rf = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_ENDPOINT_RF_PER_PACKET);

        GlassTankBlockEntity tank = context.getBlockEntity(tankPos, GlassTankBlockEntity.class);
        if (tank == null) {
            context.fail("Glass tank should have a block entity");
            return;
        }
        // Enough for phase 1 (one maxMb delivery) plus phase 2 (ten smallMb deliveries).
        tank.setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(maxMb + 10 * smallMb));

        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);

        // Cross-phase state, captured across separately-scheduled top-level callbacks (each delay here
        // is an absolute tick offset from test start, not relative to the previous callback).
        long[] energyBeforePhase2 = {0};

        context.runAfterDelay(15, () -> enableDefaultRoute(context, sinkPos));

        // Phase 1: one big delivery of exactly maxMb — costs exactly 1 * rf.
        context.runAfterDelay(20, () -> {
            PipeNetwork network = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(providerPos));
            if (network == null) {
                context.fail("Network should have formed at the provider by tick 20");
                return;
            }
            network.placeFluidOrder(Fluids.WATER, maxMb, context.absolutePos(sinkPos));
        });

        context.runAfterDelay(60, () -> {
            long spentPhase1 = PowerJunctionBlockEntity.CAPACITY - junction.energyStorage(null).getAmount();
            if (spentPhase1 != rf) {
                context.fail("One max-size delivery should cost exactly " + rf + " RF, cost " + spentPhase1);
                return;
            }

            energyBeforePhase2[0] = junction.energyStorage(null).getAmount();

            // Phase 2: ten independently-placed small orders — each becomes its own dispatch/packet.
            PipeNetwork network = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(providerPos));
            for (int i = 0; i < 10; i++) {
                network.placeFluidOrder(Fluids.WATER, smallMb, context.absolutePos(sinkPos));
            }
        });

        context.runAfterDelay(180, () -> {
            long spentPhase2 = energyBeforePhase2[0] - junction.energyStorage(null).getAmount();
            if (spentPhase2 != 10 * rf) {
                context.fail("Ten independently-dispatched small deliveries should cost exactly "
                        + (10 * rf) + " RF total, cost " + spentPhase2);
                return;
            }
            context.succeed();
        });
    }
}
