package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsCore;
import com.logistics.automation.refinery.RefineryBlockEntity;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluid;

/**
 * Gametests for the Refinery: fluid-in / fluid-out distillation, RF cost, and the input/output tank
 * split (insertion always targets the input tank, extraction always draws the output tank,
 * regardless of side — see {@code RefineryBlockEntity.RefineryFluidView}).
 */
public class RefineryGameTest {

    private static Fluid liquidBiomass() {
        return BuiltInRegistries.FLUID.getValue(LogisticsCore.resource("liquid_biomass").toIdentifier());
    }

    private static Fluid bioFuel() {
        return BuiltInRegistries.FLUID.getValue(LogisticsCore.resource("bio_fuel").toIdentifier());
    }

    private static RefineryBlockEntity place(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsAutomation.BLOCK.REFINERY);
        RefineryBlockEntity be = context.getBlockEntity(pos, RefineryBlockEntity.class);
        if (be == null) {
            context.fail("Refinery should create RefineryBlockEntity");
        }
        return be;
    }

    /** Fills the buffer to capacity, looping since insert is rate-limited per call by max-input. */
    private static void chargeFully(RefineryBlockEntity be) {
        var energy = be.energyStorage(null);
        long capacity = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_ENERGY_CAPACITY);
        long input = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_MAX_ENERGY_INPUT);
        for (long filled = 0; filled <= capacity; filled += input) {
            energy.insert(input, false);
        }
    }

    @GameTest
    public void testPlacement(GameTestHelper context) {
        place(context, new BlockPos(1, 1, 1));
        context.succeed();
    }

    /**
     * Wiki claim (Usage): "Pipe a fluid into the input tank; the Refinery processes it over time and
     * deposits the product into its output tank, from which a Fluid Extractor Pipe or Pump can draw
     * it." Insertion always targets the input tank and extraction always targets the output tank,
     * on every side — there's no per-face routing to get wrong.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Usage">wiki/Refinery.txt § Usage</a>
     */
    @GameTest
    public void testInsertTargetsInputExtractTargetsOutput(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        RefineryBlockEntity refinery = place(context, pos);

        for (Direction side : Direction.values()) {
            IFluidStorage view = refinery.fluidStorage(side);
            if (view == null) {
                context.fail("Refinery should expose fluid storage from " + side);
                return;
            }
            long inserted = view.insert(SimpleFluidKey.of(liquidBiomass()), FluidUnits.mb(100), true);
            if (inserted != FluidUnits.mb(100)) {
                context.fail("Refinery should accept liquid biomass into its input tank from " + side);
                return;
            }
            // The output tank starts empty, so nothing should be extractable yet from any side.
            long extracted = view.extract(SimpleFluidKey.of(liquidBiomass()), FluidUnits.mb(100), true);
            if (extracted != 0) {
                context.fail("Refinery should not let the input fluid be extracted straight back out from " + side);
                return;
            }
        }
        context.succeed();
    }

    /**
     * Wiki claim (Recipes): "Liquid Biomass (200 mB) -> Bio Fuel (100 mB)." (Power): "Each recipe
     * costs a total of 5,000 RF."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Recipes">wiki/Refinery.txt § Recipes</a>
     */
    @GameTest(maxTicks = 280)
    public void testDistillsLiquidBiomassIntoBioFuel(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        RefineryBlockEntity refinery = place(context, pos);

        IFluidStorage view = refinery.fluidStorage(Direction.UP);
        view.insert(SimpleFluidKey.of(liquidBiomass()), FluidUnits.mb(200), false);
        chargeFully(refinery);
        long filledEnergy = refinery.energyStorage(null).getAmount();

        // 5,000 RF at 20 RF/t -> 250 ticks; 270 leaves setup slack.
        context.runAfterDelay(270, () -> {
            if (refinery.tank().getAmount() != FluidUnits.mb(100)
                    || refinery.tank().getFluidKey().getFluid() != bioFuel()) {
                context.fail("Refinery output tank should hold 100 mB of bio fuel, got: "
                        + refinery.tank().getAmount());
                return;
            }
            long remainingBiomass = view.extract(SimpleFluidKey.of(liquidBiomass()), FluidUnits.mb(1), true);
            if (remainingBiomass != 0) {
                context.fail("Input liquid biomass should be fully consumed (200 mB drained in one cycle)");
                return;
            }
            long spent = filledEnergy - refinery.energyStorage(null).getAmount();
            if (spent != 5_000) {
                context.fail("Distilling liquid biomass should cost exactly 5,000 RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }
}
