package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsPower;
import com.logistics.automation.refinery.RefineryBlockEntity;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

/**
 * Shared Refinery GameTest bodies, compiled directly into both loaders' {@code gametest} source
 * sets (see {@code common/build.gradle}). Loader-specific glue wires these into each loader's own
 * registration mechanism: Fabric's {@code @GameTest}-annotated {@code RefineryGameTest} delegates
 * to these methods, and NeoForge's {@code RefineryGameTestRegistration} references them directly
 * as {@code Consumer<GameTestHelper>} method references.
 */
public class RefineryGameTestBody {

    private static Fluid liquidBiomass() {
        return BuiltInRegistries.FLUID.get(LogisticsCore.resource("liquid_biomass").toIdentifier());
    }

    private static Fluid bioFuel() {
        return BuiltInRegistries.FLUID.get(LogisticsCore.resource("bio_fuel").toIdentifier());
    }

    private static RefineryBlockEntity place(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsAutomation.BLOCK.REFINERY);
        RefineryBlockEntity be = (RefineryBlockEntity) context.getBlockEntity(pos);
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
        if (input <= 0) {
            throw new IllegalStateException("REFINERY_MAX_ENERGY_INPUT must be positive, got: " + input);
        }
        for (long filled = 0; filled < capacity; filled += input) {
            energy.insert(input, false);
        }
    }

    public static void testPlacement(GameTestHelper context) {
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
    public static void testInsertTargetsInputExtractTargetsOutput(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        RefineryBlockEntity refinery = place(context, pos);

        long expectedInputTotal = 0;
        for (Direction side : Direction.values()) {
            IFluidStorage view = refinery.fluidStorage(side);
            if (view == null) {
                context.fail("Refinery should expose fluid storage from " + side);
                return;
            }
            long inserted = view.insert(SimpleFluidKey.of(liquidBiomass()), FluidUnits.mb(100), false);
            if (inserted != FluidUnits.mb(100)) {
                context.fail("Refinery should accept liquid biomass into its input tank from " + side);
                return;
            }
            expectedInputTotal += inserted;

            long actualInputAmount = 0;
            for (IFluidView contained : view.contents()) {
                if (contained.resource().getFluid() == liquidBiomass()) {
                    actualInputAmount = contained.amount();
                }
            }
            if (actualInputAmount != expectedInputTotal) {
                context.fail("Refinery input tank should contain " + expectedInputTotal
                        + " mB of liquid biomass after inserting from " + side + ", got: " + actualInputAmount);
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
    public static void testDistillsLiquidBiomassIntoBioFuel(GameTestHelper context) {
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
            // contents() never yields a zero-amount view, so an empty input tank simply won't appear
            // here — check the view directly rather than through extract(), which only ever routes
            // to the output tank and would trivially return 0 regardless of the input tank's state.
            for (IFluidView contained : view.contents()) {
                if (contained.resource().getFluid() == liquidBiomass()) {
                    context.fail("Input liquid biomass should be fully consumed (200 mB drained in one cycle), got: "
                            + contained.amount());
                    return;
                }
            }
            long spent = filledEnergy - refinery.energyStorage(null).getAmount();
            if (spent != 5_000) {
                context.fail("Distilling liquid biomass should cost exactly 5,000 RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Power): "connect a strong RF source" — each recipe costs a total of 5,000 RF.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Power">wiki/Refinery.txt § Power</a>
     */
    public static void testDistillsViaRealEngine(GameTestHelper context) {
        // z=3 so the engine (z=2) and its redstone block (z=1) both stay inside the test
        // structure. MC 1.21.1's GameTest runner wedges in clearSpaceForStructure when a test
        // places blocks at negative relative coordinates; newer versions tolerate it.
        BlockPos pos = new BlockPos(1, 1, 3);
        BlockPos enginePos = pos.north();
        BlockPos redstoneBlockPos = enginePos.north();

        RefineryBlockEntity refinery = place(context, pos);
        context.setBlock(redstoneBlockPos, Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.SOUTH)
                .setValue(AbstractEngineBlock.POWERED, true));

        CreativeEngineBlockEntity engine = (CreativeEngineBlockEntity) context.getBlockEntity(enginePos);
        if (refinery == null || engine == null) {
            context.fail("Expected refinery and engine block entities");
            return;
        }

        IFluidStorage view = refinery.fluidStorage(Direction.UP);
        view.insert(SimpleFluidKey.of(liquidBiomass()), FluidUnits.mb(200), false);
        // Cycle 20 -> 40 -> 80 -> 160 RF/t, comfortably above the refinery's 128 RF/t input cap.
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();

        context.succeedWhen(() -> context.assertTrue(!(refinery.tank().getAmount() != FluidUnits.mb(100)
                    || refinery.tank().getFluidKey().getFluid() != bioFuel()), "Engine-powered refinery should distill 100 mB of bio fuel"));
    }
}
