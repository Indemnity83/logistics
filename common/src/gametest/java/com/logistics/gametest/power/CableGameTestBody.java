package com.logistics.gametest.power;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.automation.macerator.MaceratorBlockEntity;
import com.logistics.core.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.power.cable.CableBlock;
import com.logistics.power.cable.CableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Cable topology and engine interaction: which neighbours a cable connects to, how that connection
 * reacts to a neighbour rotating, and which engines the network may and may not draw from.
 */
public class CableGameTestBody {

    /** Ender Cable throughput: the per-tick budget the consumer-before-battery test needs. */
    private static final long CABLE_BUDGET_FOR_TIER_TEST = 120L;

    /**
     * A consumer demand large enough that a battery outbidding it actually starves it.
     *
     * <p>At a small demand the bug hides: the battery banks the engine's output and then pushes the
     * consumer's few RF back out itself on the next tick, so the consumer is served anyway -- just
     * laundered through the battery and billed to the cable twice. Only once the consumer wants
     * enough that the double billing exhausts the cable's budget does it visibly go without.
     */
    private static final long CONSUMER_DRAIN_RATE = 100L;

    public static void testCreativeEnginePowersCableNetwork(GameTestHelper context) {
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

        CableBlockEntity firstCable = context.getBlockEntity(firstCablePos, CableBlockEntity.class);
        CableBlockEntity secondCable = context.getBlockEntity(secondCablePos, CableBlockEntity.class);
        MaceratorBlockEntity machine = context.getBlockEntity(machinePos, MaceratorBlockEntity.class);
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

    public static void testCableConnectionUpdatesWhenNeighborOutputRotates(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
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

    public static void testCableConnectsToBufferlessEngineOutputs(GameTestHelper context) {
        BlockPos steamEnginePos = new BlockPos(1, 1, 1);
        BlockPos steamCablePos = new BlockPos(2, 1, 1);
        BlockPos reactionEnginePos = new BlockPos(4, 1, 1);
        BlockPos reactionCablePos = new BlockPos(5, 1, 1);

        context.setBlock(steamCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(steamEnginePos, LogisticsPower.BLOCK.STEAM_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST));
        context.setBlock(reactionCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(reactionEnginePos, LogisticsPower.BLOCK.REACTION_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST));

        CableBlockEntity steamCable = context.getBlockEntity(steamCablePos, CableBlockEntity.class);
        CableBlockEntity reactionCable = context.getBlockEntity(reactionCablePos, CableBlockEntity.class);
        if (steamCable == null || reactionCable == null) {
            context.fail("Expected cable block entities");
            return;
        }
        if (steamCable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.DEVICE
                || reactionCable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.DEVICE) {
            context.fail("Cables should connect to bufferless engine output faces");
            return;
        }
        context.succeed();
    }

    public static void testCableDoesNotConnectToRedstoneEngine(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(enginePos, LogisticsCore.BLOCK.REDSTONE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST));

        RedstoneEngineBlockEntity engine = context.getBlockEntity(enginePos, RedstoneEngineBlockEntity.class);
        if (engine == null) {
            context.fail("Expected redstone engine block entity");
            return;
        }
        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        if (cable == null) {
            context.fail("Expected cable block entity");
            return;
        }
        if (cable.getCachedConnectionType(Direction.WEST) != CableBlock.ConnectionType.NONE) {
            context.fail("Cable should not connect to redstone engine");
            return;
        }
        context.succeed();
    }

    /**
     * The same layout as {@link #testCreativeEnginePowersCableNetwork}, swapping the engine. A
     * machine with work to do sits behind the cable so the network genuinely wants energy —
     * without a consumer nothing would be pulled either way, and the test would pass without
     * showing that the redstone engine is the reason.
     */
    public static void testRedstoneEngineIsNotPulledByCableNetwork(GameTestHelper context) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos cablePos = new BlockPos(2, 1, 1);
        BlockPos machinePos = new BlockPos(3, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);
        context.setBlock(enginePos, LogisticsCore.BLOCK.REDSTONE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        MaceratorBlockEntity machine = context.getBlockEntity(machinePos, MaceratorBlockEntity.class);
        if (machine == null) {
            context.fail("Expected macerator block entity");
            return;
        }
        giveMaceratorWork(machine);

        long[] engineEnergyWhileDemanded = new long[1];
        context.runAfterDelay(20, () -> {
            RedstoneEngineBlockEntity engine = context.getBlockEntity(enginePos, RedstoneEngineBlockEntity.class);
            if (engine == null) {
                context.fail("Expected redstone engine block entity");
                return;
            }
            if (engine.getEnergy() <= 0) {
                context.fail("Redstone engine should have generated energy to be drained of");
                return;
            }
            engineEnergyWhileDemanded[0] = engine.getEnergy();
        });

        context.runAfterDelay(40, () -> {
            RedstoneEngineBlockEntity engine = context.getBlockEntity(enginePos, RedstoneEngineBlockEntity.class);
            CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
            if (engine == null || cable == null) {
                context.fail("Expected redstone engine and cable block entities");
                return;
            }
            if (machine.energyStorage(Direction.EAST).getAmount() != 0) {
                context.fail("Cable network should not deliver redstone engine energy to a demanding machine");
                return;
            }
            if (engine.getEnergy() < engineEnergyWhileDemanded[0]) {
                context.fail("Redstone engine buffer was drained despite demand on the network");
                return;
            }
            if (cable.energyStorage(Direction.WEST).getAmount() != 0) {
                context.fail("Cable should not buffer redstone engine energy");
                return;
            }
            context.succeed();
        });
    }


    public static void testCablePlacementExposesEnergyStorage(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        if (cable == null) {
            context.fail("Copper cable should have block entity");
            return;
        }

        for (Direction direction : Direction.values()) {
            IEnergyStorage storage = cable.energyStorage(direction);
            if (!storage.canInsert()) {
                context.fail("Copper cable should support insertion from " + direction);
                return;
            }
            if (storage.canExtract()) {
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

    public static void testInsertedCableEnergyPassesThroughToMachine(GameTestHelper context) {
        BlockPos sourceCablePos = new BlockPos(1, 1, 1);
        BlockPos relayCablePos = new BlockPos(2, 1, 1);
        BlockPos machinePos = new BlockPos(3, 1, 1);

        context.setBlock(sourceCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(relayCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);

        CableBlockEntity sourceCable = context.getBlockEntity(sourceCablePos, CableBlockEntity.class);
        MaceratorBlockEntity machine = context.getBlockEntity(machinePos, MaceratorBlockEntity.class);
        if (sourceCable == null || machine == null) {
            context.fail("Expected cable and machine block entities");
            return;
        }
        giveMaceratorWork(machine);

        long inserted = sourceCable.energyStorage(Direction.WEST).insert(640L, false);
        if (inserted <= 0) {
            context.fail("Cable network should pass inserted energy through to the machine");
            return;
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

    public static void testCableChargesIdleMachineBuffer(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos machinePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        MaceratorBlockEntity machine = context.getBlockEntity(machinePos, MaceratorBlockEntity.class);
        if (cable == null || machine == null) {
            context.fail("Expected cable and idle machine block entities");
            return;
        }

        long inserted = cable.energyStorage(Direction.WEST).insert(30L, false);
        if (inserted != 30L) {
            context.fail("Idle machine buffer should accept cable energy, got: " + inserted);
            return;
        }

        long machineEnergy = machine.energyStorage(Direction.WEST).getAmount();
        if (machineEnergy != 30L) {
            context.fail("Idle machine buffer should charge without work, got: " + machineEnergy);
            return;
        }
        context.succeed();
    }

    public static void testCableInsertionDistributesByReportedDemand(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos smallSinkPos = new BlockPos(2, 1, 1);
        BlockPos largeSinkPos = new BlockPos(1, 1, 2);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(smallSinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);
        context.setBlock(largeSinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        CreativeSinkBlockEntity smallSink = context.getBlockEntity(smallSinkPos, CreativeSinkBlockEntity.class);
        CreativeSinkBlockEntity largeSink = context.getBlockEntity(largeSinkPos, CreativeSinkBlockEntity.class);
        if (cable == null || smallSink == null || largeSink == null) {
            context.fail("Expected cable and creative sink block entities");
            return;
        }
        setSinkDrainRate(largeSink, 10L, context);

        long inserted = cable.energyStorage(Direction.WEST).insert(9L, false);
        if (inserted != 9L) {
            context.fail("Cable should accept all energy demanded by sinks, got: " + inserted);
            return;
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

    public static void testMixedTierRouteIsCappedByWeakestCable(GameTestHelper context) {
        BlockPos enderCablePos = new BlockPos(1, 1, 1);
        BlockPos copperCablePos = new BlockPos(2, 1, 1);
        BlockPos sinkPos = new BlockPos(3, 1, 1);

        context.setBlock(enderCablePos, LogisticsPower.BLOCK.ENDER_CABLE);
        context.setBlock(copperCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity enderCable = context.getBlockEntity(enderCablePos, CableBlockEntity.class);
        CreativeSinkBlockEntity sink = context.getBlockEntity(sinkPos, CreativeSinkBlockEntity.class);
        if (enderCable == null || sink == null) {
            context.fail("Expected ender cable and creative sink block entities");
            return;
        }
        sink.setUnlimitedDrainRate();

        long inserted = enderCable.energyStorage(Direction.WEST).insert(60L, false);
        if (inserted != 30L) {
            context.fail("Ender-to-copper route should be capped at 30 RF/t, got: " + inserted);
            return;
        }

        long extra = enderCable.energyStorage(Direction.WEST).insert(60L, false);
        if (extra != 0L) {
            context.fail("Copper bottleneck should be spent for the tick, got extra: " + extra);
            return;
        }

        context.succeed();
    }

    /**
     * A block update landing between two pushes in the same tick rebuilds the cable network. The
     * cap is a property of the cable, not of the network object, so the second push must still be
     * refused — and the tick after must start with a full budget again.
     *
     * <p>A Laser Quarry placing or breaking blocks beside a cable does exactly this from inside the
     * block-entity tick loop.
     */
    public static void testMidTickRebuildKeepsTheThroughputCap(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos sinkPos = new BlockPos(2, 1, 1);
        BlockPos neighborPos = new BlockPos(1, 2, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        CreativeSinkBlockEntity sink = context.getBlockEntity(sinkPos, CreativeSinkBlockEntity.class);
        if (cable == null || sink == null) {
            context.fail("Expected cable and creative sink block entities");
            return;
        }
        sink.setUnlimitedDrainRate();

        long rate = cable.getTransferRate();
        IEnergyStorage endpoint = cable.energyStorage(Direction.WEST);

        long first = endpoint.insert(rate * 2, false);
        if (first != rate) {
            context.fail("Cable should carry its full rating on the first push, got: " + first);
            return;
        }

        // Topology change sourced mid-tick: the neighbour update marks the network manager dirty,
        // so the next push rebuilds the network before it moves any energy.
        context.setBlock(neighborPos, Blocks.STONE);

        long afterRebuild = endpoint.insert(rate * 2, false);
        if (afterRebuild != 0L) {
            context.fail("A mid-tick network rebuild must not refill the cable's budget, got extra: "
                    + afterRebuild);
            return;
        }

        context.runAfterDelay(1, () -> {
            long nextTick = endpoint.insert(rate * 2, false);
            if (nextTick != rate) {
                context.fail("The next tick should start with a full budget, got: " + nextTick);
                return;
            }
            context.succeed();
        });
    }

    public static void testCableNetworkStopsAtRemovedCable(GameTestHelper context) {
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

            CableBlockEntity sourceCable = context.getBlockEntity(sourceCablePos, CableBlockEntity.class);
            CableBlockEntity downstreamCable = context.getBlockEntity(downstreamCablePos, CableBlockEntity.class);
            MaceratorBlockEntity machine = context.getBlockEntity(machinePos, MaceratorBlockEntity.class);
            if (sourceCable == null || downstreamCable == null || machine == null) {
                context.fail("Expected source cable, downstream cable, and machine block entities");
                return;
            }
            giveMaceratorWork(machine);

            long inserted = sourceCable.energyStorage(Direction.WEST).insert(640L, false);
            if (inserted != 0) {
                context.fail("Split cable network should reject energy without connected consumers, got: " + inserted);
                return;
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

    public static void testCableNetworkRejoinsAfterCableIsRestored(GameTestHelper context) {
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

                CableBlockEntity sourceCable = context.getBlockEntity(sourceCablePos, CableBlockEntity.class);
                MaceratorBlockEntity machine = context.getBlockEntity(machinePos, MaceratorBlockEntity.class);
                if (sourceCable == null || machine == null) {
                    context.fail("Expected source cable and machine block entities after restoring cable");
                    return;
                }
                giveMaceratorWork(machine);

                long inserted = sourceCable.energyStorage(Direction.WEST).insert(640L, false);
                if (inserted <= 0) {
                    context.fail("Restored cable should rejoin network and deliver energy");
                    return;
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
     * A cable must not connect to an extraction pipe, but an engine delivering directly into the
     * pipe's buffer still must work.
     *
     * <p>Returns the pipe so a loader wrapper can additionally assert its buffer is invisible to
     * that loader's energy grid — the grid lookup itself has no shared equivalent.
     */
    public static PipeBlockEntity cableDoesNotPowerExtractionPipe(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos pipePos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.ITEM_EXTRACTOR_PIPE);

        PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
        if (pipe == null) {
            context.fail("Item extractor pipe should have a block entity");
            return null;
        }

        // Read the cached path the game itself uses, not a freshly computed answer.
        CableBlock cableBlock = (CableBlock) LogisticsPower.BLOCK.COPPER_CABLE;
        CableBlock.ConnectionType connection =
                cableBlock.getConnectionType(context.getLevel(), context.absolutePos(cablePos), Direction.EAST);
        if (connection != CableBlock.ConnectionType.NONE) {
            context.fail("Cable should not connect to an extraction pipe, got: " + connection);
            return null;
        }

        IEnergyStorage buffer = pipe.energyStorage(Direction.WEST);
        if (buffer == null || buffer.insert(10, false) <= 0 || buffer.getAmount() <= 0) {
            context.fail("Engine direct delivery into the pipe's energy buffer should still work");
            return null;
        }
        return pipe;
    }

    private static void setSinkDrainRate(CreativeSinkBlockEntity sink, long drainRate, GameTestHelper context) {
        for (int attempts = 0; attempts < 20 && sink.getDrainRate() != drainRate; attempts++) {
            sink.cycleDrainRate();
        }
        if (sink.getDrainRate() != drainRate) {
            context.fail("Could not set creative sink drain rate to " + drainRate
                    + ", got: " + sink.getDrainRate());
        }
    }

    /**
     * Verifies a cable network still delivers energy after one of its cables is destroyed, recreated,
     * and reloaded from its saved NBT.
     *
     * <p>Covers {@code CableNetworkManager} recovery rather than persistence: {@code registeredInNetwork}
     * is transient, so the rebuilt block entity must re-register before energy crosses that position
     * again. Loading the saved data changes nothing observable here — see TESTING.md for why, and why
     * the pipe equivalent is the one that covers persistence.
     *
     * <p>Reconstruction, not a chunk unload.
     */
    public static void testCableNetworkSurvivesCableReconstruction(GameTestHelper context) {
        BlockPos sourceCablePos = new BlockPos(1, 1, 1);
        BlockPos rebuiltCablePos = new BlockPos(2, 1, 1);
        BlockPos downstreamCablePos = new BlockPos(3, 1, 1);
        BlockPos machinePos = new BlockPos(4, 1, 1);

        context.setBlock(sourceCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(rebuiltCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(downstreamCablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.MACERATOR);

        HolderLookup.Provider registries = context.getLevel().registryAccess();

        context.runAfterDelay(2, () -> {
            CableBlockEntity original = context.getBlockEntity(rebuiltCablePos, CableBlockEntity.class);
            if (original == null) {
                context.fail("Expected a cable block entity at " + rebuiltCablePos);
                return;
            }

            CompoundTag saved = original.saveCustomOnly(registries);
            context.setBlock(rebuiltCablePos, Blocks.AIR);
            context.setBlock(rebuiltCablePos, LogisticsPower.BLOCK.COPPER_CABLE);

            CableBlockEntity rebuilt = context.getBlockEntity(rebuiltCablePos, CableBlockEntity.class);
            if (rebuilt == null) {
                context.fail("Expected a fresh CableBlockEntity at " + rebuiltCablePos);
                return;
            }
            rebuilt.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));

            // Give the rebuilt cable a tick to re-register with the network manager.
            context.runAfterDelay(2, () -> {
                CableBlockEntity sourceCable = context.getBlockEntity(sourceCablePos, CableBlockEntity.class);
                MaceratorBlockEntity machine = context.getBlockEntity(machinePos, MaceratorBlockEntity.class);
                if (sourceCable == null || machine == null) {
                    context.fail("Expected source cable and machine block entities after reconstruction");
                    return;
                }
                giveMaceratorWork(machine);

                long inserted = sourceCable.energyStorage(Direction.WEST).insert(640L, false);
                if (inserted <= 0) {
                    context.fail("Reconstructed cable should rejoin the network and accept energy");
                    return;
                }

                long machineEnergy = machine.energyStorage(Direction.WEST).getAmount();
                if (machineEnergy <= 0) {
                    context.fail("Energy should reach the machine through the reconstructed cable, got: "
                            + machineEnergy);
                    return;
                }
                context.succeed();
            });
        });
    }

    /**
     * A consumer placed between two pushes in the same tick still receives the second one.
     *
     * <p>The tick's device scan is shared by every push in it, so the network has to notice the
     * neighbourhood changing underneath that scan rather than serving the rest of the tick from a
     * picture taken before the consumer existed.
     */
    public static void testDeviceAddedMidTickReceivesEnergy(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos sinkPos = new BlockPos(2, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        if (cable == null) {
            context.fail("Expected a cable block entity");
            return;
        }

        IEnergyStorage endpoint = cable.energyStorage(Direction.WEST);
        long rate = cable.getTransferRate();

        long withNothingAttached = endpoint.insert(rate, false);
        if (withNothingAttached != 0L) {
            context.fail("A cable with no consumer should accept nothing, got: " + withNothingAttached);
            return;
        }

        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);
        CreativeSinkBlockEntity sink = context.getBlockEntity(sinkPos, CreativeSinkBlockEntity.class);
        if (sink == null) {
            context.fail("Expected a creative sink block entity");
            return;
        }
        sink.setUnlimitedDrainRate();

        long afterPlacing = endpoint.insert(rate, false);
        if (afterPlacing != rate) {
            context.fail("A consumer placed mid-tick should still receive this tick's push, got: " + afterPlacing);
            return;
        }
        context.succeed();
    }

    /**
     * A consumer broken between two pushes in the same tick receives nothing further.
     *
     * <p>The first push is simulated so it finds the sink without spending the tick's throughput
     * budget — after a committed push the second would return zero whether or not the sink was
     * still there, which would prove nothing.
     */
    public static void testDeviceRemovedMidTickReceivesNothing(GameTestHelper context) {
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
        sink.setUnlimitedDrainRate();

        IEnergyStorage endpoint = cable.energyStorage(Direction.WEST);
        long rate = cable.getTransferRate();

        long simulated = endpoint.insert(rate, true);
        if (simulated != rate) {
            context.fail("The sink should be found before it is removed, got: " + simulated);
            return;
        }

        context.setBlock(sinkPos, Blocks.AIR);

        long afterRemoval = endpoint.insert(rate, false);
        if (afterRemoval != 0L) {
            context.fail("Energy must not be sent to a consumer removed mid-tick, got: " + afterRemoval);
            return;
        }
        context.succeed();
    }

    private static void giveMaceratorWork(MaceratorBlockEntity machine) {
        machine.setItem(0, new ItemStack(Items.IRON_INGOT));
    }

    /**
     * A consumer is served before a battery. A battery is not an {@code EnergyDemandProvider}, so its
     * demand is its whole remaining capacity -- up to 100,000 RF -- while a consumer asks only for the
     * few RF it can use this tick. Split pro rata across one flat list the consumer gets a rounding
     * error and the battery soaks up the network.
     *
     * <p>An Ender Cable and a wound-up engine on purpose. At a copper cable's 30 RF/t every
     * {@code floor(ideal)} lands on zero and {@code distributeRemainder} hands the budget out one RF
     * at a time, evenly -- which masks the imbalance entirely. The weights only decide the split once
     * there is enough per tick for the flooring to mean something.
     *
     * <p>The engine goes down last and explicitly {@code POWERED}, facing the cable: {@code setBlock}
     * fires neighbour updates, and an engine recomputes {@code POWERED} from real redstone whenever
     * one arrives.
     */
    public static void testMachineIsServedBeforeABattery(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos sinkPos = new BlockPos(1, 1, 2);
        BlockPos batteryPos = new BlockPos(1, 2, 1);
        BlockPos enginePos = new BlockPos(0, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.ENDER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        CreativeSinkBlockEntity sink = context.getBlockEntity(sinkPos, CreativeSinkBlockEntity.class);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        CreativeEngineBlockEntity engine = context.getBlockEntity(enginePos, CreativeEngineBlockEntity.class);
        if (sink == null || battery == null || engine == null) {
            context.fail("Expected a sink, a battery and an engine");
            return;
        }
        setSinkDrainRate(sink, CONSUMER_DRAIN_RATE, context);
        while (engine.getOutputRate() < CABLE_BUDGET_FOR_TIER_TEST) {
            engine.cycleOutputLevel();
        }

        // Totalled over a window rather than checked tick by tick. A single tick's delivery can be
        // split either side of the sink's own block-entity tick -- the engine pushes, the sink rolls
        // its counter, a battery pushes the rest -- so a per-tick reading understates a sink that was
        // in fact fully served. The total over the window is not affected.
        int firstSample = 20;
        int lastSample = 40;
        java.util.concurrent.atomic.AtomicLong received = new java.util.concurrent.atomic.AtomicLong();
        for (int tick = firstSample; tick <= lastSample; tick++) {
            context.runAfterDelay(tick, () -> received.addAndGet(sink.energyReceivedLastTick()));
        }

        long samples = lastSample - firstSample + 1;
        long expected = CONSUMER_DRAIN_RATE * samples;
        context.runAfterDelay(41, () -> {
            long batteryEnergy = battery.energyStorage(null).getAmount();
            if (received.get() * 10 < expected * 9) {
                context.fail("A battery outbid the consumer for the cable's budget: the consumer took "
                        + received.get() + " RF over " + samples + " ticks against a demand of " + expected
                        + ", while the battery banked " + batteryEnergy);
                return;
            }
            // Control: the battery must still charge on what is left over, or this would pass just
            // as well if buffers had stopped receiving power at all.
            if (batteryEnergy <= 0) {
                context.fail("The battery received nothing; buffers should still take the leftover");
                return;
            }
            context.succeed();
        });
    }
}
