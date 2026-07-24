package com.logistics.gametest.power;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

public class SteamEngineGameTest {

    private static SteamEngineBlockEntity placePowered(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPower.BLOCK.STEAM_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        return (SteamEngineBlockEntity) context.getBlockEntity(pos);
    }

    private static void primeFuelAndWater(SteamEngineBlockEntity engine) {
        engine.setTheItem(new ItemStack(Items.COAL, 16));
        engine.waterTank().tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(4000));
    }

    private static long batteryStored(GameTestHelper context, BlockPos pos) {
        BatteryBlockEntity b = (BatteryBlockEntity) context.getBlockEntity(pos);
        return ((EnergyComponent) b.energyStorage(null)).getAmount();
    }

    /**
     * With a consumer on the output face, the engine first <em>heats</em> the boiler (~500+ ticks past the
     * boiling point) before steam builds pressure, which it pushes as RF <em>directly</em> (no RF buffer,
     * no energy capability) — the battery accumulates it. Then draining water sags the pressure.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1000)
    public void testSteamEngineDeliversRfFromPressureThenSags(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        BlockPos batteryPos = new BlockPos(1, 1, 0); // output faces EAST
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        SteamEngineBlockEntity engine = placePowered(context, pos);
        if (engine == null) {
            context.fail("Steam engine block entity not found");
            return;
        }
        primeFuelAndWater(engine);

        context.runAfterDelay(800, () -> {
            SteamEngineBlockEntity e = (SteamEngineBlockEntity) context.getBlockEntity(pos);
            double built = e.simulation().pressure();
            if (built <= 0) {
                context.fail("Steam engine should have built pressure once hot, pressure: " + built);
                return;
            }
            if (batteryStored(context, batteryPos) <= 0) {
                context.fail("Consumer should have received RF pushed from pressure, stored: "
                        + batteryStored(context, batteryPos));
                return;
            }
            // The Steam Engine has no overheat latch — heat clamps and discards, never fails.
            if (e.isOverheated()) {
                context.fail("Steam engine must never overheat");
                return;
            }

            // Cut the water; the turbine keeps drawing pressure down with no boiler to replenish it.
            e.waterTank().tank().extract(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(100_000), false);
            context.runAfterDelay(150, () -> {
                SteamEngineBlockEntity drained = (SteamEngineBlockEntity) context.getBlockEntity(pos);
                if (drained.simulation().pressure() >= built) {
                    context.fail("Pressure should sag once water runs out, was " + built
                            + " now " + drained.simulation().pressure());
                    return;
                }
                context.succeed();
            });
        });
    }

    /** RF pushed from pressure flows through a cable to a consumer, even with no engine capability. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 900)
    public void testSteamEngineDeliversThroughCable(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        BlockPos cablePos = new BlockPos(1, 1, 0); // output faces EAST → cable → battery
        BlockPos batteryPos = new BlockPos(2, 1, 0);
        context.setBlock(cablePos, LogisticsPower.BLOCK.COPPER_CABLE);
        context.setBlock(batteryPos, LogisticsPower.BLOCK.BATTERY);
        SteamEngineBlockEntity engine = placePowered(context, pos);
        if (engine == null) {
            context.fail("Steam engine block entity not found");
            return;
        }
        primeFuelAndWater(engine);

        context.runAfterDelay(850, () -> {
            if (batteryStored(context, batteryPos) <= 0) {
                context.fail("Consumer behind a cable should receive RF, stored: "
                        + batteryStored(context, batteryPos));
                return;
            }
            context.succeed();
        });
    }

    /** Once hot, the boiler builds pressure with no consumer but the turbine delivers nothing. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 800)
    public void testSteamEngineHoldsPressureWithNoConsumer(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        SteamEngineBlockEntity engine = placePowered(context, pos);
        if (engine == null) {
            context.fail("Steam engine block entity not found");
            return;
        }
        primeFuelAndWater(engine);

        context.runAfterDelay(750, () -> {
            SteamEngineBlockEntity e = (SteamEngineBlockEntity) context.getBlockEntity(pos);
            if (e.simulation().pressure() <= 0) {
                context.fail("Boiler should build pressure with no consumer, pressure: " + e.simulation().pressure());
                return;
            }
            if (e.simulation().lastGenerationRate() != 0) {
                context.fail("No consumer → no RF delivered, rate: " + e.simulation().lastGenerationRate());
                return;
            }
            context.succeed();
        });
    }

    /** The water-only fluid view accepts water into the boiler tank and rejects other fluids. */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testWaterTankAcceptsOnlyWater(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.STEAM_ENGINE);
        SteamEngineBlockEntity engine = (SteamEngineBlockEntity) context.getBlockEntity(pos);

        long water = engine.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(100), false);
        if (water <= 0 || engine.waterTank().tank().isEmpty()) {
            context.fail("Water should be accepted into the boiler tank");
            return;
        }
        long lava = engine.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(100), false);
        if (lava != 0) {
            context.fail("The boiler tank should reject non-water fluids");
            return;
        }
        context.succeed();
    }
}
