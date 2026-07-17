package com.logistics.gametest.pipe;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.automation.pump.FluidPumpBlockEntity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;

public class FluidPumpGameTest {

    // Tests set this as a per-instance arm-speed override so the pump reaches the floor within a small tick
    // budget (the arm normally creeps down at 0.01 blocks/tick).
    private static final float FAST_ARM_SPEED = 16f;

    private static void fastArm(FluidPumpBlockEntity pump) {
        pump.setArmSpeedOverride(FAST_ARM_SPEED);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testFluidPumpPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 2, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        if (!(context.getBlockEntity(pos) instanceof FluidPumpBlockEntity)) {
            context.fail("Fluid pump should create a block entity");
        }
        context.succeed();
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testFluidPumpEnergyAndTankAccessibleFromTopAndSides(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 2, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pos);
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

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testFluidPumpTubeDescendsWithoutEnergy(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 5, 1);
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fastArm(pump);
        float startArmY = pump.armY();

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 2L, () -> {
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

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testFluidPumpRemovesSourceAndFillsTank(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos waterPos = pumpPos.below();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(waterPos, Blocks.WATER);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY), false);

        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) + 2, () -> {
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

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 120)
    public void testFluidPumpFindsConnectedSourceInRadius(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 3, 3);
        BlockPos firstWater = pumpPos.below();
        BlockPos connectedWater = firstWater.east();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(firstWater, Blocks.WATER);
        context.setBlock(connectedWater, Blocks.WATER);
        encloseWater(context, firstWater, connectedWater);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 4L, () -> {
            if (context.getBlockState(firstWater).getFluidState().isSource()
                    || context.getBlockState(connectedWater).getFluidState().isSource()) {
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

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 160)
    public void testFluidPumpDrainsFinitePool(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(2, 4, 2);
        BlockPos firstWater = pumpPos.below();
        // 3 sources < infinite threshold (9), in a line that can't reform sources, so the pump carves
        // the whole pool. Water flows (UPDATE_ALL), so assert no sources remain rather than full air
        // (flowing remnants decay on vanilla's schedule).
        BlockPos[] pool = {firstWater, firstWater.east(), firstWater.east().east()};
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        for (BlockPos water : pool) {
            context.setBlock(water, Blocks.WATER);
        }
        encloseWater(context, pool);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 6L, () -> {
            for (BlockPos water : pool) {
                if (context.getBlockState(water).getFluidState().isSource()) {
                    context.fail("Fluid pump should carve every source from a finite pool");
                    return;
                }
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testFluidPumpTreatsLargeBodyAsInfinite(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 5, 3);
        BlockPos center = pumpPos.below();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
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

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY), false);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 2L + 4, () -> {
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

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testFluidPumpOutputsToPipeAbove(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos pipePos = pumpPos.above();
        BlockPos waterPos = pumpPos.below();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.COPPER_FLUID_PIPE);
        context.setBlock(waterPos, Blocks.WATER);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) context.getBlockEntity(pipePos);
        if (pump == null || pipe == null) {
            context.fail("Expected pump and fluid pipe block entities");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY), false);

        fastArm(pump);

        context.succeedWhen(() -> context.assertTrue(
                pipe.totalMillibuckets() > 0 && pipe.containedFluid().getFluid() == Fluids.WATER,
                "Fluid pump should push water into the pipe above"));
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testFluidPumpOutputsToPipeOnSide(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos pipePos = pumpPos.east();
        BlockPos waterPos = pumpPos.below();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(pipePos, LogisticsPipe.BLOCK.COPPER_FLUID_PIPE);
        context.setBlock(waterPos, Blocks.WATER);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) context.getBlockEntity(pipePos);
        if (pump == null || pipe == null) {
            context.fail("Expected pump and fluid pipe block entities");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY), false);

        fastArm(pump);

        context.succeedWhen(() -> context.assertTrue(
                pipe.totalMillibuckets() > 0 && pipe.containedFluid().getFluid() == Fluids.WATER,
                "Fluid pump should push water into a pipe on its side"));
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void testFluidPumpDrainsConnectedLavaSources(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(2, 4, 2);
        BlockPos a = pumpPos.below();
        BlockPos b = a.east();
        BlockPos c = b.east();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(a, Blocks.LAVA);
        context.setBlock(b, Blocks.LAVA);
        context.setBlock(c, Blocks.LAVA);
        encloseWater(context, a, b, c);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 5L, () -> {
            if (!context.getBlockState(a).isAir()
                    || !context.getBlockState(b).isAir()
                    || !context.getBlockState(c).isAir()) {
                context.fail("Fluid pump should drain all connected lava sources at the same layer");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void testFluidPumpCrossesFlowingToReachSources(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(2, 4, 2);
        BlockPos a = pumpPos.below();
        BlockPos b = a.east();
        BlockPos c = b.east();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(a, Blocks.LAVA);
        // Flowing lava between the two sources: the flood must cross it to reach the far source.
        context.setBlock(b, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        context.setBlock(c, Blocks.LAVA);
        encloseWater(context, a, b, c);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 5L, () -> {
            if (!context.getBlockState(a).isAir() || !context.getBlockState(c).isAir()) {
                context.fail("Fluid pump should reach sources across flowing fluid in the same layer");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testFluidPumpDrainsOpenLavaPool(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(3, 4, 3);
        BlockPos center = pumpPos.below();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                context.setBlock(center.offset(dx, -1, dz), Blocks.STONE);
            }
        }
        BlockPos[] pool = new BlockPos[9];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                pool[i] = center.offset(dx, 0, dz);
                context.setBlock(pool[i], Blocks.LAVA);
                i++;
            }
        }
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        // Lava flows (UPDATE_ALL), so flowing remnants decay on vanilla's slow schedule; the pump's job
        // is to remove every source block in the pool.
        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 12L, () -> {
            for (BlockPos p : pool) {
                if (context.getBlockState(p).getFluidState().isSource()) {
                    context.fail("Open lava pool still has a source block at " + p);
                    return;
                }
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void testFluidPumpFinishesLayerWithOutputTank(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(2, 4, 2);
        BlockPos tankPos = pumpPos.above();
        BlockPos a = pumpPos.below();
        BlockPos b = a.east();
        BlockPos c = b.east();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        context.setBlock(a, Blocks.WATER);
        context.setBlock(b, Blocks.WATER);
        context.setBlock(c, Blocks.WATER);
        encloseWater(context, a, b, c);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        // The tank instantly empties the pump's buffer each pump; the pump must still finish the layer.
        // Water flows (UPDATE_ALL), so assert no sources remain rather than full air.
        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 4L, () -> {
            if (context.getBlockState(a).getFluidState().isSource()
                    || context.getBlockState(b).getFluidState().isSource()
                    || context.getBlockState(c).getFluidState().isSource()) {
                context.fail("Fluid pump should finish the layer even while a tank empties its buffer");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testFluidPumpStallsAboveSolidFloor(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(2, 5, 2);
        BlockPos water = pumpPos.below();
        BlockPos floor = water.below();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(water, Blocks.WATER);
        encloseWater(context, water);
        context.setBlock(floor, Blocks.STONE);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 3L, () -> {
            if (!context.getBlockState(water).isAir()) {
                context.fail("Fluid pump should drain the water");
                return;
            }
            // The tube tip must not have descended into the solid floor (top of floor = pump Y - 1).
            if (pump.armY() < pump.getBlockPos().getY() - 1.0f) {
                context.fail("Fluid pump tube descended into the solid floor");
                return;
            }
            context.succeed();
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testFluidPumpDrainsFurthestFirst(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 4, 2);
        BlockPos near = pumpPos.below();
        BlockPos mid = near.east();
        BlockPos far = mid.east();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(near, Blocks.WATER);
        context.setBlock(mid, Blocks.WATER);
        context.setBlock(far, Blocks.WATER);
        encloseWater(context, near, mid, far);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            context.fail("Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        // After the first pump the furthest source is gone but the one under the tube remains.
        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) + 6L, () -> {
            if (context.getBlockState(far).getFluidState().isSource()) {
                context.fail("Fluid pump should drain the furthest source first");
                return;
            }
            if (!context.getBlockState(near).getFluidState().isSource()) {
                context.fail("The source under the tube should be drained last");
                return;
            }
            context.succeed();
        });
    }

    // Fills the energy buffer; a single insert is capped at maxEnergyInput, only enough for one pump.
    private static void fillEnergy(FluidPumpBlockEntity pump) {
        long capacity = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY);
        long input = Math.max(1, LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_MAX_ENERGY_INPUT));
        for (long filled = 0; filled <= capacity; filled += input) {
            pump.energyStorage(Direction.NORTH).insert(input, false);
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
