package com.logistics.gametest.power;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.power.cable.CableBlock;
import com.logistics.power.cable.CableBlockEntity;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.core.engine.block.entity.RedstoneEngineBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import team.reborn.energy.api.EnergyStorage;

/**
 * Game tests for power cables.
 */
public class CableGameTest {
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCablePlacementExposesEnergyStorage(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);

        CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
        if (cable == null) {
            context.fail("Copper cable should have block entity");
            return;
        }

        for (Direction direction : Direction.values()) {
            EnergyStorage storage = findStorage(context, cablePos, direction);
            if (storage == null) {
                context.fail("Copper cable should expose storage from " + direction);
                return;
            }
            if (!storage.supportsInsertion()) {
                context.fail("Copper cable should support insertion from " + direction);
                return;
            }
            if (storage.supportsExtraction()) {
                context.fail("Copper cable should not expose extractable battery storage from " + direction);
                return;
            }
            if (storage.getAmount() != 0L || storage.getCapacity() != 0L) {
                context.fail("Copper cable should report zero stored energy and capacity");
                return;
            }
        }

        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testAbortedCableTransactionAfterTickDoesNotReachCreativeSink(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos sinkPos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
        CreativeSinkBlockEntity sink = (CreativeSinkBlockEntity) context.getBlockEntity(sinkPos);
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

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 30)
    public void testInsertedCableEnergyPassesThroughToMachine(GameTestHelper context) {
        BlockPos sourceCablePos = new BlockPos(1, 1, 1);
        BlockPos relayCablePos = new BlockPos(2, 1, 1);
        BlockPos machinePos = new BlockPos(3, 1, 1);

        context.setBlock(sourceCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(relayCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);

        CableBlockEntity sourceCable = (CableBlockEntity) context.getBlockEntity(sourceCablePos);
        MaceratorBlockEntity machine = (MaceratorBlockEntity) context.getBlockEntity(machinePos);
        if (sourceCable == null || machine == null) {
            context.fail("Expected cable and machine block entities");
            return;
        }
        giveMaceratorWork(machine);

        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = findStorage(context, sourceCablePos, Direction.WEST).insert(640L, transaction);
            if (inserted <= 0) {
                context.fail("Cable network should pass inserted energy through to the machine");
                return;
            }
            transaction.commit();
        }

        long machineEnergy = machine.energyStorage(Direction.WEST).getAmount();
        if (machineEnergy <= 0) {
            context.fail("Cable network should deliver inserted energy to machine, got: " + machineEnergy);
            return;
        }
        if (sourceCable.energyStorage(Direction.WEST).getAmount() != 0) {
            context.fail("Cable should not retain delivered energy");
            return;
        }
        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCableChargesIdleMachineBuffer(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos machinePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);

        CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
        MaceratorBlockEntity machine = (MaceratorBlockEntity) context.getBlockEntity(machinePos);
        if (cable == null || machine == null) {
            context.fail("Expected cable and idle machine block entities");
            return;
        }

        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = findStorage(context, cablePos, Direction.WEST).insert(30L, transaction);
            if (inserted != 30L) {
                context.fail("Idle machine buffer should accept cable energy, got: " + inserted);
                return;
            }
            transaction.commit();
        }

        long machineEnergy = machine.energyStorage(Direction.WEST).getAmount();
        if (machineEnergy != 30L) {
            context.fail("Idle machine buffer should charge without work, got: " + machineEnergy);
            return;
        }
        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testAbortedCableTransactionDoesNotReachCreativeSink(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos sinkPos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
        CreativeSinkBlockEntity sink = (CreativeSinkBlockEntity) context.getBlockEntity(sinkPos);
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

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCableInsertionDistributesByReportedDemand(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos smallSinkPos = new BlockPos(2, 1, 1);
        BlockPos largeSinkPos = new BlockPos(1, 1, 2);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(smallSinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);
        context.setBlock(largeSinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
        CreativeSinkBlockEntity smallSink = (CreativeSinkBlockEntity) context.getBlockEntity(smallSinkPos);
        CreativeSinkBlockEntity largeSink = (CreativeSinkBlockEntity) context.getBlockEntity(largeSinkPos);
        if (cable == null || smallSink == null || largeSink == null) {
            context.fail("Expected cable and creative sink block entities");
            return;
        }
        setSinkDrainRate(largeSink, 10L, context);

        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = findStorage(context, cablePos, Direction.WEST).insert(9L, transaction);
            if (inserted != 9L) {
                context.fail("Cable should accept all energy demanded by sinks, got: " + inserted);
                return;
            }
            transaction.commit();
        }

        long smallDemand = smallSink.networkDemandPerTick();
        long largeDemand = largeSink.networkDemandPerTick();
        if (smallDemand != 2L || largeDemand != 4L) {
            context.fail("Cable should split 9 RF across 5/10 demand as 3/6, remaining demands: "
                    + smallDemand + "/" + largeDemand);
            return;
        }

        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMixedTierRouteIsCappedByWeakestCable(GameTestHelper context) {
        BlockPos enderCablePos = new BlockPos(1, 1, 1);
        BlockPos copperCablePos = new BlockPos(2, 1, 1);
        BlockPos sinkPos = new BlockPos(3, 1, 1);

        context.setBlock(enderCablePos, LogisticsPower.BLOCK.ENDER_CABLE);
        context.setBlock(copperCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity enderCable = (CableBlockEntity) context.getBlockEntity(enderCablePos);
        CreativeSinkBlockEntity sink = (CreativeSinkBlockEntity) context.getBlockEntity(sinkPos);
        if (enderCable == null || sink == null) {
            context.fail("Expected ender cable and creative sink block entities");
            return;
        }
        sink.setUnlimitedDrainRate();

        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = findStorage(context, enderCablePos, Direction.WEST).insert(60L, transaction);
            if (inserted != 30L) {
                context.fail("Ender-to-copper route should be capped at 30 RF/t, got: " + inserted);
                return;
            }
            transaction.commit();
        }

        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = findStorage(context, enderCablePos, Direction.WEST).insert(60L, transaction);
            if (inserted != 0L) {
                context.fail("Copper bottleneck should be spent for the tick, got extra: " + inserted);
                return;
            }
            transaction.commit();
        }

        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testCreativeEnginePowersCableNetwork(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(0, 1, 1);
        BlockPos firstCablePos = new BlockPos(1, 1, 1);
        BlockPos secondCablePos = new BlockPos(2, 1, 1);
        BlockPos machinePos = new BlockPos(3, 1, 1);

        context.setBlock(firstCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(secondCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        CableBlockEntity firstCable = (CableBlockEntity) context.getBlockEntity(firstCablePos);
        CableBlockEntity secondCable = (CableBlockEntity) context.getBlockEntity(secondCablePos);
        MaceratorBlockEntity machine = (MaceratorBlockEntity) context.getBlockEntity(machinePos);
        if (firstCable == null || secondCable == null || machine == null) {
            context.fail("Expected cable and machine block entities");
            return;
        }
        giveMaceratorWork(machine);

        context.runAfterDelay(20, () -> {
            long machineEnergy = machine.energyStorage(Direction.WEST).getAmount();
            if (machineEnergy <= 0) {
                context.fail("Creative engine should power machine through cable network");
                return;
            }
            if (firstCable.energyStorage(Direction.WEST).getAmount() != 0
                    || secondCable.energyStorage(Direction.WEST).getAmount() != 0) {
                context.fail("Cables should not retain creative engine energy");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testCableNetworkStopsAtRemovedCable(GameTestHelper context) {
        BlockPos sourceCablePos = new BlockPos(1, 1, 1);
        BlockPos removedCablePos = new BlockPos(2, 1, 1);
        BlockPos downstreamCablePos = new BlockPos(3, 1, 1);
        BlockPos machinePos = new BlockPos(4, 1, 1);

        context.setBlock(sourceCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(removedCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(downstreamCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);

        context.runAfterDelay(2, () -> {
            context.setBlock(removedCablePos, Blocks.AIR);

            CableBlockEntity sourceCable = (CableBlockEntity) context.getBlockEntity(sourceCablePos);
            CableBlockEntity downstreamCable = (CableBlockEntity) context.getBlockEntity(downstreamCablePos);
            MaceratorBlockEntity machine = (MaceratorBlockEntity) context.getBlockEntity(machinePos);
            if (sourceCable == null || downstreamCable == null || machine == null) {
                context.fail("Expected source cable, downstream cable, and machine block entities");
                return;
            }
            giveMaceratorWork(machine);

            EnergyStorage sourceStorage = findStorage(context, sourceCablePos, Direction.WEST);
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = sourceStorage.insert(640L, transaction);
                if (inserted != 0) {
                    context.fail("Split cable network should reject energy without connected consumers, got: " + inserted);
                    return;
                }
                transaction.commit();
            }

            context.runAfterDelay(10, () -> {
                long machineEnergy = machine.energyStorage(Direction.WEST).getAmount();
                if (machineEnergy != 0) {
                    context.fail("Removed cable should split network before energy reaches machine, got: " + machineEnergy);
                    return;
                }
                context.succeed();
            });
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCableConnectionUpdatesWhenNeighborOutputRotates(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
        if (cable == null) {
            context.fail("Expected cable block entity");
            return;
        }

        if (cable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.DEVICE) {
            context.fail("Cable should initially connect to engine output");
            return;
        }

        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.NORTH)
                .setValue(AbstractEngineBlock.POWERED, true));

        if (cable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.NONE) {
            context.fail("Cable connection cache should update when neighboring output rotates away");
            return;
        }

        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 30)
    public void testRedstoneEngineIsNotPulledByCableNetwork(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(enginePos, LogisticsCore.BLOCK.REDSTONE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        context.runAfterDelay(20, () -> {
            RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) context.getBlockEntity(enginePos);
            CableBlockEntity cable = (CableBlockEntity) context.getBlockEntity(cablePos);
            if (engine == null || cable == null) {
                context.fail("Expected redstone engine and cable block entities");
                return;
            }
            if (engine.getEnergy() <= 0) {
                context.fail("Cable network should not pull directly from redstone engine buffer");
                return;
            }
            if (cable.energyStorage(Direction.WEST).getAmount() != 0) {
                context.fail("Cable should not buffer redstone engine energy");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 50)
    public void testCableNetworkRejoinsAfterCableIsRestored(GameTestHelper context) {
        BlockPos sourceCablePos = new BlockPos(1, 1, 1);
        BlockPos restoredCablePos = new BlockPos(2, 1, 1);
        BlockPos downstreamCablePos = new BlockPos(3, 1, 1);
        BlockPos machinePos = new BlockPos(4, 1, 1);

        context.setBlock(sourceCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(restoredCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(downstreamCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);

        context.runAfterDelay(2, () -> {
            context.setBlock(restoredCablePos, Blocks.AIR);
            context.runAfterDelay(2, () -> {
                context.setBlock(restoredCablePos, LogisticsPower.BLOCK.COPPER_CABLE);

                CableBlockEntity sourceCable = (CableBlockEntity) context.getBlockEntity(sourceCablePos);
                MaceratorBlockEntity machine = (MaceratorBlockEntity) context.getBlockEntity(machinePos);
                if (sourceCable == null || machine == null) {
                    context.fail("Expected source cable and machine block entities after restoring cable");
                    return;
                }
                giveMaceratorWork(machine);

                try (Transaction transaction = Transaction.openOuter()) {
                    long inserted = findStorage(context, sourceCablePos, Direction.WEST).insert(640L, transaction);
                    if (inserted <= 0) {
                        context.fail("Restored cable should rejoin network and deliver energy");
                        return;
                    }
                    transaction.commit();
                }

                long machineEnergy = machine.energyStorage(Direction.WEST).getAmount();
                if (machineEnergy <= 0) {
                    context.fail("Restored cable network should deliver energy to machine, got: " + machineEnergy);
                    return;
                }
                context.succeed();
            });
        });
    }

    /**
     * Extraction pipes are powered only by a directly-adjacent engine: a cable must not connect to
     * one, and the pipe's energy buffer must stay off the loader grid (so cables, batteries, and
     * other mods cannot find it). The engine's direct path — inserting into {@code energyStorage} —
     * must still fill the buffer.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCableDoesNotPowerExtractionPipe(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos pipePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE);

        PipeBlockEntity pipe = (PipeBlockEntity) context.getBlockEntity(pipePos);
        if (pipe == null) {
            context.fail("Item extractor pipe should have a block entity");
            return;
        }

        // The cable must report NO connection toward the pipe, via the cached path the game actually uses.
        CableBlock cableBlock = (CableBlock) LogisticsPower.BLOCK.COPPER_CABLE;
        CableBlock.ConnectionType connection =
                cableBlock.getConnectionType(context.getLevel(), context.absolutePos(cablePos), Direction.EAST);
        if (connection != CableBlock.ConnectionType.NONE) {
            context.fail("Cable should not connect to an extraction pipe, got: " + connection);
            return;
        }

        // The pipe's energy buffer must be invisible to the loader energy grid.
        EnergyStorage gridView = EnergyStorage.SIDED.find(context.getLevel(), context.absolutePos(pipePos), Direction.WEST);
        if (gridView != null) {
            context.fail("Extraction pipe must not expose energy to the grid; got a non-null storage");
            return;
        }

        // But an engine's direct delivery (insert into energyStorage) still fills the buffer.
        IEnergyStorage buffer = pipe.energyStorage(Direction.WEST);
        if (buffer == null || buffer.insert(10, false) <= 0 || buffer.getAmount() <= 0) {
            context.fail("Engine direct delivery into the pipe's energy buffer should still work");
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
