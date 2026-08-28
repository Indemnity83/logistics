package com.logistics.gametest.pipe;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Shared power junction GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's
 * own registration mechanism: Fabric's {@code @GameTest}-annotated {@code PowerJunctionGameTest}
 * delegates to these methods, and NeoForge's {@code PowerJunctionGameTestRegistration} references
 * them directly as {@code Consumer<GameTestHelper>} method references.
 *
 * <p>Tests for the Logistics Power Junction: the block that bridges RF into the logistics pipe
 * network (replacing the old battery-powers-the-network bridge).
 */
public class PowerJunctionGameTestBody {

    /** Fill a junction's RF buffer to capacity via its rate-capped insert loop. */
    private static void fill(GameTestHelper context, PowerJunctionBlockEntity junction) {
        IEnergyStorage es = junction.energyStorage(null);
        long remaining = PowerJunctionBlockEntity.CAPACITY;
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted <= 0) {
                context.fail("Failed to fill junction: insert returned " + inserted
                        + " with " + remaining + " RF remaining");
                return;
            }
            remaining -= inserted;
        }
    }

    private static int bit(Direction dir) {
        return 1 << dir.get3DDataValue();
    }

    /** A filled junction adjacent to a pipe powers that pipe's network and is drawn down. */
    public static void testJunctionPowersNetwork(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 1, 0);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(junctionPos, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);
        if (junction == null) {
            context.fail("Junction should have a block entity");
            return;
        }
        fill(context, junction);

        context.runAfterDelay(25, () -> {
            PipeNetwork net = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pipePos));
            if (net == null) {
                context.fail("Pipe should have formed a network");
                return;
            }
            long before = junction.energyStorage(null).getAmount();
            if (!net.consumeEnergy(500)) {
                context.fail("A network with a filled junction should supply energy");
                return;
            }
            long drawn = before - junction.energyStorage(null).getAmount();
            if (drawn != 500) {
                context.fail("Expected 500 RF drawn from the junction, drew " + drawn);
                return;
            }
            context.succeed();
        });
    }

    /** An unfilled junction supplies nothing (the hard power gate). */
    public static void testEmptyJunctionDoesNotPowerNetwork(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 1, 0);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(junctionPos, LogisticsPipe.BLOCK.POWER_JUNCTION);

        context.runAfterDelay(25, () -> {
            PipeNetwork net = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pipePos));
            if (net == null) {
                context.fail("Pipe should have formed a network");
                return;
            }
            if (net.consumeEnergy(1)) {
                context.fail("An empty junction should not be able to power the network");
                return;
            }
            context.succeed();
        });
    }

    /** An adjacent pipe forms a POWER connection (rendered arm) toward the junction, not a route. */
    public static void testPipeFormsPowerConnectionToJunction(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos junctionPos = new BlockPos(1, 1, 0); // EAST of the pipe
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(junctionPos, LogisticsPipe.BLOCK.POWER_JUNCTION);

        context.runAfterDelay(10, () -> {
            PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
            if (pipe == null) {
                context.fail("Pipe should have a block entity");
                return;
            }
            PipeConnection.Type type = pipe.getCachedConnectionType(Direction.EAST);
            if (type != PipeConnection.Type.POWER) {
                context.fail("Pipe should form a POWER connection toward the junction, got " + type);
                return;
            }
            context.succeed();
        });
    }

    /** A filled junction makes a logistics pipe's junction-facing and pipe-facing arms "powered". */
    public static void testLogisticsArmsPoweredWhenJunctionCharged(GameTestHelper context) {
        BlockPos pipeA = new BlockPos(0, 1, 0);
        BlockPos pipeB = new BlockPos(1, 1, 0); // EAST: another logistics pipe
        BlockPos junctionPos = new BlockPos(0, 2, 0); // UP: junction
        context.setBlock(pipeA, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(pipeB, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(junctionPos, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);
        if (junction == null) {
            context.fail("Junction should have a block entity");
            return;
        }
        fill(context, junction);

        context.runAfterDelay(25, () -> {
            PipeBlockEntity pipe = context.getBlockEntity(pipeA, PipeBlockEntity.class);
            if (pipe == null) {
                context.fail("Pipe should have a block entity");
                return;
            }
            int mask = pipe.getPoweredArmMask();
            if ((mask & bit(Direction.UP)) == 0) {
                context.fail("Arm toward the junction should be powered (green)");
                return;
            }
            if ((mask & bit(Direction.EAST)) == 0) {
                context.fail("Arm toward the adjacent logistics pipe should be powered (green)");
                return;
            }
            context.succeed();
        });
    }

    /** A machine that speaks the PIPE connection only for item I/O (the quarry) is not a power link. */
    public static void testQuarryArmIsNotTreatedAsPower(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(0, 1, 0);
        BlockPos pipePos = new BlockPos(0, 2, 0);     // above the quarry
        BlockPos junctionPos = new BlockPos(1, 2, 0); // EAST: powers the network
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(junctionPos, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = context.getBlockEntity(junctionPos, PowerJunctionBlockEntity.class);
        if (junction == null) {
            context.fail("Junction should have a block entity");
            return;
        }
        fill(context, junction);

        context.runAfterDelay(25, () -> {
            PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
            if (pipe == null) {
                context.fail("Pipe should have a block entity");
                return;
            }
            // The quarry must actually be connected (as PIPE for item I/O) — otherwise the
            // "not a power link" check below would pass simply because nothing is connected.
            PipeConnection.Type quarryConnection = pipe.getCachedConnectionType(Direction.DOWN);
            if (quarryConnection != PipeConnection.Type.PIPE) {
                context.fail("Pipe should connect to the quarry as PIPE for item I/O, got " + quarryConnection);
                return;
            }
            int mask = pipe.getPoweredArmMask();
            if ((mask & bit(Direction.EAST)) == 0) {
                context.fail("Junction-facing arm should be powered (confirms the network has power)");
                return;
            }
            if ((mask & bit(Direction.DOWN)) != 0) {
                context.fail("Arm into the quarry should not be a power link (it's an item endpoint)");
                return;
            }
            context.succeed();
        });
    }
}
