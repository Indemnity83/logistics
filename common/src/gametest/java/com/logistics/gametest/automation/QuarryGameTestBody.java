package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.LogisticsPower;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.laserquarry.entity.QuarryPhase;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.power.cable.CableBlock;
import com.logistics.power.cable.CableBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Shared laser quarry GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each
 * loader's own registration mechanism: Fabric's {@code @GameTest}-annotated
 * {@code QuarryGameTest} delegates to these methods, and NeoForge's
 * {@code QuarryGameTestRegistration} references them directly as {@code Consumer<GameTestHelper>}
 * method references.
 */
public class QuarryGameTestBody {

    /**
     * Test that laser quarry can be placed and creates block entity.
     */
    public static void testQuarryPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        // Place quarry
        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        // Verify block entity exists
        LaserQuarryBlockEntity blockEntity = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);
        if (blockEntity == null) {
            context.fail("Laser quarry should create LaserQuarryBlockEntity");
            return;
        }

        context.succeed();
    }

    /**
     * A player can wire the quarry from any face.
     *
     * <p>Walks the six faces one at a time, building the rig a player builds — a creative engine
     * pushing into a copper cable that touches the quarry — and asserting energy actually lands in
     * the quarry's buffer. Asking the block entity for an {@code IEnergyStorage} per direction
     * (what this test used to do) never touches the cable network, and the network reaches a
     * machine through the loader's own {@code EnergyCapabilityLookup} ({@code CableNetwork:157}),
     * so a quarry missing from a loader's energy-capability registration answered the old probe
     * perfectly while no cable on any side could power it.
     *
     * <p>Each round tears the rig down again and asserts the input stops, so the next face's
     * reading is its own and not the previous face's leftover.
     */
    public static void testQuarryAcceptsEnergy(GameTestHelper context) {
        BlockPos pos = new BlockPos(3, 3, 3);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        // The rig reaches two blocks out on every axis, and a powered quarry clears and mines its
        // own bounds — aim those well clear so it cannot eat the cable under test.
        BlockPos absPos = context.absolutePos(pos);
        quarry.setCustomBounds(absPos.getX() + 6, absPos.getZ() + 6, absPos.getX() + 10, absPos.getZ() + 10);

        powerQuarryFromFace(context, pos, quarry, 0);
    }

    /** One face of {@link #testQuarryAcceptsEnergy}: wire it up, prove power flows, tear it down. */
    private static void powerQuarryFromFace(
            GameTestHelper context, BlockPos quarryPos, LaserQuarryBlockEntity quarry, int faceIndex) {
        if (faceIndex >= Direction.values().length) {
            context.succeed();
            return;
        }

        Direction face = Direction.values()[faceIndex];
        BlockPos cablePos = quarryPos.relative(face);
        BlockPos enginePos = cablePos.relative(face);

        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, face.getOpposite())
                .setValue(AbstractEngineBlock.POWERED, true));

        CableBlockEntity cable = context.getBlockEntity(cablePos, CableBlockEntity.class);
        if (cable == null) {
            context.fail("Expected a cable block entity on the quarry's " + face + " face");
            return;
        }

        CableBlock.ConnectionType connection = cable.getCachedConnectionType(face.getOpposite());
        if (connection != CableBlock.ConnectionType.DEVICE) {
            context.fail("A cable on the quarry's " + face + " face should connect to it as a powered device, got: "
                    + connection);
            return;
        }

        context.runAfterDelay(12, () -> {
            if (quarry.getEnergyReceivedLastTick() <= 0) {
                context.fail("Quarry should be receiving energy through a cable on its " + face + " face, got "
                        + quarry.getEnergyReceivedLastTick() + " RF/t");
                return;
            }

            context.setBlock(enginePos, Blocks.AIR);
            context.setBlock(cablePos, Blocks.AIR);

            context.runAfterDelay(4, () -> {
                if (quarry.getEnergyReceivedLastTick() != 0) {
                    context.fail("Quarry should stop receiving energy once the cable on its " + face
                            + " face is removed, still got " + quarry.getEnergyReceivedLastTick() + " RF/t");
                    return;
                }
                powerQuarryFromFace(context, quarryPos, quarry, faceIndex + 1);
            });
        });
    }

    public static void testQuarryTracksCommittedEnergyInput(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        long inserted = quarry.energyStorage(Direction.NORTH).insert(60, false);
        if (inserted != 60) {
            context.fail("Expected quarry to accept 60 RF, got " + inserted);
            return;
        }

        context.runAfterDelay(1, () -> {
            if (quarry.getEnergyReceivedLastTick() != 60) {
                context.fail("Expected quarry to report 60 RF/t input, got "
                        + quarry.getEnergyReceivedLastTick());
                return;
            }
            context.succeed();
        });
    }

    /**
     * The quarry only ever pushes items out; nothing a player can build pushes items in.
     *
     * <p>Five real hoppers, one per reachable face, each holding a diamond and aimed at the quarry.
     * A hopper below could only ever pull — no vanilla block inserts upward — so the quarry's DOWN
     * face has no player-reachable inserter and is covered by the contract sweep at the end.
     *
     * <p>Every diamond must still be in its hopper and none may be lying on the ground. Asking
     * {@code canAcceptFrom} directly (what this test used to do) cannot see the failure that costs
     * a player items: a loader adapter exposing the quarry as an item container would let hoppers
     * feed a machine with no way to get the items back out, while the block entity kept answering
     * "no" to a question nothing was asking.
     */
    public static void testQuarryDoesNotAcceptItems(GameTestHelper context) {
        BlockPos pos = new BlockPos(2, 2, 2);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        // Left unpowered on purpose: a quarry with no energy neither clears nor mines, so the
        // hoppers stay put for the whole run.
        List<BlockPos> hopperPositions = new ArrayList<>();
        for (Direction face : Direction.values()) {
            if (face == Direction.DOWN) {
                continue;
            }

            BlockPos hopperPos = pos.relative(face);
            context.setBlock(hopperPos, Blocks.HOPPER.defaultBlockState()
                    .setValue(HopperBlock.FACING, face.getOpposite()));

            HopperBlockEntity hopper = context.getBlockEntity(hopperPos, HopperBlockEntity.class);
            if (hopper == null) {
                context.fail("Expected a hopper block entity on the quarry's " + face + " face");
                return;
            }
            hopper.setItem(0, new ItemStack(Items.DIAMOND));
            hopperPositions.add(hopperPos);
        }

        // A hopper retries every 8 ticks; 40 gives each of them several attempts.
        context.runAfterDelay(40, () -> {
            for (BlockPos hopperPos : hopperPositions) {
                context.assertContainerContains(hopperPos, Items.DIAMOND);
            }

            List<ItemEntity> dropped = context.getLevel().getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(context.absolutePos(pos)).inflate(4.0),
                    entity -> entity.getItem().is(Items.DIAMOND));
            if (!dropped.isEmpty()) {
                context.fail("Items pushed at the quarry were dropped into the world instead of being left "
                        + "in the hopper, found " + dropped.size() + " item entit(ies)");
                return;
            }

            // Nothing in vanilla inserts upward, so the DOWN face is only reachable through the pipe
            // contract itself — assert it, and every other face, refuses outright.
            ItemStack testStack = new ItemStack(Items.DIAMOND);
            for (Direction direction : Direction.values()) {
                if (quarry.canAcceptFrom(direction, testStack)) {
                    context.fail("Laser quarry should NOT accept items from " + direction);
                    return;
                }

                if (quarry.addItem(direction, testStack)) {
                    context.fail("Laser quarry should NOT allow item insertion from " + direction);
                    return;
                }
            }

            context.succeed();
        });
    }

    /**
     * Test that laser quarry starts in CLEARING phase.
     */
    public static void testQuarryInitialPhase(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        // Verify starts in CLEARING phase
        if (quarry.getCurrentPhase() != QuarryPhase.CLEARING) {
            context.fail("Laser quarry should start in CLEARING phase, got: " + quarry.getCurrentPhase());
            return;
        }

        // Verify not finished
        if (quarry.isFinished()) {
            context.fail("Newly placed quarry should not be finished");
            return;
        }

        context.succeed();
    }

    /**
     * Wiki claim (Mining area): "Default (no markers): mines a 16×16 area centered on the quarry's
     * placement." Placing a quarry with no adjacent markers (via the real {@code setPlacedBy} path,
     * not just a raw block-state write) leaves custom bounds unset, so it falls back to that default
     * (the 16 config value itself is asserted in {@code common/src/test/.../laserquarry/LaserQuarryConfigTest}
     * — this test doesn't measure the resulting area).
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Mining_area">wiki/Laser Quarry.txt § Mining area</a>
     */
    public static void testQuarryHasNoCustomBoundsWithoutMarkers(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        // Exercise the real placement path (setPlacedBy checks for adjacent markers), rather than
        // relying on setBlock's placement having skipped it.
        ((LaserQuarryBlock) LogisticsAutomation.BLOCK.LASER_QUARRY)
                .setPlacedBy(context.getLevel(), context.absolutePos(pos), context.getBlockState(pos), null, ItemStack.EMPTY);

        if (quarry.hasCustomBounds()) {
            context.fail("A quarry placed without markers should not have custom bounds set");
            return;
        }

        context.succeed();
    }

    /**
     * Only the quarry's top face takes a pipe.
     *
     * <p>Read from six real pipes, one per face, after the server has ticked them — the cached
     * connection types asserted here are the ones the router and the renderer consume, and a pipe
     * only reaches the quarry's answer through the loader's {@code PipeConnectionLookup}
     * registration. Calling {@code quarry.getConnectionType} directly (what this test used to do)
     * skipped that registration entirely, so a pipe that could not see the quarry at all — no arm
     * rendered, no items delivered — still passed.
     */
    public static void testQuarryPipeConnection(GameTestHelper context) {
        BlockPos pos = new BlockPos(2, 2, 2);

        // Unpowered, so the quarry cannot clear or mine the pipes away while the test runs.
        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        for (Direction face : Direction.values()) {
            context.setBlock(pos.relative(face), LogisticsPipe.BLOCK.COPPER_TRANSPORT_PIPE);
        }

        // The connection cache is recalculated on tick, so give the server a few before reading it.
        context.runAfterDelay(3, () -> {
            for (Direction face : Direction.values()) {
                PipeBlockEntity pipe = context.getBlockEntity(pos.relative(face), PipeBlockEntity.class);
                if (pipe == null) {
                    context.fail("Expected a pipe block entity on the quarry's " + face + " face");
                    return;
                }

                PipeConnection.Type expected =
                        face == Direction.UP ? PipeConnection.Type.PIPE : PipeConnection.Type.NONE;
                PipeConnection.Type actual = pipe.getCachedConnectionType(face.getOpposite());
                if (actual != expected) {
                    context.fail("A pipe on the quarry's " + face + " face should report " + expected
                            + " toward it, got: " + actual);
                    return;
                }
            }

            context.succeed();
        });
    }

    /**
     * Test that laser quarry block state has correct FACING property.
     */
    public static void testQuarryFacing(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        // Place quarry
        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        BlockState state = context.getBlockState(pos);

        // Verify FACING property exists
        if (!state.hasProperty(LaserQuarryBlock.FACING)) {
            context.fail("Laser quarry should have FACING property");
            return;
        }

        // Verify FACING is a valid horizontal direction
        Direction facing = state.getValue(LaserQuarryBlock.FACING);
        if (facing == Direction.UP || facing == Direction.DOWN) {
            context.fail("Laser quarry FACING should be horizontal, got: " + facing);
            return;
        }

        context.succeed();
    }
}
