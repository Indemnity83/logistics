package com.logistics.gametest.power;

import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Shared battery GameTest bodies, compiled directly into both loaders' {@code gametest} source
 * sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's own
 * registration mechanism: Fabric's {@code @GameTest}-annotated {@code BatteryGameTest} delegates
 * to these methods, and NeoForge's {@code BatteryGameTestRegistration} references them directly
 * as {@code Consumer<GameTestHelper>} method references.
 *
 * <p>Tests the charge-state rendering signal. A battery no longer powers the logistics pipe
 * network directly — that bridge is now the Power Junction (see
 * {@code com.logistics.gametest.pipe.PowerJunctionGameTest}).
 */
public class BatteryGameTestBody {

    private static void setStored(BatteryBlockEntity battery, long amount) {
        ((EnergyComponent) battery.energyStorage(null)).setAmount(amount);
    }

    /** A placed battery has its block entity. */
    public static void testBatteryPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.BATTERY);

        if (!(context.getBlockEntity(pos) instanceof BatteryBlockEntity)) {
            context.fail("Battery should have a block entity");
            return;
        }
        context.succeed();
    }

    /** The CHARGE block state property tracks stored energy (drives the multipart fill bar). */
    public static void testBatteryChargeStateTracksEnergy(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = (BatteryBlockEntity) context.getBlockEntity(pos);
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
    public static void testNetworkWithoutBatteryIsUnpowered(GameTestHelper context) {
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
    public static void testChargedBatteryDoesNotPowerNetwork(GameTestHelper context) {
        BlockPos pipePos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        BatteryBlockEntity battery = (BatteryBlockEntity) context.getBlockEntity(batteryPos);
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
