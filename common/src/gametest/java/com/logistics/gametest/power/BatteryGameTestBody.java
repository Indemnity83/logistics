package com.logistics.gametest.power;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.automation.kiln.KilnBlockEntity;
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

    /** Stored energy in a battery, as the network sees it. */
    private static long stored(BatteryBlockEntity battery) {
        return battery.energyStorage(null).getAmount();
    }

    /**
     * Two batteries sitting against each other must leave each other alone. A battery pushes into
     * whatever adjacent block will take energy, and another battery will -- so a pair shoves the
     * same energy back and forth, and which one ends up holding it depends on which happened to be
     * placed first. Nothing is generated or consumed by the shuffle; it only moves a player's
     * charge somewhere they did not put it.
     */
    public static void testTouchingBatteriesDoNotDrainEachOther(GameTestHelper context) {
        BlockPos leftPos = new BlockPos(1, 1, 1);
        BlockPos rightPos = new BlockPos(2, 1, 1);
        BlockPos machinePos = new BlockPos(1, 1, 2);
        // The empty one is placed first so it ticks first. Otherwise the charged battery pushes and
        // the empty one hands it straight back inside the same tick, and every between-tick sample
        // reads zero while the churn is happening.
        context.setBlock(rightPos, LogisticsPower.BLOCK.BATTERY);
        context.setBlock(leftPos, LogisticsPower.BLOCK.BATTERY);
        // Control: a real consumer on another face. Without it this test would pass just as well
        // if the battery's push had stopped working altogether.
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.KILN);

        BatteryBlockEntity left = (BatteryBlockEntity) context.getBlockEntity(leftPos);
        BatteryBlockEntity right = (BatteryBlockEntity) context.getBlockEntity(rightPos);
        if (left == null || right == null) {
            context.fail("Both batteries should have block entities");
            return;
        }

        long charge = BatteryBlockEntity.capacity() / 2;
        setStored(left, charge);
        setStored(right, 0);

        // Sampled every tick, not just at the end: the two push into each other, so a stack that
        // crosses and comes straight back reads as untouched by the time the dust settles.
        java.util.concurrent.atomic.AtomicLong peak = new java.util.concurrent.atomic.AtomicLong();
        for (int tick = 1; tick <= 20; tick++) {
            context.runAfterDelay(tick, () -> peak.accumulateAndGet(stored(right), Math::max));
        }

        context.runAfterDelay(21, () -> {
            if (!(context.getBlockEntity(machinePos) instanceof KilnBlockEntity machine)
                    || machine.energyStorage(null).getAmount() <= 0) {
                context.fail("The battery pushed nothing into the machine beside it, so this test "
                        + "proves nothing about what it does to another battery");
                return;
            }
            if (peak.get() != 0) {
                context.fail("The charged battery pushed " + peak.get()
                        + " RF into the empty one beside it");
                return;
            }
            context.succeed();
        });
    }

    /**
     * Batteries wired to the same cable must not drain one another either. Every battery on a
     * network is offered as a source as well as a target, so the one the network happens to sort
     * first empties into the rest -- which reads in game as a battery that sits there refusing to
     * charge, and as a charging order that depends on where the blocks are rather than on anything
     * the player did.
     */
    public static void testBatteriesOnOneCableDoNotDrainEachOther(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 0);
        BlockPos leftPos = new BlockPos(0, 1, 0);
        BlockPos rightPos = new BlockPos(2, 1, 0);
        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        // The empty one is placed first so it ticks first. Otherwise the charged battery pushes and
        // the empty one hands it straight back inside the same tick, and every between-tick sample
        // reads zero while the churn is happening.
        context.setBlock(rightPos, LogisticsPower.BLOCK.BATTERY);
        context.setBlock(leftPos, LogisticsPower.BLOCK.BATTERY);

        BatteryBlockEntity left = (BatteryBlockEntity) context.getBlockEntity(leftPos);
        BatteryBlockEntity right = (BatteryBlockEntity) context.getBlockEntity(rightPos);
        if (left == null || right == null) {
            context.fail("Both batteries should have block entities");
            return;
        }

        long charge = BatteryBlockEntity.capacity() / 2;
        setStored(left, charge);
        setStored(right, 0);

        java.util.concurrent.atomic.AtomicLong peakRight = new java.util.concurrent.atomic.AtomicLong();
        for (int tick = 1; tick <= 40; tick++) {
            context.runAfterDelay(tick, () -> peakRight.accumulateAndGet(stored(right), Math::max));
        }

        context.runAfterDelay(41, () -> {
            if (peakRight.get() != 0) {
                context.fail("A battery pushed " + peakRight.get()
                        + " RF through the cable into another battery");
                return;
            }
            if (stored(right) != 0) {
                context.fail("A battery drained " + stored(right) + " RF through the cable into another battery");
                return;
            }
            if (stored(left) != charge) {
                context.fail("The charged battery lost energy across the cable: "
                        + stored(left) + " of " + charge);
                return;
            }
            context.succeed();
        });
    }
}
