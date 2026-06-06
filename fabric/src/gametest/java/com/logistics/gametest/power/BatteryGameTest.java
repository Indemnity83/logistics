package com.logistics.gametest.power;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Game tests for the Battery block: charge-state rendering signal and powering adjacent
 * logistics pipe networks (the "power use in pipes" feature).
 */
public class BatteryGameTest {

    private static void setStored(BatteryBlockEntity battery, long amount) {
        ((EnergyComponent) battery.energyStorage(null)).setAmount(amount);
    }

    /** A placed battery has its block entity. */
    @GameTest
    public void testBatteryPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.BATTERY);

        if (context.getBlockEntity(pos, BatteryBlockEntity.class) == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        context.succeed();
    }

    /** The CHARGE block state property tracks stored energy (drives the multipart fill bar). */
    @GameTest(maxTicks = 30)
    public void testBatteryChargeStateTracksEnergy(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(pos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, BatteryBlockEntity.CAPACITY);

        context.runAfterDelay(5, () -> {
            int charge = context.getBlockState(pos).getValue(AbstractBatteryBlockEntity.CHARGE);
            if (charge != 10) {
                context.fail("Full battery should report charge level 10, got " + charge);
                return;
            }
            context.succeed();
        });
    }

    /** A charged battery adjacent to a pipe powers that pipe's network. */
    @GameTest(maxTicks = 40)
    public void testChargedBatteryPowersNetwork(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, BatteryBlockEntity.CAPACITY);

        context.runAfterDelay(25, () -> {
            PipeNetwork net = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pipePos));
            if (net == null) {
                context.fail("Pipe should have formed a network");
                return;
            }
            long before = battery.getEnergyStored();
            if (!net.consumeEnergy(500)) {
                context.fail("A network with a charged battery should supply energy");
                return;
            }
            long drawn = before - battery.getEnergyStored();
            if (drawn != 500) {
                context.fail("Expected 500 RF drawn from the battery, drew " + drawn);
                return;
            }
            context.succeed();
        });
    }

    /** An adjacent pipe forms a POWER connection (rendered arm) toward the battery, not a route. */
    @GameTest(maxTicks = 40)
    public void testPipeFormsPowerConnectionToBattery(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0); // EAST of the pipe
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);

        context.runAfterDelay(10, () -> {
            PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
            if (pipe == null) {
                context.fail("Pipe should have a block entity");
                return;
            }
            PipeConnection.Type type = pipe.getCachedConnectionType(Direction.EAST);
            if (type != PipeConnection.Type.POWER) {
                context.fail("Pipe should form a POWER connection toward the battery, got " + type);
                return;
            }
            context.succeed();
        });
    }

    /** A network with no battery cannot supply energy (the hard power gate). */
    @GameTest(maxTicks = 40)
    public void testNetworkWithoutBatteryIsUnpowered(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);

        context.runAfterDelay(25, () -> {
            PipeNetwork net = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pipePos));
            if (net == null) {
                context.fail("Pipe should have formed a network");
                return;
            }
            if (net.consumeEnergy(1)) {
                context.fail("A network with no battery should not supply energy");
                return;
            }
            context.succeed();
        });
    }

    /** A drained battery on the network supplies nothing (all-or-nothing). */
    @GameTest(maxTicks = 40)
    public void testEmptyBatteryDoesNotPowerNetwork(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, 0);

        context.runAfterDelay(25, () -> {
            PipeNetwork net = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pipePos));
            if (net == null) {
                context.fail("Pipe should have formed a network");
                return;
            }
            if (net.consumeEnergy(1)) {
                context.fail("An empty battery should not be able to power the network");
                return;
            }
            context.succeed();
        });
    }

    private static int bit(Direction dir) {
        return 1 << dir.get3DDataValue();
    }

    /** A charged battery makes a logistics pipe's battery-facing and pipe-facing arms "powered". */
    @GameTest(maxTicks = 40)
    public void testLogisticsArmsPoweredWhenBatteryCharged(GameTestHelper context) {
        BlockPos pipeA = new BlockPos(0, 1, 0);
        BlockPos pipeB = new BlockPos(1, 1, 0); // EAST: another logistics pipe
        BlockPos batteryPos = new BlockPos(0, 2, 0); // UP: battery
        context.setBlock(pipeA, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(pipeB, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, BatteryBlockEntity.CAPACITY);

        context.runAfterDelay(25, () -> {
            PipeBlockEntity pipe = context.getBlockEntity(pipeA, PipeBlockEntity.class);
            if (pipe == null) {
                context.fail("Pipe should have a block entity");
                return;
            }
            int mask = pipe.getPoweredArmMask();
            if ((mask & bit(Direction.UP)) == 0) {
                context.fail("Arm toward the battery should be powered (green)");
                return;
            }
            if ((mask & bit(Direction.EAST)) == 0) {
                context.fail("Arm toward the adjacent logistics pipe should be powered (green)");
                return;
            }
            context.succeed();
        });
    }

    /** Power flows network-wide: a logistics arm into a transport-pipe bridge is green when powered. */
    @GameTest(maxTicks = 40)
    public void testPowerFlowsThroughTransportBridge(GameTestHelper context) {
        BlockPos pipeA = new BlockPos(0, 1, 0);
        BlockPos copper = new BlockPos(1, 1, 0);   // transport pipe bridging the two logistics pipes
        BlockPos pipeB = new BlockPos(2, 1, 0);
        BlockPos batteryPos = new BlockPos(2, 2, 0);
        context.setBlock(pipeA, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(copper, LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        context.setBlock(pipeB, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, BatteryBlockEntity.CAPACITY);

        context.runAfterDelay(25, () -> {
            PipeBlockEntity pipe = context.getBlockEntity(pipeA, PipeBlockEntity.class);
            if (pipe == null) {
                context.fail("Pipe should have a block entity");
                return;
            }
            if ((pipe.getPoweredArmMask() & bit(Direction.EAST)) == 0) {
                context.fail("Logistics arm into the transport-pipe bridge should be powered (green)");
                return;
            }
            context.succeed();
        });
    }

    /** A machine that speaks the PIPE connection only for item I/O (the quarry) is not a power link. */
    @GameTest(maxTicks = 40)
    public void testQuarryArmIsNotTreatedAsPower(GameTestHelper context) {
        BlockPos quarryPos = new BlockPos(0, 1, 0);
        BlockPos pipePos = new BlockPos(0, 2, 0);   // above the quarry
        BlockPos batteryPos = new BlockPos(1, 2, 0); // EAST: powers the network
        context.setBlock(quarryPos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, BatteryBlockEntity.CAPACITY);

        context.runAfterDelay(25, () -> {
            PipeBlockEntity pipe = context.getBlockEntity(pipePos, PipeBlockEntity.class);
            if (pipe == null) {
                context.fail("Pipe should have a block entity");
                return;
            }
            int mask = pipe.getPoweredArmMask();
            if ((mask & bit(Direction.EAST)) == 0) {
                context.fail("Battery-facing arm should be powered (confirms the network has power)");
                return;
            }
            if ((mask & bit(Direction.DOWN)) != 0) {
                context.fail("Arm into the quarry should not be a power link (it's an item endpoint)");
                return;
            }
            context.succeed();
        });
    }

    /** With a drained battery the logistics pipe's arms report unpowered (all red). */
    @GameTest(maxTicks = 40)
    public void testLogisticsArmsUnpoweredWhenBatteryEmpty(GameTestHelper context) {
        BlockPos pipeA = new BlockPos(0, 1, 0);
        BlockPos pipeB = new BlockPos(1, 1, 0);
        BlockPos batteryPos = new BlockPos(0, 2, 0);
        context.setBlock(pipeA, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(pipeB, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, 0);

        context.runAfterDelay(25, () -> {
            PipeBlockEntity pipe = context.getBlockEntity(pipeA, PipeBlockEntity.class);
            if (pipe == null) {
                context.fail("Pipe should have a block entity");
                return;
            }
            if (pipe.getPoweredArmMask() != 0) {
                context.fail("An unpowered network should leave all arms unpowered, mask="
                        + pipe.getPoweredArmMask());
                return;
            }
            context.succeed();
        });
    }
}
