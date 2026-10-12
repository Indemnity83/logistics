package com.logistics.gametest.core;

import com.logistics.core.lib.compat.PlantCompat;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@link PlantCompat} against real vanilla plants in a running level: bone meal advances a crop,
 * and berry picking rolls vanilla's harvest drops and resets the plant the way a player's pick does.
 */
public class PlantCompatGameTestBody {

    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos PLANT = SOIL.above();

    public static void testFertilizeAdvancesWheat(GameTestHelper context) {
        context.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState());
        context.setBlock(PLANT, Blocks.WHEAT.defaultBlockState());
        ServerLevel level = context.getLevel();
        BlockPos pos = context.absolutePos(PLANT);

        if (!PlantCompat.canFertilize(level, pos, level.getBlockState(pos))) {
            context.fail("an age-0 wheat crop must be fertilizable");
            return;
        }
        boolean applied = false;
        for (int attempt = 0; attempt < 16 && !applied; attempt++) {
            applied = PlantCompat.fertilize(level, level.getRandom(), pos, level.getBlockState(pos));
        }
        if (!applied) {
            context.fail("fertilize never reported a growth step on wheat");
            return;
        }
        BlockState grown = level.getBlockState(pos);
        if (!grown.is(Blocks.WHEAT) || grown.getValue(CropBlock.AGE) <= 0) {
            context.fail("fertilize must advance the wheat's age, got " + grown);
            return;
        }
        context.succeed();
    }

    public static void testFertilizeRejectsNonBonemealableBlock(GameTestHelper context) {
        context.setBlock(PLANT, Blocks.STONE.defaultBlockState());
        ServerLevel level = context.getLevel();
        BlockPos pos = context.absolutePos(PLANT);
        BlockState stone = level.getBlockState(pos);

        if (PlantCompat.canFertilize(level, pos, stone)) {
            context.fail("stone must not be fertilizable");
            return;
        }
        if (PlantCompat.fertilize(level, level.getRandom(), pos, stone)) {
            context.fail("fertilize must return false for stone");
            return;
        }
        if (!level.getBlockState(pos).is(Blocks.STONE)) {
            context.fail("stone must be left alone, got " + level.getBlockState(pos));
            return;
        }
        context.succeed();
    }

    public static void testHarvestBerriesFromMatureBush(GameTestHelper context) {
        BlockPos pos = placeBush(context, SweetBerryBushBlock.MAX_AGE);
        ServerLevel level = context.getLevel();

        List<ItemStack> drops = PlantCompat.harvestBerries(level, pos, level.getBlockState(pos));

        int berries = drops.stream().filter(s -> s.is(Items.SWEET_BERRIES)).mapToInt(ItemStack::getCount).sum();
        if (berries < 1) {
            context.fail("a mature bush must yield at least one sweet berry, got " + drops);
            return;
        }
        if (drops.stream().anyMatch(s -> !s.is(Items.SWEET_BERRIES))) {
            context.fail("a bush must yield only sweet berries, got " + drops);
            return;
        }
        if (!bushAt(context, 1)) {
            return;
        }
        context.succeed();
    }

    public static void testHarvestBerriesFromImmatureBushDoesNothing(GameTestHelper context) {
        for (int age = 0; age <= 1; age++) {
            BlockPos pos = placeBush(context, age);
            ServerLevel level = context.getLevel();

            List<ItemStack> drops = PlantCompat.harvestBerries(level, pos, level.getBlockState(pos));

            if (!drops.isEmpty()) {
                context.fail("an age-" + age + " bush has nothing to pick, got " + drops);
                return;
            }
            if (!bushAt(context, age)) {
                return;
            }
        }
        context.succeed();
    }

    public static void testHarvestBerriesFromCaveVines(GameTestHelper context) {
        BlockPos ceiling = PLANT.above();
        context.setBlock(ceiling, Blocks.STONE.defaultBlockState());
        context.setBlock(PLANT, Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, true));
        ServerLevel level = context.getLevel();
        BlockPos pos = context.absolutePos(PLANT);

        List<ItemStack> drops = PlantCompat.harvestBerries(level, pos, level.getBlockState(pos));

        if (drops.stream().noneMatch(s -> s.is(Items.GLOW_BERRIES))) {
            context.fail("cave vines with berries must yield glow berries, got " + drops);
            return;
        }
        BlockState picked = level.getBlockState(pos);
        if (!picked.is(Blocks.CAVE_VINES) || picked.getValue(CaveVines.BERRIES)) {
            context.fail("picked cave vines must lose their berries, got " + picked);
            return;
        }
        if (!PlantCompat.harvestBerries(level, pos, level.getBlockState(pos)).isEmpty()) {
            context.fail("picked cave vines have nothing left to pick");
            return;
        }
        context.succeed();
    }

    private static BlockPos placeBush(GameTestHelper context, int age) {
        context.setBlock(SOIL, Blocks.GRASS_BLOCK.defaultBlockState());
        context.setBlock(PLANT, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, age));
        return context.absolutePos(PLANT);
    }

    /** Fails the test and returns false unless the bush stands at {@code age}. */
    private static boolean bushAt(GameTestHelper context, int age) {
        BlockState state = context.getBlockState(PLANT);
        if (!state.is(Blocks.SWEET_BERRY_BUSH) || state.getValue(SweetBerryBushBlock.AGE) != age) {
            context.fail("the bush must be at age " + age + ", got " + state);
            return false;
        }
        return true;
    }
}
