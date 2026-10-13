package com.logistics.forestry.farm.arboretum;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.area.WorkArea;
import com.logistics.forestry.farm.FarmInputs;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.PlotAction;
import com.logistics.forestry.logic.TreeView;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Arboretum logic")
class ArboretumLogicTest extends MinecraftTestEnvironment {

    private static final BlockState NATURAL_LEAVES =
            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
    private static final BlockState PLAYER_LEAVES =
            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    private ArboretumLogic logic;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private ArboretumLogic logic() {
        if (logic == null) {
            logic = new ArboretumLogic(
                    stack -> stack.getItem() instanceof BlockItem item && item.getBlock() instanceof SaplingBlock,
                    stack -> stack.is(Items.DIRT) || stack.is(LogisticsForestry.BLOCK.HUMUS.asItem()),
                    stack -> stack.is(Items.DARK_OAK_SAPLING),
                    stack -> stack.is(LogisticsForestry.BLOCK.HUMUS.asItem()));
        }
        return logic;
    }

    private static WorkArea area(int size) {
        return WorkArea.inFront(BlockPos.ZERO, Direction.SOUTH, size);
    }

    private static BlockPos cell(WorkArea area, int row, int column) {
        return area.plot(row * area.size() + column);
    }

    private PlotAction decide(TestWorld world, WorkArea area, int row, int column, TestInputs inputs) {
        return logic().decide(world, area, cell(area, row, column), inputs);
    }

    // ==================== Shape ====================

    @Test
    @DisplayName("lays soil and replants from its output, without water")
    void farmShape() {
        assertThat(logic().laysSoil()).isTrue();
        assertThat(logic().replantsFromOutput()).isTrue();
        assertThat(logic().usesWater()).isFalse();
        assertThat(logic().isGermling(new ItemStack(Items.OAK_SAPLING))).isTrue();
        assertThat(logic().isGermling(ItemStack.EMPTY)).isFalse();
        assertThat(logic().isSoil(new ItemStack(LogisticsForestry.BLOCK.HUMUS))).isTrue();
        assertThat(logic().isSoil(new ItemStack(Items.STONE))).isFalse();
    }

    // ==================== Geometry ====================

    @Test
    @DisplayName("single saplings go only where row and column are both even")
    void singlesOnSpacingTwoLattice() {
        WorkArea area = area(9);
        TestWorld world = TestWorld.dirtFloor(area);
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.OAK_SAPLING, 64));

        Set<String> planted = new HashSet<>();
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                PlotAction action = decide(world, area, row, column, inputs);
                if (action instanceof PlotAction.Plant plant) {
                    assertThat(plant.positions()).containsExactly(cell(area, row, column));
                    assertThat(plant.germling().is(Items.OAK_SAPLING)).isTrue();
                    planted.add(row + "," + column);
                } else {
                    assertThat(action).isEqualTo(PlotAction.NONE);
                }
            }
        }
        Set<String> expected = new HashSet<>();
        for (int row = 0; row < 9; row += 2) {
            for (int column = 0; column < 9; column += 2) {
                expected.add(row + "," + column);
            }
        }
        assertThat(planted).isEqualTo(expected);
    }

    @Test
    @DisplayName("2×2 saplings go in groups anchored every third row and column, never singly")
    void groupsAnchoredEveryThird() {
        WorkArea area = area(9);
        TestWorld world = TestWorld.dirtFloor(area);
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.DARK_OAK_SAPLING, 64));

        Set<String> anchors = new HashSet<>();
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                PlotAction action = decide(world, area, row, column, inputs);
                if (action instanceof PlotAction.Plant plant) {
                    assertThat(plant.positions())
                            .containsExactly(
                                    cell(area, row, column),
                                    cell(area, row, column + 1),
                                    cell(area, row + 1, column),
                                    cell(area, row + 1, column + 1));
                    assertThat(plant.germling().is(Items.DARK_OAK_SAPLING)).isTrue();
                    anchors.add(row + "," + column);
                } else {
                    assertThat(action).isEqualTo(PlotAction.NONE);
                }
            }
        }
        assertThat(anchors)
                .containsExactlyInAnyOrder("0,0", "0,3", "0,6", "3,0", "3,3", "3,6", "6,0", "6,3", "6,6");
    }

    @Test
    @DisplayName("a group whose far cells would leave the area has no anchor")
    void groupMustFitInsideArea() {
        WorkArea area = area(7);
        TestWorld world = TestWorld.dirtFloor(area);
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.DARK_OAK_SAPLING, 64));

        assertThat(decide(world, area, 3, 3, inputs)).isInstanceOf(PlotAction.Plant.class);
        assertThat(decide(world, area, 6, 0, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(decide(world, area, 0, 6, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(ArboretumLogic.isGroupAnchor(3, 0, 0)).isTrue();
        assertThat(ArboretumLogic.isGroupAnchor(1, 0, 0)).isFalse();
    }

    @Test
    @DisplayName("an anchor prefers a 2×2 group; other lattice points take the single sapling")
    void prefersGroupAtAnchor() {
        WorkArea area = area(9);
        TestWorld world = TestWorld.dirtFloor(area);
        TestInputs inputs = new TestInputs()
                .germlings(new ItemStack(Items.OAK_SAPLING, 64), new ItemStack(Items.DARK_OAK_SAPLING, 4));

        PlotAction atAnchor = decide(world, area, 0, 0, inputs);
        assertThat(atAnchor).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).hasSize(4);
            assertThat(plant.germling().is(Items.DARK_OAK_SAPLING)).isTrue();
        });
        PlotAction atLattice = decide(world, area, 0, 2, inputs);
        assertThat(atLattice).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).containsExactly(cell(area, 0, 2));
            assertThat(plant.germling().is(Items.OAK_SAPLING)).isTrue();
        });
    }

    @Test
    @DisplayName("a blocked group falls back to a single sapling at a lattice anchor")
    void blockedGroupFallsBackToSingle() {
        WorkArea area = area(9);
        TestWorld world = TestWorld.dirtFloor(area);
        world.set(cell(area, 1, 1), Blocks.STONE.defaultBlockState());
        TestInputs inputs = new TestInputs()
                .germlings(new ItemStack(Items.DARK_OAK_SAPLING, 4), new ItemStack(Items.OAK_SAPLING, 4));

        PlotAction action = decide(world, area, 0, 0, inputs);

        assertThat(action).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).containsExactly(cell(area, 0, 0));
            assertThat(plant.germling().is(Items.OAK_SAPLING)).isTrue();
        });
    }

    @Test
    @DisplayName("fewer than four 2×2 saplings ask for the whole group, and never plant one alone")
    void shortGroupAsksForAllFour() {
        WorkArea area = area(9);
        TestWorld world = TestWorld.dirtFloor(area);
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.DARK_OAK_SAPLING, 3));

        assertThat(decide(world, area, 0, 0, inputs))
                .isInstanceOfSatisfying(PlotAction.Plant.class, plant -> assertThat(plant.positions()).hasSize(4));
        assertThat(decide(world, area, 0, 2, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(decide(world, area, 2, 2, inputs)).isEqualTo(PlotAction.NONE);
    }

    @Test
    @DisplayName("with no germlings at all, lattice points ask for one and other cells stay quiet")
    void noGermlings() {
        WorkArea area = area(9);
        TestWorld world = TestWorld.dirtFloor(area);
        TestInputs inputs = new TestInputs();

        assertThat(decide(world, area, 2, 2, inputs)).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).containsExactly(cell(area, 2, 2));
            assertThat(plant.germling().isEmpty()).isTrue();
        });
        assertThat(decide(world, area, 0, 3, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(decide(world, area, 1, 1, inputs)).isEqualTo(PlotAction.NONE);
    }

    @Test
    @DisplayName("without its area a plot is a 1×1 area of its own: singles only")
    void withoutAreaPlotStandsAlone() {
        TestWorld world = TestWorld.dirtFloor(area(3));
        BlockPos plot = cell(area(3), 1, 1);

        PlotAction oak = logic().decide(world, plot, new TestInputs().germlings(new ItemStack(Items.OAK_SAPLING, 4)));
        PlotAction darkOak =
                logic().decide(world, plot, new TestInputs().germlings(new ItemStack(Items.DARK_OAK_SAPLING, 4)));

        assertThat(oak).isInstanceOfSatisfying(
                PlotAction.Plant.class, plant -> assertThat(plant.positions()).containsExactly(plot));
        assertThat(darkOak).isEqualTo(PlotAction.NONE);
    }

    // ==================== Priority ====================

    @Test
    @DisplayName("a grown tree is felled first, even on a cell the pattern never plants")
    void harvestComesFirst() {
        WorkArea area = area(9);
        TestWorld world = TestWorld.dirtFloor(area);
        BlockPos offPattern = cell(area, 1, 1);
        world.tree(offPattern, NATURAL_LEAVES);
        TestInputs inputs = new TestInputs()
                .germlings(new ItemStack(Items.OAK_SAPLING, 4))
                .soil(new ItemStack(LogisticsForestry.BLOCK.HUMUS));

        PlotAction action = decide(world, area, 1, 1, inputs);

        assertThat(action).isInstanceOfSatisfying(PlotAction.Harvest.class, harvest -> {
            assertThat(harvest.blocks()).contains(offPattern, offPattern.above(3));
            assertThat(harvest.blocks().get(harvest.blocks().size() - 1)).isEqualTo(offPattern);
        });
    }

    @Test
    @DisplayName("logs under player-placed leaves are a build, not a tree")
    void playerBuildIsLeftAlone() {
        WorkArea area = area(9);
        TestWorld world = TestWorld.dirtFloor(area);
        world.tree(cell(area, 0, 0), PLAYER_LEAVES);
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, area, 0, 0, inputs)).isEqualTo(PlotAction.NONE);
    }

    @Test
    @DisplayName("soil is laid under a planting cell that cannot hold the sapling, before planting")
    void soilBeforePlanting() {
        WorkArea area = area(3);
        TestWorld world = new TestWorld();
        TestInputs inputs = new TestInputs()
                .germlings(new ItemStack(Items.OAK_SAPLING, 4))
                .soil(new ItemStack(Items.DIRT));

        assertThat(decide(world, area, 0, 0, inputs)).isEqualTo(new PlotAction.PlaceSoil(cell(area, 0, 0).below()));
        // Off the pattern nothing is prepared.
        assertThat(decide(world, area, 0, 1, inputs)).isEqualTo(PlotAction.NONE);
    }

    @Test
    @DisplayName("without soil to lay the farm still asks to plant, and the framework reports it blocked")
    void noSoilStillPlants() {
        WorkArea area = area(3);
        TestWorld world = new TestWorld();
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, area, 0, 0, inputs)).isInstanceOf(PlotAction.Plant.class);
    }

    @Test
    @DisplayName("a 2×2 group lays soil under each of its cells before planting all four")
    void groupLaysSoilUnderEveryCell() {
        WorkArea area = area(3);
        TestWorld world = TestWorld.dirtFloor(area);
        BlockPos bare = cell(area, 1, 1);
        world.set(bare.below(), Blocks.AIR.defaultBlockState());
        TestInputs inputs = new TestInputs()
                .germlings(new ItemStack(Items.DARK_OAK_SAPLING, 4))
                .soil(new ItemStack(Items.DIRT));

        assertThat(decide(world, area, 0, 0, inputs)).isEqualTo(new PlotAction.PlaceSoil(bare.below()));
    }

    @Test
    @DisplayName("Humus next in the soil slots replaces dirt and grass under an empty planting cell")
    void humusUpgrade() {
        WorkArea area = area(3);
        TestWorld world = TestWorld.dirtFloor(area);
        world.set(cell(area, 0, 2).below(), Blocks.GRASS_BLOCK.defaultBlockState());
        world.set(cell(area, 2, 0).below(), LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs humus = new TestInputs()
                .germlings(new ItemStack(Items.OAK_SAPLING, 4))
                .soil(new ItemStack(LogisticsForestry.BLOCK.HUMUS));
        TestInputs dirt = new TestInputs()
                .germlings(new ItemStack(Items.OAK_SAPLING, 4))
                .soil(new ItemStack(Items.DIRT));

        assertThat(decide(world, area, 0, 0, humus)).isEqualTo(new PlotAction.PlaceSoil(cell(area, 0, 0).below()));
        assertThat(decide(world, area, 0, 2, humus)).isEqualTo(new PlotAction.PlaceSoil(cell(area, 0, 2).below()));
        assertThat(decide(world, area, 2, 0, humus)).isInstanceOf(PlotAction.Plant.class);
        assertThat(decide(world, area, 0, 0, dirt)).isInstanceOf(PlotAction.Plant.class);
    }

    @Test
    @DisplayName("soil is never dug out from under a planted sapling")
    void neverDigsUnderSapling() {
        WorkArea area = area(3);
        TestWorld world = TestWorld.dirtFloor(area);
        world.set(cell(area, 0, 0), Blocks.OAK_SAPLING.defaultBlockState());
        TestInputs inputs = new TestInputs()
                .germlings(new ItemStack(Items.OAK_SAPLING, 4))
                .soil(new ItemStack(LogisticsForestry.BLOCK.HUMUS));

        assertThat(decide(world, area, 0, 0, inputs)).isEqualTo(PlotAction.NONE);
    }

    // ==================== Growing ====================

    @Test
    @DisplayName("a planted sapling or 2×2 group is left to grow on its own")
    void plantedSaplingsAreLeftAlone() {
        WorkArea area = area(3);
        TestWorld world = TestWorld.dirtFloor(area);
        world.set(cell(area, 0, 2), Blocks.BIRCH_SAPLING.defaultBlockState());
        for (BlockPos cell : ArboretumLogic.group(area, 0, 0)) {
            world.set(cell, Blocks.DARK_OAK_SAPLING.defaultBlockState());
        }
        TestInputs inputs = new TestInputs()
                .germlings(new ItemStack(Items.OAK_SAPLING, 4))
                .soil(new ItemStack(LogisticsForestry.BLOCK.HUMUS));

        assertThat(decide(world, area, 0, 2, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(decide(world, area, 0, 0, inputs)).isEqualTo(PlotAction.NONE);
    }

    // ==================== Fakes ====================

    /** Air unless set; saplings survive on dirt, grass and Humus. */
    private static final class TestWorld implements FarmWorld {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();

        static TestWorld dirtFloor(WorkArea area) {
            TestWorld world = new TestWorld();
            for (int i = 0; i < area.plotCount(); i++) {
                world.set(area.plot(i).below(), Blocks.DIRT.defaultBlockState());
            }
            return world;
        }

        void set(BlockPos pos, BlockState state) {
            blocks.put(pos.immutable(), state);
        }

        /** A four-log trunk at {@code base} with a ring of {@code leaves} at its top. */
        void tree(BlockPos base, BlockState leaves) {
            for (int dy = 0; dy < 4; dy++) {
                set(base.above(dy), Blocks.OAK_LOG.defaultBlockState());
            }
            BlockPos top = base.above(3);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                set(top.relative(side), leaves);
            }
            set(top.above(), leaves);
        }

        @Override
        public boolean isLoaded(BlockPos pos) {
            return true;
        }

        @Override
        public BlockState state(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public int light(BlockPos pos) {
            return 15;
        }

        @Override
        public boolean canSurvive(BlockState state, BlockPos pos) {
            BlockState ground = state(pos.below());
            return ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK) || ground.is(LogisticsForestry.BLOCK.HUMUS);
        }

        @Override
        public boolean isReplaceableSoil(BlockPos pos) {
            return false;
        }

        /** Tags are not bound in unit tests, so trees are classified by block. */
        @Override
        public TreeView treeView() {
            return pos -> {
                BlockState state = state(pos);
                if (state.is(Blocks.OAK_LOG)) {
                    return TreeView.Kind.WOOD;
                }
                if (state.is(Blocks.OAK_LEAVES) && !state.getValue(LeavesBlock.PERSISTENT)) {
                    return TreeView.Kind.CANOPY;
                }
                return TreeView.Kind.OTHER;
            };
        }
    }

    private static final class TestInputs implements FarmInputs {
        private final List<ItemStack> germlings = new ArrayList<>();
        private ItemStack soil = ItemStack.EMPTY;

        TestInputs germlings(ItemStack... stacks) {
            germlings.addAll(List.of(stacks));
            return this;
        }

        TestInputs soil(ItemStack stack) {
            soil = stack;
            return this;
        }

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
            return !soil.isEmpty();
        }

        @Override
        public ItemStack soil() {
            return soil.copyWithCount(1);
        }

        @Override
        public boolean hasWater() {
            return false;
        }
    }
}
