package com.logistics.gametest.pipe;

import com.logistics.LogisticsFluid;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.FluidPumpBlockEntity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
    public void testFluidPumpEnergyAndTankAccessibleFromTopAndSides(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 2, 1);
        context.setBlock(pos, LogisticsFluid.BLOCK.FLUID_PUMP);
        FluidPumpBlockEntity pump = context.getBlockEntity(pos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        for (Direction direction : Direction.values()) {
            boolean expectAccess = direction != Direction.DOWN;
            IEnergyStorage energy = pump.energyStorage(direction);
            boolean hasEnergy = energy != null && energy.canInsert();
            if (hasEnergy != expectAccess) {
                context.fail("Fluid pump energy access from " + direction + " should be " + expectAccess);
                return;
            }
            if ((pump.fluidStorage(direction) != null) != expectAccess) {
                context.fail("Fluid pump tank access from " + direction + " should be " + expectAccess);
                return;
            }
            if (pump.acceptsLowTierEnergyFrom(direction) != expectAccess) {
                context.fail("Fluid pump low-tier energy from " + direction + " should be " + expectAccess);
                return;
            }
        }
        context.succeed();
    }

    @GameTest(maxTicks = 40)
    public void testFluidPumpTubeDescendsWithoutEnergy(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 5, 1);
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        LogisticsConfig.get().fluidPump.armSpeed = 16f;
        float startArmY = pump.armY();

        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks * 2L, () -> {
            if (pump.energyAmount() != 0) {
                context.fail("Precondition failed: pump should have no energy");
                return;
            }
            if (pump.armY() >= startArmY - 1.0f) {
                context.fail("Fluid pump tube should descend without energy");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(maxTicks = 40)
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

        LogisticsConfig.get().fluidPump.armSpeed = 16f;

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

    @GameTest(maxTicks = 120)
    public void testFluidPumpFindsConnectedSourceInRadius(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 3, 3);
        BlockPos firstWater = pumpPos.below();
        BlockPos connectedWater = firstWater.east();
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);
        context.setBlock(firstWater, Blocks.WATER);
        context.setBlock(connectedWater, Blocks.WATER);
        encloseWater(context, firstWater, connectedWater);

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        LogisticsConfig.get().fluidPump.armSpeed = 16f;

        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks * 4L, () -> {
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

    @GameTest(maxTicks = 160)
    public void testFluidPumpConsumesFinitePoolWithoutReflow(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 3, 3);
        BlockPos firstWater = pumpPos.below();
        BlockPos[] pool = {firstWater, firstWater.east(), firstWater.south(), firstWater.east().south()};
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);
        for (BlockPos water : pool) {
            context.setBlock(water, Blocks.WATER);
        }
        encloseWater(context, pool);

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        LogisticsConfig.get().fluidPump.armSpeed = 16f;

        // 4 sources < default threshold (9) => finite pool, drained permanently with no reflow.
        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks * 6L, () -> {
            for (BlockPos water : pool) {
                if (!context.getBlockState(water).isAir()) {
                    context.fail("Fluid pump should fully drain the finite pool without reflow");
                    return;
                }
            }
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void testFluidPumpTreatsLargeBodyAsInfinite(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 5, 3);
        BlockPos center = pumpPos.below();
        context.setBlock(pumpPos, LogisticsFluid.BLOCK.FLUID_PUMP);
        // 3x3 flat body (9 sources) >= default threshold (9) => infinite, no carving.
        BlockPos[] body = new BlockPos[9];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                body[i] = center.offset(dx, 0, dz);
                context.setBlock(body[i], Blocks.WATER);
                i++;
            }
        }
        encloseWater(context, body);

        FluidPumpBlockEntity pump = context.getBlockEntity(pumpPos, FluidPumpBlockEntity.class);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfig.get().fluidPump.energyCapacity, false);
        LogisticsConfig.get().fluidPump.armSpeed = 16f;

        context.runAfterDelay(LogisticsConfig.get().fluidPump.pumpIntervalTicks * 2L + 4, () -> {
            if (pump.tank().getAmount() < FluidUnits.mb(1_000)
                    || pump.tank().getFluidKey().getFluid() != Fluids.WATER) {
                context.fail("Fluid pump should draw fluid from a large body");
                return;
            }
            if (!context.getBlockState(center).getFluidState().isSource()) {
                context.fail("Fluid pump should not carve source blocks from a large body");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(maxTicks = 60)
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

        LogisticsConfig.get().fluidPump.armSpeed = 16f;

        context.succeedWhen(() -> {
            if (pipe.totalMillibuckets() <= 0 || pipe.containedFluid().getFluid() != Fluids.WATER) {
                throw context.assertionException("Fluid pump should push water into the pipe above");
            }
        });
    }

    @GameTest(maxTicks = 60)
    public void testFluidPumpOutputsToPipeOnSide(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos pipePos = pumpPos.east();
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

        LogisticsConfig.get().fluidPump.armSpeed = 16f;

        context.succeedWhen(() -> {
            if (pipe.totalMillibuckets() <= 0 || pipe.containedFluid().getFluid() != Fluids.WATER) {
                throw context.assertionException("Fluid pump should push water into a pipe on its side");
            }
        });
    }

    // Fills the energy buffer; a single insert is capped at maxEnergyInput, only enough for one pump.
    private static void fillEnergy(FluidPumpBlockEntity pump) {
        long capacity = LogisticsConfig.get().fluidPump.energyCapacity;
        long input = Math.max(1, LogisticsConfig.get().fluidPump.maxEnergyInput);
        for (long filled = 0; filled <= capacity; filled += input) {
            pump.energyStorage(Direction.NORTH).insert(capacity, false);
        }
    }

    // Encloses the given water blocks in stone (floor + walls) so the pool is a static body that
    // can't flow away in the open test structure.
    private static void encloseWater(GameTestHelper context, BlockPos... water) {
        Set<BlockPos> pool = new HashSet<>(Arrays.asList(water));
        for (BlockPos w : water) {
            context.setBlock(w.below(), Blocks.STONE);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = w.relative(dir);
                if (!pool.contains(neighbor)) {
                    context.setBlock(neighbor, Blocks.STONE);
                }
            }
        }
    }
}
