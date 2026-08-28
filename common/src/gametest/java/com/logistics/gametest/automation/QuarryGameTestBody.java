package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.automation.laserquarry.entity.QuarryPhase;
import com.logistics.core.lib.block.capability.PipeConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import com.logistics.core.lib.energy.IEnergyStorage;

public class QuarryGameTestBody {

    /**
     * Test that laser quarry can be placed and creates block entity.
     */
    public static void testQuarryPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        // Place quarry
        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);

        // Verify block entity exists
        LaserQuarryBlockEntity blockEntity = (LaserQuarryBlockEntity) context.getBlockEntity(pos);
        if (blockEntity == null) {
            context.fail("Laser quarry should create LaserQuarryBlockEntity");
            return;
        }

        context.succeed();
    }

    /**
     * Test that laser quarry accepts energy from all sides.
     */
    public static void testQuarryAcceptsEnergy(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(pos);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        // Test all directions provide energy storage
        for (Direction direction : Direction.values()) {
            IEnergyStorage storage = quarry.energyStorage(direction);
            if (storage == null) {
                context.fail("Laser quarry should accept energy from " + direction);
                return;
            }

            if (!storage.canInsert()) {
                context.fail("Laser quarry energy storage should support insertion from " + direction);
                return;
            }
        }

        context.succeed();
    }

    public static void testQuarryTracksCommittedEnergyInput(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(pos);

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
     * Test that laser quarry does NOT accept items from pipes.
     * Quarry only outputs items, never accepts them.
     */
    public static void testQuarryDoesNotAcceptItems(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(pos);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        // Create a test item stack
        ItemStack testStack = new ItemStack(Items.DIAMOND);

        // Test all directions reject items
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
    }

    /**
     * Test that laser quarry starts in CLEARING phase.
     */
    public static void testQuarryInitialPhase(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(pos);

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
        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(pos);

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
     * Test that laser quarry reports correct pipe connection type.
     * Should only connect to pipes from above (Direction.UP).
     */
    public static void testQuarryPipeConnection(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = (LaserQuarryBlockEntity) context.getBlockEntity(pos);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        // Test UP direction (should connect as PIPE)
        PipeConnection.Type upConnection = quarry.getConnectionType(Direction.UP);
        if (upConnection != PipeConnection.Type.PIPE) {
            context.fail("Laser quarry should accept pipe connection from UP, got: " + upConnection);
            return;
        }

        // Test all other directions (should be NONE)
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP)
                continue;

            PipeConnection.Type connection = quarry.getConnectionType(direction);
            if (connection != PipeConnection.Type.NONE) {
                context.fail("Laser quarry should NOT connect to pipes from " + direction + ", got: " + connection);
                return;
            }
        }

        context.succeed();
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
