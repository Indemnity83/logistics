package com.logistics.gametest.automation;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import com.logistics.automation.fabricator.SequentialFabricatorBlockEntity;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Gametests for the Sequential Fabricator: building a selected chipset from its material pool, RF
 * cost, and the round-robin multi-selection queue (the machine can have more than one chipset
 * queued at once — see {@code testCyclesThroughMultipleSelectedChipsets}).
 */
public class SequentialFabricatorGameTest {

    private static SequentialFabricatorBlockEntity place(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsAutomation.BLOCK.SEQUENTIAL_FABRICATOR);
        SequentialFabricatorBlockEntity be = context.getBlockEntity(pos, SequentialFabricatorBlockEntity.class);
        if (be == null) {
            context.fail("Sequential Fabricator should create SequentialFabricatorBlockEntity");
        }
        return be;
    }

    private static void chargeFully(SequentialFabricatorBlockEntity be) {
        var energy = be.energyStorage(null);
        for (int i = 0; i < 1000 && energy.insert(1_000_000L, false) > 0; i++) {
            // keep inserting until the buffer stops accepting
        }
    }

    private static ResourceId chipset(String path) {
        return ResourceId.in(LogisticsMod.MOD_ID, path);
    }

    @GameTest
    public void testPlacement(GameTestHelper context) {
        place(context, new BlockPos(1, 1, 1));
        context.succeed();
    }

    /**
     * Wiki claim (Usage): "Choose the chipset to build from the machine's GUI, then feed it the
     * ingredients; the Sequential Fabricator consumes them in order and produces the selected
     * chipset." (Recipes): "Redstone -> Redstone Chipset, 10,000 RF."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Usage">wiki/Sequential Fabricator.txt § Usage</a>
     */
    @GameTest(maxTicks = 140)
    public void testBuildsSelectedChipset(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos chestPos = pos.below();
        context.setBlock(chestPos, Blocks.CHEST);
        SequentialFabricatorBlockEntity fabricator = place(context, pos);

        fabricator.toggleSelection(chipset("fabricator/redstone_chipset"));
        fabricator.setItem(0, new ItemStack(Items.REDSTONE));
        chargeFully(fabricator);
        long filledEnergy = fabricator.energyStorage(null).getAmount();

        // redstone_chipset.json costs 10,000 RF at 80 RF/t -> 125 ticks; 135 leaves setup slack.
        context.runAfterDelay(135, () -> {
            long spent = filledEnergy - fabricator.energyStorage(null).getAmount();
            if (spent != 10_000) {
                context.fail("Building a redstone chipset should cost exactly 10,000 RF, spent: " + spent);
                return;
            }
            context.succeedWhen(() -> context.assertContainerContains(chestPos, LogisticsCore.ITEM.REDSTONE_CHIPSET));
        });
    }

    /**
     * NOTE: the wiki's Usage section reads as one selection at a time ("the selected chipset"), but
     * the machine actually supports queuing several chipsets simultaneously and cycles through them
     * round-robin, building whichever queued recipe currently has materials available (see
     * FabricatorProcessorComponent's own Javadoc). See WIKI_DISCREPANCIES.md § Sequential Fabricator.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Usage">wiki/Sequential Fabricator.txt § Usage</a>
     */
    @GameTest(maxTicks = 350)
    public void testCyclesThroughMultipleSelectedChipsets(GameTestHelper context) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos chestPos = pos.below();
        context.setBlock(chestPos, Blocks.CHEST);
        SequentialFabricatorBlockEntity fabricator = place(context, pos);

        // Both chipsets queued at once, with materials for both present in the pool up front.
        fabricator.toggleSelection(chipset("fabricator/redstone_chipset"));
        fabricator.toggleSelection(chipset("fabricator/copper_chipset"));
        fabricator.setItem(0, new ItemStack(Items.REDSTONE, 2));
        fabricator.setItem(1, new ItemStack(Items.COPPER_INGOT));
        chargeFully(fabricator);

        // 10,000 RF (redstone) + 15,000 RF (copper) at 80 RF/t -> 313 ticks total; 340 leaves slack.
        context.runAfterDelay(340, () -> context.succeedWhen(() -> {
            context.assertContainerContains(chestPos, LogisticsCore.ITEM.REDSTONE_CHIPSET);
            context.assertContainerContains(chestPos, LogisticsCore.ITEM.COPPER_CHIPSET);
        }));
    }
}
