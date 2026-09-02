package com.logistics.gametest.power;

import com.logistics.LogisticsPower;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import team.reborn.energy.api.EnergyStorage;

/**
 * Game tests for power cables.
 */
public class CableGameTest {
    @GameTest
    public void testCablePlacementExposesEnergyStorage(GameTestHelper context) {
        CableGameTestBody.testCablePlacementExposesEnergyStorage(context);
    }

    // loader-only: asserts Fabric Transaction abort semantics; NeoForge's simulate flag is not equivalent
    @GameTest
    public void testAbortedCableTransactionAfterTickDoesNotReachCreativeSink(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos sinkPos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        CreativeSinkBlockEntity sink = context.getBlockEntity(sinkPos, CreativeSinkBlockEntity.class);
        if (cable == null || sink == null) {
            context.fail("Expected cable and creative sink block entities");
            return;
        }

        long demandBefore = sink.networkDemandPerTick();
        if (demandBefore <= 0) {
            context.fail("Creative sink demand must be positive for cross-tick abort transaction test");
            return;
        }

        Transaction transaction = Transaction.openOuter();
        long inserted = findStorage(context, cablePos, Direction.WEST).insert(demandBefore, transaction);
        if (inserted != demandBefore) {
            transaction.close();
            context.fail("Cable should reserve creative sink demand inside transaction, got: " + inserted);
            return;
        }

        CreativeSinkBlockEntity.tick(context.getLevel(), context.absolutePos(sinkPos), context.getBlockState(sinkPos), sink);
        transaction.close();

        long demandAfterAbort = sink.networkDemandPerTick();
        if (demandAfterAbort != demandBefore) {
            context.fail("Aborted cross-tick transaction should restore sink demand, got: " + demandAfterAbort);
            return;
        }

        long energyReceived = sink.energyReceivedLastTick();
        if (energyReceived != 0L) {
            context.fail("Aborted cross-tick transaction should not count as received energy, got: "
                    + energyReceived);
            return;
        }
        context.succeed();
    }

    @GameTest
    public void testInsertedCableEnergyPassesThroughToMachine(GameTestHelper context) {
        CableGameTestBody.testInsertedCableEnergyPassesThroughToMachine(context);
    }

    @GameTest
    public void testCableChargesIdleMachineBuffer(GameTestHelper context) {
        CableGameTestBody.testCableChargesIdleMachineBuffer(context);
    }

    // loader-only: asserts Fabric Transaction abort semantics; NeoForge's simulate flag is not equivalent
    @GameTest
    public void testAbortedCableTransactionDoesNotReachCreativeSink(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos sinkPos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        CreativeSinkBlockEntity sink = context.getBlockEntity(sinkPos, CreativeSinkBlockEntity.class);
        if (cable == null || sink == null) {
            context.fail("Expected cable and creative sink block entities");
            return;
        }

        long demandBefore = sink.networkDemandPerTick();
        if (demandBefore <= 0) {
            context.fail("Creative sink demand must be positive for abort transaction test");
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = findStorage(context, cablePos, Direction.WEST).insert(demandBefore, transaction);
            if (inserted != demandBefore) {
                context.fail("Cable should reserve creative sink demand inside transaction, got: " + inserted);
                return;
            }
        }

        long demandAfterAbort = sink.networkDemandPerTick();
        if (demandAfterAbort != demandBefore) {
            context.fail("Aborted cable transaction should not count as sink input, got demand: " + demandAfterAbort);
            return;
        }

        context.succeed();
    }

    @GameTest
    public void testCableInsertionDistributesByReportedDemand(GameTestHelper context) {
        CableGameTestBody.testCableInsertionDistributesByReportedDemand(context);
    }

    @GameTest
    public void testMixedTierRouteIsCappedByWeakestCable(GameTestHelper context) {
        CableGameTestBody.testMixedTierRouteIsCappedByWeakestCable(context);
    }

    @GameTest
    public void testCreativeEnginePowersCableNetwork(GameTestHelper context) {
        CableGameTestBody.testCreativeEnginePowersCableNetwork(context);
    }

    @GameTest
    public void testCableNetworkStopsAtRemovedCable(GameTestHelper context) {
        CableGameTestBody.testCableNetworkStopsAtRemovedCable(context);
    }

    @GameTest
    public void testCableConnectionUpdatesWhenNeighborOutputRotates(GameTestHelper context) {
        CableGameTestBody.testCableConnectionUpdatesWhenNeighborOutputRotates(context);
    }

    @GameTest
    public void testCableConnectsToBufferlessEngineOutputs(GameTestHelper context) {
        CableGameTestBody.testCableConnectsToBufferlessEngineOutputs(context);
    }

    @GameTest
    public void testCableDoesNotConnectToRedstoneEngine(GameTestHelper context) {
        CableGameTestBody.testCableDoesNotConnectToRedstoneEngine(context);
    }

    @GameTest(maxTicks = 60)
    public void testRedstoneEngineIsNotPulledByCableNetwork(GameTestHelper context) {
        CableGameTestBody.testRedstoneEngineIsNotPulledByCableNetwork(context);
    }

    @GameTest
    public void testCableNetworkRejoinsAfterCableIsRestored(GameTestHelper context) {
        CableGameTestBody.testCableNetworkRejoinsAfterCableIsRestored(context);
    }

    /**
     * Extraction pipes are powered only by a directly-adjacent engine: a cable must not connect to
     * one, and the pipe's energy buffer must stay off the loader grid (so cables, batteries, and
     * other mods cannot find it). The engine's direct path — inserting into {@code energyStorage} —
     * must still fill the buffer.
     */
    // loader-only: asserts the pipe's buffer is invisible to Fabric's own energy grid lookup,
    // which NeoForge's capability system has no shared equivalent for. The portable half of this
    // scenario lives in CableGameTestBody#cableDoesNotPowerExtractionPipe.
    @GameTest
    public void testCableDoesNotPowerExtractionPipe(GameTestHelper context) {
        PipeBlockEntity pipe = CableGameTestBody.cableDoesNotPowerExtractionPipe(context);
        if (pipe == null) {
            return;
        }
        EnergyStorage gridView =
                EnergyStorage.SIDED.find(context.getLevel(), context.absolutePos(new BlockPos(2, 1, 1)), Direction.WEST);
        if (gridView != null) {
            context.fail("Extraction pipe must not expose energy to the grid; got a non-null storage");
            return;
        }
        context.succeed();
    }

    private static EnergyStorage findStorage(GameTestHelper ctx, BlockPos relPos, Direction dir) {
        EnergyStorage storage = EnergyStorage.SIDED.find(ctx.getLevel(), ctx.absolutePos(relPos), dir);
        if (storage == null) {
            ctx.fail("No EnergyStorage at " + relPos + " from " + dir);
        }
        return storage;
    }

    private static void giveMaceratorWork(MaceratorBlockEntity machine) {
        machine.setItem(0, new ItemStack(Items.IRON_INGOT));
    }

    private static void setSinkDrainRate(
            CreativeSinkBlockEntity sink, long drainRate, GameTestHelper context) {
        for (int attempts = 0; attempts < 20 && sink.getDrainRate() != drainRate; attempts++) {
            sink.cycleDrainRate();
        }
        if (sink.getDrainRate() != drainRate) {
            context.fail("Could not set creative sink drain rate to " + drainRate
                    + ", got: " + sink.getDrainRate());
        }
    }
}
