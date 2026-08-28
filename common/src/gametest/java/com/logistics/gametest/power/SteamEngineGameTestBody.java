package com.logistics.gametest.power;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/**
 * Shared steam engine GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each
 * loader's own registration mechanism: Fabric's {@code @GameTest}-annotated
 * {@code SteamEngineGameTest} delegates to these methods, and NeoForge's
 * {@code SteamEngineGameTestRegistration} references them directly as
 * {@code Consumer<GameTestHelper>} method references.
 */
public class SteamEngineGameTestBody {

    private static SteamEngineBlockEntity placePowered(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPower.BLOCK.STEAM_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        return context.getBlockEntity(pos, SteamEngineBlockEntity.class);
    }

    private static void primeFuelAndWater(SteamEngineBlockEntity engine) {
        engine.setTheItem(new ItemStack(Items.COAL, 16));
        engine.waterTank().tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(4000));
    }

    private static long batteryStored(GameTestHelper context, BlockPos pos) {
        BatteryBlockEntity b = context.getBlockEntity(pos, BatteryBlockEntity.class);
        return ((EnergyComponent) b.energyStorage(null)).getAmount();
    }

    /**
     * With a consumer on the output face, the engine first <em>heats</em> the boiler (~500+ ticks past the
     * boiling point) before steam builds pressure, which it pushes as RF <em>directly</em> (no RF buffer,
     * no energy capability) — the battery accumulates it. Then draining water sags the pressure.
     */
    public static void testSteamEngineDeliversRfFromPressureThenSags(GameTestHelper context) {
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
            SteamEngineBlockEntity e = context.getBlockEntity(pos, SteamEngineBlockEntity.class);
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
                SteamEngineBlockEntity drained = context.getBlockEntity(pos, SteamEngineBlockEntity.class);
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
    public static void testSteamEngineDeliversThroughCable(GameTestHelper context) {
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
    public static void testSteamEngineHoldsPressureWithNoConsumer(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        SteamEngineBlockEntity engine = placePowered(context, pos);
        if (engine == null) {
            context.fail("Steam engine block entity not found");
            return;
        }
        primeFuelAndWater(engine);

        context.runAfterDelay(750, () -> {
            SteamEngineBlockEntity e = context.getBlockEntity(pos, SteamEngineBlockEntity.class);
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
    public static void testWaterTankAcceptsOnlyWater(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.STEAM_ENGINE);
        SteamEngineBlockEntity engine = context.getBlockEntity(pos, SteamEngineBlockEntity.class);

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
