package com.logistics.gametest.pipe;

import com.logistics.LogisticsFluid;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.FluidPumpBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public class FluidPumpGameTest {

    @GameTest
    public void testFluidPumpPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 2, 1);
        context.setBlock(pos, LogisticsFluid.BLOCK.FLUID_PUMP);
        if (context.getBlockEntity(pos, FluidPumpBlockEntity.class) == null) {
            context.fail("Fluid pump should create a block entity");
        }
        context.succeed();
    }

    @GameTest
    public void testFluidPumpAcceptsEnergyFromAllSides(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 2, 1);
        context.setBlock(pos, LogisticsFluid.BLOCK.FLUID_PUMP);
        FluidPumpBlockEntity pump = context.getBlockEntity(pos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        for (Direction direction : Direction.values()) {
            IEnergyStorage storage = pump.energyStorage(direction);
            if (storage == null || !storage.canInsert()) {
                context.fail("Fluid pump should accept energy from " + direction);
                return;
            }
            if (!pump.acceptsLowTierEnergyFrom(direction)) {
                context.fail("Fluid pump should accept low-tier energy from " + direction);
                return;
            }
        }
        context.succeed();
    }

    @GameTest
    public void testFluidPumpTubeDescendsWithoutEnergy(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 5, 1);
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }

        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks * 3L, () -> {
            if (pump.energyAmount() != 0) {
                context.fail("Precondition failed: pump should have no energy");
                return;
            }
            if (pump.armY() >= pumpPos.getY() - 1.1f) {
                context.fail("Fluid pump tube should descend without energy");
                return;
            }
            context.succeed();
        });
    }

    @GameTest
    public void testFluidPumpRemovesSourceAndFillsTank(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos waterPos = pumpPos.below();
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);
        context.setBlock(waterPos, Blocks.WATER);

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfig.get().fluidPump.energyCapacity, false);

        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks + 2, () -> {
            if (!context.getBlockState(waterPos).isAir()) {
                context.fail("Fluid pump should remove the water source");
                return;
            }
            if (pump.tank().getAmount() < FluidUnits.mb(1_000)
                    || pump.tank().getFluidKey().getFluid() != Fluids.WATER) {
                context.fail("Fluid pump should store 1000 mB of water");
                return;
            }
            context.succeed();
        });
    }

    @GameTest
    public void testFluidPumpFindsConnectedSourceInRadius(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 3, 3);
        BlockPos firstWater = pumpPos.below();
        BlockPos connectedWater = firstWater.east();
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);
        context.setBlock(firstWater, Blocks.WATER);
        context.setBlock(connectedWater, Blocks.WATER);

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfig.get().fluidPump.energyCapacity, false);

        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks * 2L + 4, () -> {
            if (!context.getBlockState(firstWater).isAir() || !context.getBlockState(connectedWater).isAir()) {
                context.fail("Fluid pump should remove connected source blocks on the same body");
                return;
            }
            if (pump.tank().getAmount() < FluidUnits.mb(2_000)) {
                context.fail("Fluid pump should store both connected sources");
                return;
            }
            context.succeed();
        });
    }

    @GameTest
    public void testFluidPumpConsumesFinitePoolWithoutReflow(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 3, 3);
        BlockPos firstWater = pumpPos.below();
        BlockPos[] pool = {firstWater, firstWater.east(), firstWater.south(), firstWater.east().south()};
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);
        for (BlockPos water : pool) {
            context.setBlock(water, Blocks.WATER);
        }

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfig.get().fluidPump.energyCapacity, false);

        // 4 sources < default threshold (9) => finite pool, drained permanently with no reflow.
        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks * 5L + 20L, () -> {
            for (BlockPos water : pool) {
                if (!context.getBlockState(water).isAir()) {
                    context.fail("Fluid pump should fully drain the finite pool without reflow");
                    return;
                }
            }
            context.succeed();
        });
    }

    @GameTest
    public void testFluidPumpTreatsLargeBodyAsInfinite(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 5, 3);
        BlockPos center = pumpPos.below();
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);
        // 4x4 flat body (16 sources) >= default threshold (9) => infinite, no carving.
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                context.setBlock(center.offset(dx, 0, dz), Blocks.WATER);
            }
        }

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfig.get().fluidPump.energyCapacity, false);

        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks * 2L + 4, () -> {
            if (pump.tank().getAmount() < FluidUnits.mb(1_000)
                    || pump.tank().getFluidKey().getFluid() != Fluids.WATER) {
                context.fail("Fluid pump should draw fluid from a large body");
                return;
            }
            if (!context.getLevel().getFluidState(center).isSource()) {
                context.fail("Fluid pump should not carve source blocks from a large body");
                return;
            }
            context.succeed();
        });
    }

    @GameTest
    public void testFluidPumpOutputsToPipeAbove(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos pipePos = pumpPos.above();
        BlockPos waterPos = pumpPos.below();
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);
        context.setBlock(pipePos, LogisticsFluid.BLOCK.COPPER_FLUID_PIPE);
        context.setBlock(waterPos, Blocks.WATER);

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        FluidPipeBlockEntity pipe = context.getBlockEntity(pipePos, FluidPipeBlockEntity.class);
        if (pump == null || pipe == null) {
            context.fail("Expected pump and fluid pipe block entities");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfig.get().fluidPump.energyCapacity, false);

        context.succeedWhen(() -> {
            if (pipe.totalMillibuckets() <= 0 || pipe.containedFluid().getFluid() != Fluids.WATER) {
                throw context.assertionException("Fluid pump should push water into the pipe above");
            }
        });
    }
}
