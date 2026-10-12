package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.forestry.farm.crop.CropFarmLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Crop Farm logic")
class CropFarmLogicTest extends MinecraftTestEnvironment {

    private static final BlockPos PLOT = new BlockPos(3, 64, 5);
    private static final BlockPos SOIL = PLOT.below();

    /** Stands in for the {@code tillable} tag, which is not bound in unit tests. */
    private final CropFarmLogic logic = new CropFarmLogic(
            state -> state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH));

    private final FakeFarmWorld world = new FakeFarmWorld();
    private final Inputs inputs = new Inputs();

    private static BlockState farmland(int moisture) {
        return Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, moisture);
    }

    private static BlockState wheat(int age) {
        return Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age);
    }

    /** {@link ItemStack} has no value equality, so a plant action is compared field by field. */
    private static void assertPlants(PlotAction action, Item germling) {
        assertThat(action).isInstanceOf(PlotAction.Plant.class);
        PlotAction.Plant plant = (PlotAction.Plant) action;
        assertThat(plant.positions()).containsExactly(PLOT);
        assertThat(plant.germling().getItem()).isEqualTo(germling);
        assertThat(plant.germling().getCount()).isEqualTo(1);
    }

    private PlotAction decide() {
        return logic.decide(world, PLOT, inputs);
    }

    private PlotAction decideInLight(int light) {
        FarmWorld lit = new FarmWorld() {
            @Override
            public boolean isLoaded(BlockPos pos) {
                return world.isLoaded(pos);
            }

            @Override
            public BlockState state(BlockPos pos) {
                return world.state(pos);
            }

            @Override
            public int light(BlockPos pos) {
                return light;
            }

            @Override
            public boolean canSurvive(BlockState state, BlockPos pos) {
                return world.canSurvive(state, pos);
            }

            @Override
            public boolean canFertilize(BlockPos pos) {
                return world.canFertilize(pos);
            }

            @Override
            public boolean isReplaceableSoil(BlockPos pos) {
                return world.isReplaceableSoil(pos);
            }
        };
        return logic.decide(lit, PLOT, inputs);
    }

    // ==================== Shape ====================

    @Test
    @DisplayName("irrigates, lays soil and replants from its own harvest")
    void machineShape() {
        assertThat(logic.usesWater()).isTrue();
        assertThat(logic.laysSoil()).isTrue();
        assertThat(logic.replantsFromOutput()).isTrue();
    }

    @Test
    @DisplayName("plants each seed as its crop block")
    void plantStates() {
        assertThat(logic.plantState(new ItemStack(Items.WHEAT_SEEDS), PLOT, world).getBlock()).isEqualTo(Blocks.WHEAT);
        assertThat(logic.plantState(new ItemStack(Items.CARROT), PLOT, world).getBlock()).isEqualTo(Blocks.CARROTS);
        assertThat(logic.plantState(new ItemStack(Items.POTATO), PLOT, world).getBlock()).isEqualTo(Blocks.POTATOES);
        assertThat(logic.plantState(new ItemStack(Items.BEETROOT_SEEDS), PLOT, world).getBlock())
                .isEqualTo(Blocks.BEETROOTS);
    }

    // ==================== Harvest ====================

    @Test
    @DisplayName("harvests a mature crop first, even on dry farmland")
    void harvestsMatureCropFirst() {
        world.set(PLOT, wheat(CropBlock.MAX_AGE));
        world.set(SOIL, farmland(0));
        world.fertilizable.add(PLOT);
        inputs.fertilizer = true;

        assertThat(decide()).isEqualTo(PlotAction.harvest(PLOT));
    }

    @Test
    @DisplayName("harvests mature beetroots, whose crop has fewer ages")
    void harvestsMatureBeetroot() {
        world.set(PLOT, Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, BeetrootBlock.MAX_AGE));
        world.set(SOIL, farmland(7));

        assertThat(decide()).isEqualTo(PlotAction.harvest(PLOT));
    }

    @Test
    @DisplayName("leaves a mature stem alone: only crop blocks are harvested")
    void leavesStemAlone() {
        world.set(PLOT, Blocks.PUMPKIN_STEM.defaultBlockState().setValue(StemBlock.AGE, StemBlock.MAX_AGE));
        world.set(SOIL, farmland(0));

        assertThat(decide()).isEqualTo(PlotAction.NONE);
    }

    // ==================== Soil ====================

    @Test
    @DisplayName("lays soil where the plot has no ground")
    void laysSoilOverAir() {
        assertThat(decide()).isEqualTo(new PlotAction.PlaceSoil(SOIL));
    }

    @Test
    @DisplayName("tills dirt, grass and dirt path into dry farmland")
    void tillsTillableGround() {
        for (BlockState ground : List.of(
                Blocks.DIRT.defaultBlockState(),
                Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.DIRT_PATH.defaultBlockState())) {
            world.set(SOIL, ground);

            assertThat(decide()).as("over %s", ground).isEqualTo(new PlotAction.Till(SOIL, farmland(0)));
        }
    }

    @Test
    @DisplayName("leaves ground that is not tillable alone")
    void leavesOtherGroundAlone() {
        for (BlockState ground : List.of(
                Blocks.COARSE_DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.SAND.defaultBlockState())) {
            world.set(SOIL, ground);

            assertThat(decide()).as("over %s", ground).isEqualTo(PlotAction.NONE);
        }
    }

    @Test
    @DisplayName("does nothing while the soil below the plot is not loaded")
    void unloadedSoil() {
        world.unloaded.add(SOIL);

        assertThat(decide()).isEqualTo(PlotAction.NONE);
    }

    // ==================== Water ====================

    @Test
    @DisplayName("irrigates dry farmland before planting")
    void irrigatesBeforePlanting() {
        world.set(SOIL, farmland(0));
        inputs.germlings.add(new ItemStack(Items.WHEAT_SEEDS, 4));

        assertThat(decide()).isEqualTo(new PlotAction.Irrigate(SOIL));
    }

    @Test
    @DisplayName("tops up farmland that has started to dry under a growing crop, before fertilizing")
    void irrigatesUnderCropBeforeFertilizing() {
        world.set(PLOT, wheat(3));
        world.set(SOIL, farmland(6));
        world.fertilizable.add(PLOT);
        inputs.fertilizer = true;

        assertThat(decide()).isEqualTo(new PlotAction.Irrigate(SOIL));
    }

    // ==================== Plant ====================

    @Test
    @DisplayName("plants the first germling on wet farmland")
    void plantsOnWetFarmland() {
        world.set(SOIL, farmland(7));
        inputs.germlings.add(new ItemStack(Items.CARROT, 2));
        inputs.germlings.add(new ItemStack(Items.WHEAT_SEEDS, 9));

        assertPlants(decide(), Items.CARROT);
    }

    @Test
    @DisplayName("asks to plant with nothing to plant, which the farm reports as no germlings")
    void plantsWithoutGermlings() {
        world.set(SOIL, farmland(7));

        PlotAction action = decide();

        assertThat(action).isInstanceOf(PlotAction.Plant.class);
        assertThat(((PlotAction.Plant) action).germling().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("reports too dark instead of planting below light 8")
    void tooDarkToPlant() {
        world.set(SOIL, farmland(7));
        inputs.germlings.add(new ItemStack(Items.WHEAT_SEEDS, 4));

        assertThat(decideInLight(CropFarmLogic.MIN_LIGHT - 1)).isEqualTo(PlotAction.blocked(PlotAction.Reason.TOO_DARK));
        assertPlants(decideInLight(CropFarmLogic.MIN_LIGHT), Items.WHEAT_SEEDS);
    }

    @Test
    @DisplayName("still tills and irrigates in the dark; only planting waits for light")
    void darkStillPreparesSoil() {
        world.set(SOIL, Blocks.DIRT.defaultBlockState());
        assertThat(decideInLight(0)).isEqualTo(new PlotAction.Till(SOIL, farmland(0)));

        world.set(SOIL, farmland(0));
        assertThat(decideInLight(0)).isEqualTo(new PlotAction.Irrigate(SOIL));
    }

    // ==================== Fertilize ====================

    @Test
    @DisplayName("fertilizes a growing crop on wet farmland when it has fertilizer")
    void fertilizesGrowingCrop() {
        world.set(PLOT, wheat(2));
        world.set(SOIL, farmland(7));
        world.fertilizable.add(PLOT);
        inputs.fertilizer = true;

        assertThat(decide()).isEqualTo(new PlotAction.Fertilize(PLOT));
    }

    @Test
    @DisplayName("leaves a growing crop to grow on its own without fertilizer")
    void waitsWithoutFertilizer() {
        world.set(PLOT, wheat(2));
        world.set(SOIL, farmland(7));
        world.fertilizable.add(PLOT);

        assertThat(decide()).isEqualTo(PlotAction.NONE);
    }

    // ==================== Obstacles ====================

    @Test
    @DisplayName("leaves a plot with something else on it alone, whatever the ground needs")
    void obstaclesUntouched() {
        inputs.germlings.add(new ItemStack(Items.WHEAT_SEEDS, 4));
        inputs.fertilizer = true;
        for (BlockState obstacle : List.of(
                Blocks.TORCH.defaultBlockState(),
                Blocks.OAK_FENCE.defaultBlockState(),
                Blocks.STONE.defaultBlockState(),
                Blocks.OAK_SAPLING.defaultBlockState())) {
            world.set(PLOT, obstacle);
            for (BlockState ground : List.of(
                    Blocks.AIR.defaultBlockState(), Blocks.DIRT.defaultBlockState(), farmland(0), farmland(7))) {
                world.set(SOIL, ground);

                assertThat(decide()).as("%s over %s", obstacle, ground).isEqualTo(PlotAction.NONE);
            }
        }
    }

    private static final class Inputs implements FarmInputs {
        final List<ItemStack> germlings = new ArrayList<>();
        boolean fertilizer;

        @Override
        public List<ItemStack> germlings() {
            return germlings;
        }

        @Override
        public int germlingCount(ItemStack kind) {
            return germlings.stream()
                    .filter(stack -> ItemStack.isSameItemSameComponents(stack, kind))
                    .mapToInt(ItemStack::getCount)
                    .sum();
        }

        @Override
        public boolean hasSoil() {
            return false;
        }

        @Override
        public boolean hasFertilizer() {
            return fertilizer;
        }
    }
}
