package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.entity.LaserQuarryBlockEntity;
import com.logistics.core.lib.block.capability.PipeConnection;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import com.logistics.core.lib.energy.IEnergyStorage;

public class QuarryGameTest {

    /**
     * Test that laser quarry can be placed and creates block entity.
     */
    @GameTest
    public void testQuarryPlacement(GameTestHelper context) {
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
     * Test that laser quarry accepts energy from all sides.
     */
    @GameTest
    public void testQuarryAcceptsEnergy(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

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

    /**
     * Test that laser quarry does NOT accept items from pipes.
     * Quarry only outputs items, never accepts them.
     */
    @GameTest
    public void testQuarryDoesNotAcceptItems(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

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
    @GameTest
    public void testQuarryInitialPhase(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

        if (quarry == null) {
            context.fail("Expected LaserQuarryBlockEntity");
            return;
        }

        // Verify starts in CLEARING phase
        if (quarry.getCurrentPhase() != LaserQuarryBlockEntity.Phase.CLEARING) {
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
     * Test that laser quarry reports correct pipe connection type.
     * Should only connect to pipes from above (Direction.UP).
     */
    @GameTest
    public void testQuarryPipeConnection(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);

        context.setBlock(pos, LogisticsAutomation.BLOCK.LASER_QUARRY);
        LaserQuarryBlockEntity quarry = context.getBlockEntity(pos, LaserQuarryBlockEntity.class);

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
    @GameTest
    public void testQuarryFacing(GameTestHelper context) {
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
