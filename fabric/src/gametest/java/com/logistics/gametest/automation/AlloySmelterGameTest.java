package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.automation.alloysmelter.AlloySmelterBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Gametests for the Alloy Smelter: order-independent dual inputs, RF cost, and both documented
 * recipe families (ore processing with a flux, and alloying two metals together).
 */
public class AlloySmelterGameTest {

    private static final int INPUT_A = AlloySmelterBlockEntity.INPUT_A_SLOT;
    private static final int INPUT_B = AlloySmelterBlockEntity.INPUT_B_SLOT;
    private static final int PRIMARY = AlloySmelterBlockEntity.PRIMARY_OUTPUT_SLOT;

    private static AlloySmelterBlockEntity place(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsAutomation.BLOCK.ALLOY_SMELTER);
        AlloySmelterBlockEntity be = context.getBlockEntity(pos, AlloySmelterBlockEntity.class);
        if (be == null) {
            context.fail("Alloy Smelter should create AlloySmelterBlockEntity");
        }
        return be;
    }

    private static void chargeFully(AlloySmelterBlockEntity be) {
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
     * Wiki claim (Usage): "The two inputs are order-independent." Both (ore, sand) and (sand, ore)
     * must start drawing energy — i.e. both resolve to the same recipe.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Usage">wiki/Alloy Smelter.txt § Usage</a>
     */
    @GameTest(maxTicks = 40)
    public void testInputsAreOrderIndependent(GameTestHelper context) {
        BlockPos posA = new BlockPos(1, 1, 1);
        AlloySmelterBlockEntity forwardOrder = place(context, posA);
        chargeFully(forwardOrder);
        forwardOrder.setItem(INPUT_A, new ItemStack(Items.IRON_ORE));
        forwardOrder.setItem(INPUT_B, new ItemStack(Items.SAND));

        BlockPos posB = new BlockPos(3, 1, 1);
        AlloySmelterBlockEntity reverseOrder = place(context, posB);
        chargeFully(reverseOrder);
        reverseOrder.setItem(INPUT_A, new ItemStack(Items.SAND));
        reverseOrder.setItem(INPUT_B, new ItemStack(Items.IRON_ORE));

        long forwardStart = forwardOrder.energyStorage(null).getAmount();
        long reverseStart = reverseOrder.energyStorage(null).getAmount();

        context.runAfterDelay(10, () -> {
            if (forwardOrder.energyStorage(null).getAmount() >= forwardStart) {
                context.fail("Ore-then-sand should have started smelting and drawing energy");
                return;
            }
            if (reverseOrder.energyStorage(null).getAmount() >= reverseStart) {
                context.fail("Sand-then-ore should also resolve the same recipe and draw energy");
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Ore processing): "Iron Ore;Deepslate Iron Ore + Sand;Red Sand -> Iron Ingot,2,
     * Byproduct: Rich Slag, 5% chance." (Power): "Each recipe carries its own RF cost."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Ore_processing">wiki/Alloy Smelter.txt § Ore processing</a>
     */
    @GameTest(maxTicks = 220)
    public void testSmeltsIronOreWithSandFlux(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        AlloySmelterBlockEntity smelter = place(context, pos);
        chargeFully(smelter);
        long filledEnergy = smelter.energyStorage(null).getAmount();
        smelter.setItem(INPUT_A, new ItemStack(Items.IRON_ORE));
        smelter.setItem(INPUT_B, new ItemStack(Items.SAND));

        // iron_from_ore.json costs 4,000 RF at 20 RF/t -> 200 ticks; 210 leaves setup slack.
        context.runAfterDelay(210, () -> {
            if (!smelter.getItem(INPUT_A).isEmpty() || !smelter.getItem(INPUT_B).isEmpty()) {
                context.fail("Both inputs should be consumed after smelting");
                return;
            }
            ItemStack output = smelter.getItem(PRIMARY);
            if (!output.is(Items.IRON_INGOT) || output.getCount() != 2) {
                context.fail("Primary output should be 2 iron ingots, got: " + output);
                return;
            }
            long spent = filledEnergy - smelter.energyStorage(null).getAmount();
            if (spent != 4_000) {
                context.fail("Smelting iron ore should cost exactly 4,000 RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }

    /**
     * Wiki claim (Alloying): "Bronze is smelted from three copper and one tin, in ingot...form" —
     * "Copper Ingot,InputCount=3 + Tin Ingot -> Bronze Ingot,4."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Alloying">wiki/Alloy Smelter.txt § Alloying</a>
     */
    @GameTest(maxTicks = 220)
    public void testAlloysCopperAndTinIntoBronze(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        AlloySmelterBlockEntity smelter = place(context, pos);
        chargeFully(smelter);
        long filledEnergy = smelter.energyStorage(null).getAmount();
        smelter.setItem(INPUT_A, new ItemStack(Items.COPPER_INGOT, 3));
        smelter.setItem(INPUT_B, new ItemStack(LogisticsCore.ITEM.TIN_INGOT));

        context.runAfterDelay(210, () -> {
            if (!smelter.getItem(INPUT_A).isEmpty() || !smelter.getItem(INPUT_B).isEmpty()) {
                context.fail("Both inputs (3 copper, 1 tin) should be consumed after alloying");
                return;
            }
            ItemStack output = smelter.getItem(PRIMARY);
            if (!output.is(LogisticsCore.ITEM.BRONZE_INGOT) || output.getCount() != 4) {
                context.fail("Primary output should be 4 bronze ingots, got: " + output);
                return;
            }
            long spent = filledEnergy - smelter.energyStorage(null).getAmount();
            if (spent != 4_000) {
                context.fail("Alloying bronze should cost exactly 4,000 RF, spent: " + spent);
                return;
            }
            context.succeed();
        });
    }
}
