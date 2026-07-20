package com.logistics.gametest.power;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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
        return context.getBlockEntity(pos, SteamEngineBlockEntity.class);
    }

    /** A powered engine with fuel + water builds pressure, generates RF, and never overheats. */
    @GameTest(maxTicks = 120)
    public void testSteamEngineBuildsPressureThenSagsWhenDrained(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        SteamEngineBlockEntity engine = placePowered(context, pos);
        if (engine == null) {
            context.fail("Steam engine block entity not found");
            return;
        }
        engine.setTheItem(new ItemStack(Items.COAL, 16));
        engine.waterTank().tank().setContents(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(500));

        context.runAfterDelay(40, () -> {
            SteamEngineBlockEntity e = context.getBlockEntity(pos, SteamEngineBlockEntity.class);
            if (e.getEnergyStored() <= 0) {
                context.fail("Steam engine should have generated energy, stored: " + e.getEnergyStored());
                return;
            }
            double built = e.simulation().pressure();
            if (built <= 0) {
                context.fail("Steam engine should have built pressure, pressure: " + built);
                return;
            }
            // Pressure, not temperature, is the state variable: it can never overheat.
            if (e.getHeatStage() != HeatStage.COLD) {
                context.fail("Steam engine should never leave the COLD stage, stage: " + e.getHeatStage());
                return;
            }
            if (e.isOverheated()) {
                context.fail("Steam engine must never overheat");
                return;
            }

            // Cut the water supply; the turbine keeps drawing pressure down with no boiler to replenish it.
            e.waterTank().tank().extract(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(10_000), false);

            context.runAfterDelay(60, () -> {
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

    /** The water-only fluid view accepts water into the boiler tank and rejects other fluids. */
    @GameTest
    public void testWaterTankAcceptsOnlyWater(GameTestHelper context) {
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
