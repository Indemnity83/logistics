package com.logistics.gametest.power;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.automation.kiln.KilnBlockEntity;
import com.logistics.power.block.BatteryTier;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.block.entity.CreativeSinkBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;

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

    /** Second-phase levelling charges: partly charged either side, so both are free to push. */
    private static final long HIGHER_SHARE = 30_000L;
    private static final long LOWER_SHARE = 10_000L;

    private static void setStored(BatteryBlockEntity battery, long amount) {
        ((EnergyComponent) battery.energyStorage(null)).setAmount(amount);
    }

    /** A placed battery has its block entity. */
    public static void testBatteryPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.COPPER_BATTERY);

        if (context.getBlockEntity(pos, BatteryBlockEntity.class) == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        context.succeed();
    }

    /** The CHARGE block state property tracks stored energy (drives the multipart fill bar). */
    public static void testBatteryChargeStateTracksEnergy(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.COPPER_BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(pos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, BatteryTier.COPPER.capacity());

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
        context.setBlock(batteryPos, LogisticsPower.BLOCK.COPPER_BATTERY);
        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Battery should have a block entity");
            return;
        }
        setStored(battery, BatteryTier.COPPER.capacity());

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

    private static Block blockFor(BatteryTier tier) {
        return switch (tier) {
            case TIN -> LogisticsPower.BLOCK.TIN_BATTERY;
            case COPPER -> LogisticsPower.BLOCK.COPPER_BATTERY;
            case GOLD -> LogisticsPower.BLOCK.GOLD_BATTERY;
            case AMETHYST -> LogisticsPower.BLOCK.AMETHYST_BATTERY;
            case ECHO -> LogisticsPower.BLOCK.ECHO_BATTERY;
        };
    }

    /**
     * All five tiers share one {@code BlockEntityType}, so a placed battery has to take its
     * capacity from the block it belongs to. If that lookup regressed to a single constant, every
     * tier would silently report the same ceiling.
     */
    public static void testEachTierReportsItsOwnCapacity(GameTestHelper context) {
        // One position, reused: the empty test structure is only a few blocks across, and nothing
        // here ticks, so replacing the block in place is both safe and free of neighbour effects.
        BlockPos pos = new BlockPos(1, 1, 1);
        for (BatteryTier tier : BatteryTier.values()) {
            context.setBlock(pos, blockFor(tier));

            BatteryBlockEntity battery = context.getBlockEntity(pos, BatteryBlockEntity.class);
            if (battery == null) {
                context.fail(tier + " battery should have a block entity");
                return;
            }
            long actual = battery.energyStorage(null).getCapacity();
            if (actual != tier.capacity()) {
                context.fail(tier + " battery should hold " + tier.capacity() + " RF, got " + actual);
                return;
            }
        }
        context.succeed();
    }

    /**
     * The tier has to govern the runtime ceiling, not just the reported number. A Gold battery
     * pre-charged to what a Copper one holds is not full, so a real engine feeding it through a
     * real cable must keep filling it past that mark -- which it cannot do if the ceiling
     * regressed to the base tier's.
     *
     * <p>Pre-charging is what makes this affordable: copper cable moves 30 RF/t, so filling
     * 1,600,000 RF from empty would take tens of thousands of ticks. Starting at Copper's ceiling
     * puts the only interesting boundary a few ticks away.
     */
    public static void testHigherTierChargesPastTheLowerTierCeiling(GameTestHelper context) {
        BlockPos batteryPos = new BlockPos(1, 1, 0);
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos enginePos = new BlockPos(1, 1, 2);

        context.setBlock(batteryPos, LogisticsPower.BLOCK.GOLD_BATTERY);
        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        // Engine placed last, facing the cable: setBlock fires neighbour updates that recompute
        // POWERED, and an engine only exposes its buffer on the output face.
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.NORTH)
                .setValue(AbstractEngineBlock.POWERED, true));

        BatteryBlockEntity battery = context.getBlockEntity(batteryPos, BatteryBlockEntity.class);
        if (battery == null) {
            context.fail("Gold battery should have a block entity");
            return;
        }
        long copperCeiling = BatteryTier.COPPER.capacity();
        setStored(battery, copperCeiling);

        context.runAfterDelay(40, () -> {
            long now = stored(battery);
            if (now <= copperCeiling) {
                context.fail("A Gold battery held at Copper's " + copperCeiling
                        + " RF ceiling should keep charging, got " + now);
                return;
            }
            if (now > BatteryTier.GOLD.capacity()) {
                context.fail("A Gold battery should never exceed " + BatteryTier.GOLD.capacity()
                        + " RF, got " + now);
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
        context.setBlock(rightPos, LogisticsPower.BLOCK.COPPER_BATTERY);
        context.setBlock(leftPos, LogisticsPower.BLOCK.COPPER_BATTERY);
        // Control: a real consumer on another face. Without it this test would pass just as well
        // if the battery's push had stopped working altogether.
        context.setBlock(machinePos, LogisticsAutomation.BLOCK.KILN);

        BatteryBlockEntity left = context.getBlockEntity(leftPos, BatteryBlockEntity.class);
        BatteryBlockEntity right = context.getBlockEntity(rightPos, BatteryBlockEntity.class);
        if (left == null || right == null) {
            context.fail("Both batteries should have block entities");
            return;
        }

        long charge = BatteryTier.COPPER.capacity() / 2;
        setStored(left, charge);
        setStored(right, 0);

        // Sampled every tick, not just at the end: the two push into each other, so a stack that
        // crosses and comes straight back reads as untouched by the time the dust settles.
        java.util.concurrent.atomic.AtomicLong peak = new java.util.concurrent.atomic.AtomicLong();
        for (int tick = 1; tick <= 20; tick++) {
            context.runAfterDelay(tick, () -> peak.accumulateAndGet(stored(right), Math::max));
        }

        context.runAfterDelay(21, () -> {
            KilnBlockEntity machine = context.getBlockEntity(machinePos, KilnBlockEntity.class);
            if (machine == null || machine.energyStorage(null).getAmount() <= 0) {
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
     * A bank of batteries on one cable charges together, not one at a time. The allocator splits the
     * budget pro rata by each target's remaining room, so three equally empty batteries take an equal
     * share and stay level as they fill -- what a player expects a battery bank to do.
     *
     * <p>Two rig details are load-bearing. The engine is placed <em>last</em>: {@code setBlock} fires
     * neighbour updates, and {@link AbstractEngineBlock} recomputes {@code POWERED} from real
     * redstone, so a block placed after a synthetically powered engine switches it straight back off.
     * And {@code FACING} must aim at the cable, because an engine exposes its buffer on the output
     * face alone and the network never pulls from an engine -- it has to push.
     */
    public static void testBatteriesOnOneCableFillEvenly(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos northPos = new BlockPos(1, 1, 0);
        BlockPos southPos = new BlockPos(1, 1, 2);
        BlockPos upPos = new BlockPos(1, 2, 1);
        BlockPos enginePos = new BlockPos(0, 1, 1);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(northPos, LogisticsPower.BLOCK.COPPER_BATTERY);
        context.setBlock(southPos, LogisticsPower.BLOCK.COPPER_BATTERY);
        context.setBlock(upPos, LogisticsPower.BLOCK.COPPER_BATTERY);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));

        BatteryBlockEntity north = context.getBlockEntity(northPos, BatteryBlockEntity.class);
        BatteryBlockEntity south = context.getBlockEntity(southPos, BatteryBlockEntity.class);
        BatteryBlockEntity up = context.getBlockEntity(upPos, BatteryBlockEntity.class);
        CreativeEngineBlockEntity engine = context.getBlockEntity(enginePos, CreativeEngineBlockEntity.class);
        if (north == null || south == null || up == null || engine == null) {
            context.fail("Expected three batteries and an engine");
            return;
        }

        context.runAfterDelay(40, () -> {
            long lowest = Math.min(stored(north), Math.min(stored(south), stored(up)));
            long highest = Math.max(stored(north), Math.max(stored(south), stored(up)));
            String charges = stored(north) + "/" + stored(south) + "/" + stored(up);

            if (lowest <= 0) {
                context.fail("Every battery on the cable should be charging, got " + charges);
                return;
            }
            // One tick's whole network input: no battery may run more than a single tick ahead of
            // another. Sequential filling leaves one at the engine's total output and the rest near zero.
            long tolerance = engine.getOutputRate();
            if (highest - lowest > tolerance) {
                context.fail("Batteries on one cable should fill together within " + tolerance
                        + " RF of each other, got " + charges);
                return;
            }
            context.succeed();
        });
    }

    /**
     * A bank of batteries on one cable empties together, not one at a time. Every battery on a
     * network is offered to it as a source, and the draw for a consumer is shared across them pro
     * rata, so the bank falls level instead of the front one carrying the whole load.
     *
     * <p>What used to decide this was the block-entity tick order. A battery pushed onto the cable
     * itself, spending whatever of the per-tick budget was still going, so whichever battery ticked
     * first supplied everything and the rest of the bank sat untouched -- an order the player never
     * chose. A battery now leaves a cable neighbour to the network.
     */
    public static void testBatteriesOnOneCableDrainEvenly(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 1);
        BlockPos sinkPos = new BlockPos(1, 1, 2);
        BlockPos firstPos = new BlockPos(1, 2, 1);
        BlockPos secondPos = new BlockPos(1, 1, 0);

        context.setBlock(cablePos, LogisticsPower.BLOCK.ENDER_CABLE);
        context.setBlock(sinkPos, LogisticsPower.BLOCK.CREATIVE_SINK);
        context.setBlock(firstPos, LogisticsPower.BLOCK.COPPER_BATTERY);
        context.setBlock(secondPos, LogisticsPower.BLOCK.COPPER_BATTERY);

        CreativeSinkBlockEntity sink = context.getBlockEntity(sinkPos, CreativeSinkBlockEntity.class);
        BatteryBlockEntity first = context.getBlockEntity(firstPos, BatteryBlockEntity.class);
        BatteryBlockEntity second = context.getBlockEntity(secondPos, BatteryBlockEntity.class);
        if (sink == null || first == null || second == null) {
            context.fail("Expected two batteries and a sink");
            return;
        }
        sink.setUnlimitedDrainRate();

        long charge = BatteryTier.COPPER.capacity() / 2;
        setStored(first, charge);
        setStored(second, charge);

        context.runAfterDelay(40, () -> {
            long firstGave = charge - stored(first);
            long secondGave = charge - stored(second);

            if (firstGave <= 0 || secondGave <= 0) {
                context.fail("Both batteries should supply the consumer, gave " + firstGave
                        + " and " + secondGave + " RF");
                return;
            }
            // Neither may carry more than about two thirds of the load; one battery draining while
            // the other sits full is the behaviour this replaces.
            long total = firstGave + secondGave;
            if (Math.max(firstGave, secondGave) * 3 > total * 2) {
                context.fail("A battery bank should empty together, but one carried the load: "
                        + firstGave + " RF against " + secondGave + " RF");
                return;
            }
            context.succeed();
        });
    }

    /**
     * Batteries wired together level out. A bank is one buffer as far as a player is concerned, so a
     * charged battery and an empty one on the same cable should meet in the middle rather than
     * staying as they were.
     *
     * <p>This inverts {@code batteries_on_one_cable_do_not_drain_each_other}, shipped in 0.8.9. That
     * fix refused buffer-to-buffer transfer outright, which was the right same-day answer to one
     * battery silently emptying into the rest -- but it also froze a bank into whatever state it
     * happened to be in. The transfer is now bounded by the amount that leaves both at the same fill
     * instead of being refused, so charge runs downhill and stops on level. Which battery was placed
     * first no longer enters into it.
     *
     * <p>Direct face-to-face transfer is still refused: see
     * {@link #testTouchingBatteriesDoNotDrainEachOther}. A cable is required.
     */
    public static void testBatteriesOnOneCableLevelOut(GameTestHelper context) {
        BlockPos cablePos = new BlockPos(1, 1, 0);
        BlockPos leftPos = new BlockPos(0, 1, 0);
        BlockPos rightPos = new BlockPos(2, 1, 0);
        context.setBlock(cablePos, LogisticsPower.BLOCK.ENDER_CABLE);
        context.setBlock(rightPos, LogisticsPower.BLOCK.COPPER_BATTERY);
        context.setBlock(leftPos, LogisticsPower.BLOCK.COPPER_BATTERY);

        BatteryBlockEntity left = context.getBlockEntity(leftPos, BatteryBlockEntity.class);
        BatteryBlockEntity right = context.getBlockEntity(rightPos, BatteryBlockEntity.class);
        if (left == null || right == null) {
            context.fail("Both batteries should have block entities");
            return;
        }

        long charge = BatteryTier.COPPER.capacity() / 2;
        setStored(left, charge);
        setStored(right, 0);

        context.runAfterDelay(41, () -> {
            long total = stored(left) + stored(right);
            if (total != charge) {
                context.fail("Levelling must conserve charge: " + stored(left) + " + " + stored(right)
                        + " is not the " + charge + " RF the bank started with");
                return;
            }
            if (stored(right) <= 0) {
                context.fail("A charged battery should level into an empty one across a cable, "
                        + "but the empty one is still at " + stored(right) + " RF");
                return;
            }
            // Downhill only: the charged one must never fall below the one it is filling, which is
            // what an overshooting or oscillating transfer would look like.
            if (stored(left) < stored(right)) {
                context.fail("Levelling overshot: the source fell to " + stored(left)
                        + " RF below the target's " + stored(right) + " RF");
                return;
            }
            // Second phase: two partly charged batteries must close the gap between them. This is
            // the case that tells a bounded transfer from an unbounded one. Unbounded, each is as
            // free to push as the other, the two flows cancel, and the pair sits at its original
            // split forever -- which reads exactly like the frozen bank the 0.8.9 guard produced.
            setStored(left, HIGHER_SHARE);
            setStored(right, LOWER_SHARE);
        });

        context.runAfterDelay(80, () -> {
            long gap = stored(left) - stored(right);
            long startingGap = HIGHER_SHARE - LOWER_SHARE;

            if (gap < 0) {
                context.fail("Levelling overshot in the second phase: " + stored(left)
                        + " RF now below " + stored(right) + " RF");
                return;
            }
            if (gap * 10 > startingGap * 9) {
                context.fail("Two partly charged batteries should close the gap between them, but it "
                        + "only moved from " + startingGap + " to " + gap + " RF");
                return;
            }
            context.succeed();
        });
    }
}
