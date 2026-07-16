package com.logistics.gametest.power;

import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Game tests for the Battery block: charge-state rendering signal. A battery no longer powers
 * the logistics pipe network directly — that bridge is now the Power Junction (see
 * {@code com.logistics.gametest.pipe.PowerJunctionGameTest}).
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
        setStored(battery, BatteryBlockEntity.capacity());

        context.runAfterDelay(5, () -> {
            int charge = context.getBlockState(pos).getValue(AbstractBatteryBlockEntity.CHARGE);
            if (charge != 10) {
                context.fail("Full battery should report charge level 10, got " + charge);
                return;
            }
            context.succeed();
        });
    }

    /** A network with no power source cannot supply energy (the hard power gate). */
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
                context.fail("A network with no power source should not supply energy");
                return;
            }
            context.succeed();
        });
    }

    /** A charged battery alone no longer powers the network — only a Power Junction bridges RF in. */
    @GameTest(maxTicks = 40)
    public void testChargedBatteryDoesNotPowerNetwork(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, BatteryBlockEntity.capacity());

        context.runAfterDelay(25, () -> {
            PipeNetwork net = NetworkRegistry.getNetwork(context.getLevel(), context.absolutePos(pipePos));
            if (net == null) {
                context.fail("Pipe should have formed a network");
                return;
            }
            if (net.consumeEnergy(1)) {
                context.fail("A battery alone should no longer power the network");
                return;
            }
            context.succeed();
        });
    }
}
