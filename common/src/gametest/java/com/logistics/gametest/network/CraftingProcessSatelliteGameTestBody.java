package com.logistics.gametest.network;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.block.entity.PowerJunctionBlockEntity;
import com.logistics.pipe.modules.CraftingModule;
import com.logistics.pipe.modules.ProcessModule;
import com.logistics.pipe.modules.RequesterModule;
import com.logistics.pipe.modules.SatelliteModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Shared in-world GameTest bodies for the Crafting, Process and Satellite Logistics Pipes — the
 * pipes that make things on demand rather than only moving stock. Every test drives a real
 * provider → producer → requester network and asserts on what a real chest received.
 */
public class CraftingProcessSatelliteGameTestBody {

    /** Place a filled Power Junction so the network can pay module energy costs. */
    private static void placeChargedPowerJunction(GameTestHelper context, BlockPos pos) {
        context.setBlock(pos, LogisticsPipe.BLOCK.POWER_JUNCTION);
        PowerJunctionBlockEntity junction = context.getBlockEntity(pos, PowerJunctionBlockEntity.class);
        IEnergyStorage es = junction.energyStorage(null);
        long remaining = PowerJunctionBlockEntity.CAPACITY;
        while (remaining > 0) {
            long inserted = es.insert(remaining, false);
            if (inserted <= 0) break;
            remaining -= inserted;
        }
    }

    /** The pipe at {@code pos} and its module of type {@code type}, configured as a player would. */
    private static <T extends Module> T module(GameTestHelper context, BlockPos pos, Class<T> type) {
        PipeBlockEntity entity = context.getBlockEntity(pos, PipeBlockEntity.class);
        if (!(entity.getBlockState().getBlock() instanceof PipeBlock block) || block.getPipe() == null) {
            throw new IllegalStateException("No pipe at " + pos);
        }
        T module = block.getPipe().getModule(type, entity);
        if (module == null) {
            throw new IllegalStateException(type.getSimpleName() + " missing from pipe at " + pos);
        }
        return module;
    }

    private static PipeContext ctx(GameTestHelper context, BlockPos pos) {
        return context.getBlockEntity(pos, PipeBlockEntity.class).createContext();
    }

    private static int count(GameTestHelper context, BlockPos pos, Item item) {
        Container container = (Container) context.getLevel().getBlockEntity(context.absolutePos(pos));
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void requestFrom(GameTestHelper context, BlockPos requesterPos, String itemId, int amount) {
        module(context, requesterPos, RequesterModule.class)
                .setRequestConfig(ctx(context, requesterPos), 0, itemId, amount);
    }

    /** Source chest holding {@code stock}, fed to the network by the provider pipe beside it. */
    private static void placeSource(GameTestHelper context, BlockPos chestPos, BlockPos providerPos, ItemStack stock) {
        context.setBlock(chestPos, Blocks.CHEST);
        context.setBlock(providerPos, LogisticsPipe.BLOCK.PROVIDER_LOGISTICS_PIPE);
        context.getBlockEntity(chestPos, ChestBlockEntity.class).setItem(0, stock);
    }

    /**
     * Configure the crafting pipe at {@code pipePos} with a recipe whose ingredients sit in the
     * given crafter-grid {@code slots}, one {@code ingredient} each.
     */
    private static void configureRecipe(
            GameTestHelper context, BlockPos pipePos, Item ingredient, int[] slots, Item result, int resultCount) {
        CraftingModule crafting = module(context, pipePos, CraftingModule.class);
        PipeContext pipeCtx = ctx(context, pipePos);
        String ingredientId = BuiltInRegistries.ITEM.getKey(ingredient).toString();
        for (int slot : slots) {
            crafting.setIngredient(pipeCtx, slot, ingredientId, 1);
        }
        crafting.setResult(pipeCtx, BuiltInRegistries.ITEM.getKey(result).toString(), resultCount);
    }

    /**
     * Wiki claim (Usage): "The Crafting Logistics Pipe fulfills requests for items that aren't in
     * stock by crafting them. Configure a 3×3 recipe in its GUI; when the output is requested, the
     * pipe pulls the ingredients from providers, crafts, and delivers the result."
     *
     * <p>The network holds one oak log and no planks. A requester asking for 4 oak planks can only
     * be served by the crafting pipe, which must order the log from the provider, craft it in the
     * adjacent Crafter, and deliver the planks to the requester's chest.
     *
     * <p>NOTE: the pipe crafts through an adjacent vanilla Crafter block and advertises nothing
     * without one — a requirement wiki/Crafting Logistics Pipe.txt does not mention. See
     * WIKI_DISCREPANCIES.md.
     *
     * <p>Layout (y=1, crafter north of the crafting pipe at (2,1,0)):
     * <pre>
     * [source chest] [provider] [crafting pipe] [requester] [dest chest]
     *    (0,1,1)       (1,1,1)      (2,1,1)        (3,1,1)     (4,1,1)
     * </pre>
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crafting_Logistics_Pipe#Usage">wiki/Crafting Logistics Pipe.txt § Usage</a>
     */
    public static void testCraftingPipeCraftsRequestedItemFromProvidedIngredients(GameTestHelper context) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos providerPos = new BlockPos(1, 1, 1);
        BlockPos craftingPos = new BlockPos(2, 1, 1);
        BlockPos crafterPos = new BlockPos(2, 1, 0);
        BlockPos requesterPos = new BlockPos(3, 1, 1);
        BlockPos destPos = new BlockPos(4, 1, 1);

        context.setBlock(destPos, Blocks.CHEST);
        context.setBlock(requesterPos, LogisticsPipe.BLOCK.REQUESTER_LOGISTICS_PIPE);
        context.setBlock(crafterPos, Blocks.CRAFTER);
        context.setBlock(craftingPos, LogisticsPipe.BLOCK.CRAFTING_LOGISTICS_PIPE);
        placeSource(context, sourcePos, providerPos, new ItemStack(Items.OAK_LOG));
        placeChargedPowerJunction(context, providerPos.above());

        configureRecipe(context, craftingPos, Items.OAK_LOG, new int[] {0}, Items.OAK_PLANKS, 4);
        requestFrom(context, requesterPos, "minecraft:oak_planks", 4);

        context.succeedWhen(() -> {
            int planks = count(context, destPos, Items.OAK_PLANKS);
            if (planks < 4) {
                throw context.assertionException("Expected 4 crafted oak planks in the dest chest, found " + planks);
            }
            if (count(context, sourcePos, Items.OAK_LOG) != 0) {
                throw context.assertionException("The oak log should have been pulled from the provider to craft");
            }
        });
    }

    /**
     * Wiki claim (Usage): "Chain several to build multi-step crafting trees — one pipe can consume
     * what another produces."
     *
     * <p>The network holds one oak log. A requester asks for 4 oak buttons, which only the second
     * crafting pipe can make — from 4 oak planks it has to order from the first crafting pipe,
     * which in turn crafts them from the provider's log.
     *
     * <p>The second recipe uses its intermediate in a single grid slot on purpose: a recipe that
     * spreads one crafted intermediate across several slots (a crafting table's 2x2 of planks) is
     * refused as "missing ingredients" with exactly enough logs in stock, because the ingredient
     * check rounds each slot up to a whole upstream batch separately.
     *
     * <p>Layout (y=1; crafter A north of crafting pipe A, crafter B south of crafting pipe B):
     * <pre>
     *                              [crafter A]                   [dest chest]
     *                                (2,1,0)                       (4,1,0)
     * [source chest] [provider] [crafting A] [crafting B] [requester]
     *    (0,1,1)       (1,1,1)     (2,1,1)      (3,1,1)      (4,1,1)
     *                                           [crafter B]
     *                                             (3,1,2)
     * </pre>
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crafting_Logistics_Pipe#Usage">wiki/Crafting Logistics Pipe.txt § Usage</a>
     */
    public static void testChainedCraftingPipesConsumeEachOthersOutput(GameTestHelper context) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos providerPos = new BlockPos(1, 1, 1);
        BlockPos planksPipePos = new BlockPos(2, 1, 1);
        BlockPos planksCrafterPos = new BlockPos(2, 1, 0);
        BlockPos buttonPipePos = new BlockPos(3, 1, 1);
        BlockPos buttonCrafterPos = new BlockPos(3, 1, 2);
        BlockPos requesterPos = new BlockPos(4, 1, 1);
        BlockPos destPos = new BlockPos(4, 1, 0);

        context.setBlock(destPos, Blocks.CHEST);
        context.setBlock(requesterPos, LogisticsPipe.BLOCK.REQUESTER_LOGISTICS_PIPE);
        context.setBlock(buttonCrafterPos, Blocks.CRAFTER);
        context.setBlock(buttonPipePos, LogisticsPipe.BLOCK.CRAFTING_LOGISTICS_PIPE);
        context.setBlock(planksCrafterPos, Blocks.CRAFTER);
        context.setBlock(planksPipePos, LogisticsPipe.BLOCK.CRAFTING_LOGISTICS_PIPE);
        placeSource(context, sourcePos, providerPos, new ItemStack(Items.OAK_LOG));
        placeChargedPowerJunction(context, providerPos.above());

        configureRecipe(context, planksPipePos, Items.OAK_LOG, new int[] {0}, Items.OAK_PLANKS, 4);
        configureRecipe(context, buttonPipePos, Items.OAK_PLANKS, new int[] {0}, Items.OAK_BUTTON, 1);
        requestFrom(context, requesterPos, "minecraft:oak_button", 4);

        context.succeedWhen(() -> {
            int buttons = count(context, destPos, Items.OAK_BUTTON);
            if (buttons < 4) {
                throw context.assertionException("Expected 4 buttons crafted from chained planks, found " + buttons);
            }
            if (count(context, sourcePos, Items.OAK_LOG) != 0) {
                throw context.assertionException("The oak log at the bottom of the tree should have been consumed");
            }
        });
    }

    /**
     * Wiki claim (Process Logistics Pipe § Usage): "It feeds raw materials from the network into
     * the machine's input and extracts finished output back into the network, mapping input/output
     * faces [...]. Pair it with providers for materials and requesters to pull results." (Satellite
     * Logistics Pipe § Usage): "items addressed to that name are deposited into the adjacent
     * inventory."
     *
     * <p>A blast furnace only accepts its input from the top and only gives up its result from the
     * bottom, so one pipe cannot serve both faces. The process pipe sits underneath to extract, and
     * addresses its input to a satellite sitting on top. The network holds one raw iron; a
     * requester asking for an iron ingot is served only once the raw iron has gone in through the
     * satellite, been smelted, and come back out through the process pipe.
     *
     * <p>NOTE: the satellite is addressed by the number its GUI assigns, not a free-text name. See
     * WIKI_DISCREPANCIES.md.
     *
     * <p>Layout — the main line at y=1, z=1, with the furnace column above the process pipe and
     * the satellite reached by a pipe loop that never touches the furnace's sides. The requester
     * ends the line so its chest is the only way onward from it:
     * <pre>
     *  y=3:                         [satellite] (2,3,1) [pipe] (3,3,1)
     *  y=2:                         [blast furnace] (2,2,1)
     *  y=1: [source chest] [provider] [process] [pipe] [requester]
     *         (0,1,1)        (1,1,1)   (2,1,1)  (3,1,1)  (4,1,1)
     *  loop: (3,1,1) → (3,1,2) → (3,2,2) → (3,3,2) → (3,3,1) → satellite
     *  dest chest north of the requester at (4,1,0)
     * </pre>
     *
     * @see <a href="https://logistics.fandom.com/wiki/Process_Logistics_Pipe#Usage">wiki/Process Logistics Pipe.txt § Usage</a>
     * @see <a href="https://logistics.fandom.com/wiki/Satellite_Logistics_Pipe#Usage">wiki/Satellite Logistics Pipe.txt § Usage</a>
     */
    public static void testProcessPipeSmeltsThroughSatelliteFedFurnace(GameTestHelper context) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos providerPos = new BlockPos(1, 1, 1);
        BlockPos processPos = new BlockPos(2, 1, 1);
        BlockPos furnacePos = new BlockPos(2, 2, 1);
        BlockPos satellitePos = new BlockPos(2, 3, 1);
        BlockPos requesterPos = new BlockPos(4, 1, 1);
        BlockPos destPos = new BlockPos(4, 1, 0);
        BlockPos[] loop = {
            new BlockPos(3, 1, 1), new BlockPos(3, 1, 2), new BlockPos(3, 2, 2), new BlockPos(3, 3, 2), new BlockPos(3, 3, 1)
        };

        context.setBlock(destPos, Blocks.CHEST);
        context.setBlock(requesterPos, LogisticsPipe.BLOCK.REQUESTER_LOGISTICS_PIPE);
        context.setBlock(furnacePos, Blocks.BLAST_FURNACE);
        // Fuel is a precondition of the furnace, not part of what the pipes are claimed to do.
        context.getBlockEntity(furnacePos, BlastFurnaceBlockEntity.class).setItem(1, new ItemStack(Items.COAL));
        context.setBlock(processPos, LogisticsPipe.BLOCK.PROCESS_LOGISTICS_PIPE);
        context.setBlock(satellitePos, LogisticsPipe.BLOCK.SATELLITE_LOGISTICS_PIPE);
        for (BlockPos pos : loop) {
            context.setBlock(pos, LogisticsPipe.BLOCK.BASIC_LOGISTICS_PIPE);
        }
        placeSource(context, sourcePos, providerPos, new ItemStack(Items.RAW_IRON));
        // South rather than above: above the provider would sit against the furnace's side.
        placeChargedPowerJunction(context, providerPos.south());

        // Satellite 1 is what the satellite GUI's first "+" press selects.
        module(context, satellitePos, SatelliteModule.class).setSatelliteId(ctx(context, satellitePos), 1);
        ProcessModule process = module(context, processPos, ProcessModule.class);
        PipeContext processCtx = ctx(context, processPos);
        process.setInput(processCtx, 0, "minecraft:raw_iron", 1, "");
        process.setOutput(processCtx, 0, "minecraft:iron_ingot", 1);
        process.setInputSatelliteId(processCtx, 1);
        requestFrom(context, requesterPos, "minecraft:iron_ingot", 1);

        context.succeedWhen(() -> {
            int ingots = count(context, destPos, Items.IRON_INGOT);
            if (ingots < 1) {
                throw context.assertionException("Expected the smelted iron ingot in the dest chest, found " + ingots);
            }
            if (count(context, sourcePos, Items.RAW_IRON) != 0) {
                throw context.assertionException("The raw iron should have been pulled from the provider to smelt");
            }
        });
    }
}
