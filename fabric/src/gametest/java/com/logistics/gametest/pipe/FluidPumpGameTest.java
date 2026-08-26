package com.logistics.gametest.pipe;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class FluidPumpGameTest {

    // Tests set this as a per-instance arm-speed override so the pump reaches the floor within a small tick
    // budget (the arm normally creeps down at 0.01 blocks/tick).
    private static final float FAST_ARM_SPEED = 16f;

    private static void fastArm(FluidPumpBlockEntity pump) {
        pump.setArmSpeedOverride(FAST_ARM_SPEED);
    }

    private static void succeed(GameTestHelper context) {
        context.succeed();
    }

    private static void fail(GameTestHelper context, String message) {
        context.fail(message);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testFluidPumpPlacement(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 2, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        if (!(context.getBlockEntity(pos) instanceof FluidPumpBlockEntity)) {
            fail(context, "Fluid pump should create a block entity");
        }
        succeed(context);
    }

    /**
     * Wiki claim (Power): "Supply it with RF from your power system — an engine, a Battery, or
     * Cables — to keep it draining." Faces other than the bottom (the world-facing intake) accept
     * both energy and fluid connections.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Power">wiki/Pump.txt § Power</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testFluidPumpEnergyAndTankAccessibleFromTopAndSides(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 2, 1);
        context.setBlock(pos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pos);
        if (pump == null) {
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        for (Direction direction : Direction.values()) {
            boolean expectAccess = direction != Direction.DOWN;
            IEnergyStorage energy = pump.energyStorage(direction);
            boolean hasEnergy = energy != null && energy.canInsert();
            if (hasEnergy != expectAccess) {
                fail(context, "Fluid pump energy access from " + direction + " should be " + expectAccess);
                return;
            }
            if ((pump.fluidStorage(direction) != null) != expectAccess) {
                fail(context, "Fluid pump tank access from " + direction + " should be " + expectAccess);
                return;
            }
            if (pump.acceptsLowTierEnergyFrom(direction) != expectAccess) {
                fail(context, "Fluid pump low-tier energy from " + direction + " should be " + expectAccess);
                return;
            }
        }
        succeed(context);
    }

    // NOTE: the wiki (Power section) says "with no power it stops," which reads as a blanket
    // statement. In practice only the fluid-draining step requires energy — the intake tube keeps
    // descending (searching for a source) with zero power, as this test confirms. See
    // testFluidPumpDoesNotDrainWithoutEnergy for the part of the claim that does hold: no power
    // means no fluid moves into the tank.
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testFluidPumpTubeDescendsWithoutEnergy(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 5, 1);
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fastArm(pump);
        float startArmY = pump.armY();

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 2L, () -> {
            if (pump.energyAmount() != 0) {
                fail(context, "Precondition failed: pump should have no energy");
                return;
            }
            if (pump.armY() >= startArmY - 1.0f) {
                fail(context, "Fluid pump tube should descend without energy");
                return;
            }
            succeed(context);
        });
    }

    /**
     * Wiki claim (Usage): "The Pump drains fluid source blocks from the world below it into an
     * internal buffer..."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testFluidPumpRemovesSourceAndFillsTank(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos waterPos = pumpPos.below();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(waterPos, Blocks.WATER);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY), false);

        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) + 2, () -> {
            if (!context.getBlockState(waterPos).isAir()) {
                fail(context, "Fluid pump should remove the water source");
                return;
            }
            if (pump.tank().getAmount() < FluidUnits.mb(1_000)
                    || pump.tank().getFluidKey().getFluid() != Fluids.WATER) {
                fail(context, "Fluid pump should store 1000 mB of water");
                return;
            }
            succeed(context);
        });
    }

    /**
     * Wiki claim (Power): "The Pump runs on RF and keeps its own internal buffer... with no power
     * it stops." An unpowered pump does not drain a source directly beneath its tube, even once the
     * tube reaches it.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Power">wiki/Pump.txt § Power</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testFluidPumpDoesNotDrainWithoutEnergy(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos waterPos = pumpPos.below();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(waterPos, Blocks.WATER);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        // No energy inserted — the tube reaches the source layer, but draining should never start.
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 3L, () -> {
            if (!context.getBlockState(waterPos).getFluidState().isSource()) {
                fail(context, "An unpowered pump should not drain the source below it");
                return;
            }
            if (pump.tank().getAmount() != 0) {
                fail(context, "An unpowered pump's tank should stay empty, got: " + pump.tank().getAmount());
                return;
            }
            succeed(context);
        });
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testFluidPumpDoesNotDrainWaterloggedBlocks(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos slabPos = pumpPos.below();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(
                slabPos, Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        if (pump == null) {
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) + 2, () -> {
            if (!context.getBlockState(slabPos).is(Blocks.OAK_SLAB)
                    || !context.getBlockState(slabPos).getValue(BlockStateProperties.WATERLOGGED)) {
                fail(context, "Fluid pump must not drain or replace a waterlogged block");
                return;
            }
            if (pump.tank().getAmount() > 0) {
                fail(context, "Fluid pump must not pump fluid out of a waterlogged block");
                return;
            }
            succeed(context);
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
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 4L, () -> {
            if (context.getBlockState(firstWater).getFluidState().isSource()
                    || context.getBlockState(connectedWater).getFluidState().isSource()) {
                fail(context, "Fluid pump should remove connected source blocks on the same body");
                return;
            }
            if (pump.tank().getAmount() < FluidUnits.mb(2_000)) {
                fail(context, "Fluid pump should store both connected sources");
                return;
            }
            succeed(context);
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
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 6L, () -> {
            for (BlockPos water : pool) {
                if (context.getBlockState(water).getFluidState().isSource()) {
                    fail(context, "Fluid pump should carve every source from a finite pool");
                    return;
                }
            }
            succeed(context);
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
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY), false);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 2L + 4, () -> {
            if (pump.tank().getAmount() < FluidUnits.mb(1_000)
                    || pump.tank().getFluidKey().getFluid() != Fluids.WATER) {
                fail(context, "Fluid pump should draw fluid from a large body");
                return;
            }
            if (!context.getBlockState(center).getFluidState().isSource()) {
                fail(context, "Fluid pump should not carve source blocks from a large body");
                return;
            }
            succeed(context);
        });
    }

    /**
     * Wiki claim (Usage): "...feeds them into an adjacent tank or fluid pipe." Setup step 3: "Place
     * a Glass Tank or Copper Fluid Pipe against the output."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
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
            fail(context, "Expected pump and fluid pipe block entities");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY), false);

        fastArm(pump);

        context.succeedWhen(() -> {
            if (pipe.totalMillibuckets() <= 0 || pipe.containedFluid().getFluid() != Fluids.WATER) {
                throw context.assertionException("Fluid pump should push water into the pipe above");
            }
        });
    }

    /**
     * Wiki claim (Usage): "...feeds them into an adjacent tank or fluid pipe" — the output is not
     * limited to a single face; a pipe on any non-bottom side works.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
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
            fail(context, "Expected pump and fluid pipe block entities");
            return;
        }
        pump.energyStorage(Direction.NORTH).insert(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_CAPACITY), false);

        fastArm(pump);

        context.succeedWhen(() -> {
            if (pipe.totalMillibuckets() <= 0 || pipe.containedFluid().getFluid() != Fluids.WATER) {
                throw context.assertionException("Fluid pump should push water into a pipe on its side");
            }
        });
    }

    /**
     * Wiki claim (Usage): "Outputs up to 62.5 mB/t into an adjacent tank or fluid pipe."
     *
     * <p>NOTE: the code's push-rate constant (FLUID_PUMP_PUSH_RATE_MB) is 400 mB/tick, not 62.5.
     * 62.5 mB/t (= 1000 mB ÷ FLUID_PUMP_INTERVAL_TICKS(16)) looks like the *sustained average intake
     * rate* (one bucket drained from the world every 16 ticks), mislabeled here as the output/push
     * rate. This test asserts the push path directly — bypassing draining by preloading the pump's
     * own tank — and confirms one tick moves exactly FLUID_PUMP_PUSH_RATE_MB, not ~62.5. See
     * WIKI_DISCREPANCIES.md § Pump.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 20)
    public void testFluidPumpPushRateIsFourHundredNotSixtyTwoPointFive(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos tankPos = pumpPos.above();
        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        GlassTankBlockEntity tank = (GlassTankBlockEntity) context.getBlockEntity(tankPos);
        if (pump == null || tank == null) {
            fail(context, "Expected pump and glass tank block entities");
            return;
        }
        // Preload the pump's own tank directly (bypassing the drain step) so the push rate is the
        // only thing under test.
        pump.tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(16_000), false);
        long before = tank.tank().getAmount();

        context.runAfterDelay(1, () -> {
            long delta = tank.tank().getAmount() - before;
            long expected = FluidUnits.mb(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_PUSH_RATE_MB));
            if (delta != expected) {
                fail(context, "Fluid pump should push exactly " + expected + " mB/tick, got " + delta + " mB");
                return;
            }
            succeed(context);
        });
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
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 5L, () -> {
            if (!context.getBlockState(a).isAir()
                    || !context.getBlockState(b).isAir()
                    || !context.getBlockState(c).isAir()) {
                fail(context, "Fluid pump should drain all connected lava sources at the same layer");
                return;
            }
            succeed(context);
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
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 5L, () -> {
            if (!context.getBlockState(a).isAir() || !context.getBlockState(c).isAir()) {
                fail(context, "Fluid pump should reach sources across flowing fluid in the same layer");
                return;
            }
            succeed(context);
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
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        // Lava flows (UPDATE_ALL), so flowing remnants decay on vanilla's slow schedule; the pump's job
        // is to remove every source block in the pool.
        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 12L, () -> {
            for (BlockPos p : pool) {
                if (context.getBlockState(p).getFluidState().isSource()) {
                    fail(context, "Open lava pool still has a source block at " + p);
                    return;
                }
            }
            succeed(context);
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
            fail(context, "Expected FluidPumpBlockEntity");
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
                fail(context, "Fluid pump should finish the layer even while a tank empties its buffer");
                return;
            }
            succeed(context);
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
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) * 3L, () -> {
            if (!context.getBlockState(water).isAir()) {
                fail(context, "Fluid pump should drain the water");
                return;
            }
            // The tube tip must not have descended into the solid floor (top of floor = pump Y - 1).
            if (pump.armY() < pump.getBlockPos().getY() - 1.0f) {
                fail(context, "Fluid pump tube descended into the solid floor");
                return;
            }
            succeed(context);
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
            fail(context, "Expected FluidPumpBlockEntity");
            return;
        }
        fillEnergy(pump);
        fastArm(pump);

        // After the first pump the furthest source is gone but the one under the tube remains.
        context.runAfterDelay(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS) + 6L, () -> {
            if (context.getBlockState(far).getFluidState().isSource()) {
                fail(context, "Fluid pump should drain the furthest source first");
                return;
            }
            if (!context.getBlockState(near).getFluidState().isSource()) {
                fail(context, "The source under the tube should be drained last");
                return;
            }
            succeed(context);
        });
    }

    /**
     * Wiki claim (Power/Usage): "Supply it with RF from your power system... to keep it draining,"
     * feeding "into an adjacent tank." The tests above prove draining and push-rate math by
     * inserting energy directly; this one proves the whole feature as a player wires it up — a real
     * engine (no cable) delivering power and a real Glass Tank receiving the output.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Power">wiki/Pump.txt § Power</a>
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testFluidPumpDrainsAndOutputsViaRealEngine(GameTestHelper context) {
        BlockPos pumpPos = new BlockPos(1, 3, 1);
        BlockPos waterPos = pumpPos.below();
        BlockPos tankPos = pumpPos.above();
        BlockPos enginePos = pumpPos.north();
        BlockPos redstoneBlockPos = enginePos.north();

        context.setBlock(pumpPos, LogisticsAutomation.BLOCK.FLUID_PUMP);
        context.setBlock(waterPos, Blocks.WATER);
        context.setBlock(tankPos, LogisticsPipe.BLOCK.GLASS_TANK);
        context.setBlock(redstoneBlockPos, Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.SOUTH)
                .setValue(AbstractEngineBlock.POWERED, true));

        FluidPumpBlockEntity pump = (FluidPumpBlockEntity) context.getBlockEntity(pumpPos);
        GlassTankBlockEntity tank = (GlassTankBlockEntity) context.getBlockEntity(tankPos);
        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(enginePos);
        if (pump == null || tank == null || engine == null) {
            fail(context, "Expected pump, tank, and engine block entities");
            return;
        }
        // Cycle 20 -> 40 -> 80 -> 160 RF/t, comfortably above the pump's 150 RF/t input cap.
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        fastArm(pump);

        context.succeedWhen(() -> {
            if (tank.tank().getAmount() <= 0 || tank.tank().getFluidKey().getFluid() != Fluids.WATER) {
                throw context.assertionException("Engine-powered pump should push drained water into the tank");
            }
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
