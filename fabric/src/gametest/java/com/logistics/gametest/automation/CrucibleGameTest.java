package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsPower;
import com.logistics.automation.crucible.CrucibleBlockEntity;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.CreativeEngineBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;

/**
 * Gametests for the Crucible: melting an input item into a fluid, RF cost, and the output-only tank
 * (pipes can drain it but never fill it directly).
 */
public class CrucibleGameTest {

    private static final int INPUT_SLOT = 0;

    private static CrucibleBlockEntity place(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsAutomation.BLOCK.CRUCIBLE);
        CrucibleBlockEntity be = context.getBlockEntity(pos, CrucibleBlockEntity.class);
        if (be == null) {
            context.fail("Crucible should create CrucibleBlockEntity");
        }
        return be;
    }

    private static void chargeFully(CrucibleBlockEntity be) {
        var energy = be.energyStorage(null);
        for (int i = 0; i < 1000 && energy.insert(1_000_000L, false) > 0; i++) {
            // keep inserting until the buffer stops accepting
        }
    }

    @GameTest
    public void testPlacement(GameTestHelper context) {
        place(context, new BlockPos(1, 1, 1));
        context.succeed();
    }

    /**
     * Wiki claim (Usage): "...deposits the resulting fluid into its 10,000 mB output-only tank."
     * Pipes can drain the tank but never fill it directly — only the melting recipe does.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crucible#Usage">wiki/Crucible.txt § Usage</a>
     */
    @GameTest
    public void testTankIsOutputOnly(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        CrucibleBlockEntity crucible = place(context, pos);

        IFluidStorage view = crucible.fluidStorage(Direction.UP);
        if (view == null) {
            context.fail("Crucible should expose fluid storage");
            return;
        }
        long inserted = view.insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1_000), false);
        if (inserted != 0) {
            context.fail("Crucible's tank should reject external insertion, accepted: " + inserted);
            return;
        }
        context.succeed();
    }

    /**
     * Wiki claim (Recipes § Lava & water): "Ice -> Water, Amount=1000."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crucible#Lava_.26_water">wiki/Crucible.txt § Lava & water</a>
     */
    @GameTest(maxTicks = 60)
    public void testMeltsIceIntoWater(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        CrucibleBlockEntity crucible = place(context, pos);

        chargeFully(crucible);
        long filledEnergy = crucible.energyStorage(null).getAmount();
        crucible.setItem(INPUT_SLOT, new ItemStack(Items.ICE));

        // ice.json costs 1,600 RF at 40 RF/t -> 40 ticks; 50 leaves setup slack.
        context.runAfterDelay(50, () -> {
            if (!crucible.getItem(INPUT_SLOT).isEmpty()) {
                context.fail("Input ice should be consumed after melting");
                return;
            }
            if (crucible.tank().getAmount() != FluidUnits.mb(1_000)
                    || crucible.tank().getFluidKey().getFluid() != Fluids.WATER) {
                context.fail("Crucible tank should hold 1,000 mB of water, got: " + crucible.tank().getAmount());
                return;
            }
            long spent = filledEnergy - crucible.energyStorage(null).getAmount();
            if (spent != 1_600) {
                context.fail("Melting ice should cost exactly 1,600 RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Usage): "...deposits the resulting fluid into its 10,000 mB output-only tank."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crucible#Usage">wiki/Crucible.txt § Usage</a>
     */
    @GameTest(maxTicks = 100)
    public void testMeltsViaRealEngineAndHopper(GameTestHelper context) {
        BlockPos cruciblePos = new BlockPos(1, 1, 1);
        BlockPos enginePos = new BlockPos(0, 1, 1);
        BlockPos redstoneBlockPos = new BlockPos(-1, 1, 1);
        BlockPos inputHopperPos = cruciblePos.above();

        CrucibleBlockEntity crucible = place(context, cruciblePos);
        context.setBlock(redstoneBlockPos, Blocks.REDSTONE_BLOCK);
        context.setBlock(enginePos, LogisticsPower.BLOCK.CREATIVE_ENGINE
                .defaultBlockState()
                .setValue(AbstractEngineBlock.FACING, Direction.EAST)
                .setValue(AbstractEngineBlock.POWERED, true));
        context.setBlock(inputHopperPos, Blocks.HOPPER);

        CreativeEngineBlockEntity engine = context.getBlockEntity(enginePos, CreativeEngineBlockEntity.class);
        HopperBlockEntity inputHopper = context.getBlockEntity(inputHopperPos, HopperBlockEntity.class);
        if (crucible == null || engine == null || inputHopper == null) {
            context.fail("Expected crucible, engine, and input hopper block entities");
            return;
        }

        // Cycle 20 -> 40 -> 80 -> 160 RF/t, comfortably above the crucible's 128 RF/t input cap.
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();
        engine.cycleOutputLevel();

        inputHopper.setItem(0, new ItemStack(Items.ICE));

        context.succeedWhen(() -> {
            if (crucible.tank().getAmount() < FluidUnits.mb(1_000)
                    || crucible.tank().getFluidKey().getFluid() != Fluids.WATER) {
                throw context.assertionException("Engine-and-hopper-fed crucible should melt ice into water");
            }
        });
    }
}
