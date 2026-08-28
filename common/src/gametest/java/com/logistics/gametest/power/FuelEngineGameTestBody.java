package com.logistics.gametest.power;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.FuelEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FuelEngineGameTestBody {

    private static Fluid crudeOil() {
        return BuiltInRegistries.FLUID.get(LogisticsCore.resource("crude_oil").toIdentifier());
    }

    private static FuelEngineBlockEntity placePowered(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPower.BLOCK.FUEL_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        return (FuelEngineBlockEntity) context.getBlockEntity(pos);
    }

    /** A powered engine with fuel + water generates RF and warms up. */
    public static void testFuelEngineGeneratesFromFuel(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        FuelEngineBlockEntity engine = placePowered(context, pos);
        if (engine == null) {
            context.fail("Fuel engine block entity not found");
            return;
        }
        engine.fuelTank().tank().setContents(SimpleFluidKey.of(crudeOil()), FluidUnits.mb(500));
        engine.coolantTank().tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(500));

        context.runAfterDelay(20, () -> {
            FuelEngineBlockEntity e = (FuelEngineBlockEntity) context.getBlockEntity(pos);
            if (e.getEnergyStored() <= 0) {
                context.fail("Fuel engine should have generated energy, stored: " + e.getEnergyStored());
                return;
            }
            if (e.simulation().lastGenerationRate() <= 0) {
                context.fail("Fuel engine should be actively generating while burning");
                return;
            }
            // Proportional cooling settles the engine at a warm equilibrium: it heats above ambient while
            // burning, but adequate coolant keeps it far below the overheat threshold.
            double temp = e.simulation().temperature();
            if (temp <= 0 || temp > 150) {
                context.fail("Fuel engine with coolant should run warm but stable, temp: " + temp);
                return;
            }
            context.succeed();
        });
    }

    /** Without coolant the engine overheats; a wrench-style reset clears the shutdown and preserves tank fuel. */
    public static void testFuelEngineOverheatsWithoutCoolantThenResets(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        FuelEngineBlockEntity engine = placePowered(context, pos);
        if (engine == null) {
            context.fail("Fuel engine block entity not found");
            return;
        }
        engine.fuelTank().tank().setContents(SimpleFluidKey.of(crudeOil()), FluidUnits.mb(2000));
        // No coolant.

        context.runAfterDelay(120, () -> {
            FuelEngineBlockEntity e = (FuelEngineBlockEntity) context.getBlockEntity(pos);
            if (!e.isOverheated()) {
                context.fail("Fuel engine should overheat without coolant, temp: " + e.simulation().temperature());
                return;
            }
            if (e.simulation().committedFuelEnergy() != 0) {
                context.fail("Overheat should discard the committed fuel reserve");
                return;
            }
            if (e.fuelTank().tank().isEmpty()) {
                context.fail("Fuel remaining in the tank should be preserved through overheat");
                return;
            }
            long fuelBeforeReset = e.fuelTank().tank().getAmount();
            if (!e.resetOverheat() || e.isOverheated()) {
                context.fail("Wrench reset should clear the thermal shutdown");
                return;
            }
            if (e.fuelTank().tank().getAmount() != fuelBeforeReset) {
                context.fail("Wrench reset should not change the fuel tank contents");
                return;
            }
            context.succeed();
        });
    }

    /** The combined fluid view routes inserts by type: water to the coolant tank, fuel to the fuel tank. */
    public static void testFluidInsertRoutesByType(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPower.BLOCK.FUEL_ENGINE);
        FuelEngineBlockEntity engine = (FuelEngineBlockEntity) context.getBlockEntity(pos);

        long water = engine.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(100), false);
        if (water <= 0 || !engine.fuelTank().tank().isEmpty() || engine.coolantTank().tank().isEmpty()) {
            context.fail("Water should be accepted into the coolant tank only");
            return;
        }
        long fuel = engine.fluidStorage(Direction.UP).insert(SimpleFluidKey.of(crudeOil()), FluidUnits.mb(100), false);
        if (fuel <= 0 || engine.fuelTank().tank().isEmpty()) {
            context.fail("Fuel should be accepted into the fuel tank");
            return;
        }
        context.succeed();
    }
}
