package com.logistics.forestry.farm.arboretum;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.area.RingArea;
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
import java.util.stream.Stream;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Arboretum logic")
class ArboretumLogicTest extends MinecraftTestEnvironment {

    private static final BlockState NATURAL_LEAVES =
            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
    private static final BlockState PLAYER_LEAVES =
            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    /** The test ring: 3×3 inside, 7×7 outside, two lanes of 16 and 24 plots. */
    private static final RingArea RING = new RingArea(BlockPos.ZERO, 3, 7);

    private ArboretumLogic logic;

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private ArboretumLogic logic() {
        if (logic == null) {
            logic = new ArboretumLogic(
                    stack -> stack.getItem() instanceof BlockItem item && item.getBlock() instanceof SaplingBlock,
                    stack -> stack.is(LogisticsForestry.BLOCK.HUMUS.asItem()),
                    stack -> stack.is(Items.DARK_OAK_SAPLING),
                    stack -> stack.is(LogisticsForestry.BLOCK.HUMUS.asItem()),
                    state -> state.getBlock() instanceof SaplingBlock || state.is(Blocks.OAK_LOG));
        }
        return logic;
    }

    private static BlockPos cell(int lane, int step) {
        return RING.plot(lane, step);
    }

    private PlotAction decide(TestWorld world, int lane, int step, TestInputs inputs) {
        return logic().decide(world, RING, cell(lane, step), inputs);
    }

    private static TestInputs humusAnd(ItemStack... germlings) {
        return new TestInputs().germlings(germlings).soil(new ItemStack(LogisticsForestry.BLOCK.HUMUS));
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
        assertThat(logic().isSoil(new ItemStack(Items.DIRT))).isFalse();
    }

    @Test
    @DisplayName("keeps ten blocks clear above each plot")
    void clearance() {
        assertThat(logic().clearanceHeight()).isEqualTo(10);
    }

    // ==================== Pattern ====================

    static Stream<Arguments> rings() {
        return Stream.of(
                Arguments.of(new RingArea(BlockPos.ZERO, 7, 11), 12),
                Arguments.of(new RingArea(BlockPos.ZERO, 9, 13), 12),
                Arguments.of(new RingArea(BlockPos.ZERO, 3, 7), 4),
                Arguments.of(new RingArea(BlockPos.ZERO, 1, 5), 4));
    }

    private static Set<BlockPos> singles(RingArea ring) {
        Set<BlockPos> singles = new HashSet<>();
        for (int i = 0; i < ring.plotCount(); i++) {
            if (ArboretumLogic.isSingleSpot(ring.lane(i), ring.step(i))) {
                singles.add(ring.plot(i));
            }
        }
        return singles;
    }

    private static List<List<BlockPos>> groups(RingArea ring) {
        List<List<BlockPos>> groups = new ArrayList<>();
        for (int i = 0; i < ring.plotCount(); i++) {
            List<BlockPos> group = ArboretumLogic.group(ring, ring.lane(i), ring.step(i));
            if (group != null) {
                assertThat(group.get(0)).as("a group's anchor comes first").isEqualTo(ring.plot(i));
                groups.add(group);
            }
        }
        return groups;
    }

    private static boolean touches(BlockPos a, BlockPos b) {
        return a.getY() == b.getY() && Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ()) == 1;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rings")
    @DisplayName("no two single-sapling spots are orthogonally adjacent")
    void singlesNeverTouch(RingArea ring, int groupCount) {
        Set<BlockPos> singles = singles(ring);
        for (BlockPos single : singles) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                assertThat(singles).as("beside %s", single).doesNotContain(single.relative(side));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rings")
    @DisplayName("at least a third of the plots are single-sapling spots")
    void singlesCoverAThird(RingArea ring, int groupCount) {
        assertThat(singles(ring).size() * 3).isGreaterThanOrEqualTo(ring.plotCount());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rings")
    @DisplayName("every group is an axis-aligned 2×2 square inside the ring, and every corner holds one")
    void groupsAreAlignedSquares(RingArea ring, int groupCount) {
        List<List<BlockPos>> groups = groups(ring);
        assertThat(groups).hasSize(groupCount);
        for (List<BlockPos> group : groups) {
            assertThat(new HashSet<>(group)).as("%s", group).hasSize(4);
            assertThat(group).as("%s", group).allMatch(ring::contains);
            int minX = group.stream().mapToInt(BlockPos::getX).min().orElseThrow();
            int minZ = group.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
            assertThat(new HashSet<>(group))
                    .as("%s", group)
                    .containsExactlyInAnyOrder(
                            new BlockPos(minX, 0, minZ),
                            new BlockPos(minX + 1, 0, minZ),
                            new BlockPos(minX, 0, minZ + 1),
                            new BlockPos(minX + 1, 0, minZ + 1));
        }
        int far = ring.outer() / 2;
        for (BlockPos corner : List.of(
                new BlockPos(-far, 0, -far), new BlockPos(far, 0, -far), new BlockPos(far, 0, far), new BlockPos(-far, 0, far))) {
            assertThat(groups).as("corner %s", corner).anyMatch(group -> group.contains(corner));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rings")
    @DisplayName("groups never overlap and never touch each other")
    void groupsKeepApart(RingArea ring, int groupCount) {
        List<List<BlockPos>> groups = groups(ring);
        for (int a = 0; a < groups.size(); a++) {
            for (int b = a + 1; b < groups.size(); b++) {
                for (BlockPos cellA : groups.get(a)) {
                    for (BlockPos cellB : groups.get(b)) {
                        assertThat(cellA).as("%s and %s", groups.get(a), groups.get(b)).isNotEqualTo(cellB);
                        assertThat(touches(cellA, cellB))
                                .as("%s and %s", groups.get(a), groups.get(b))
                                .isFalse();
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("a ring one lane wide has no room for a group, at the corners or anywhere else")
    void narrowRingHasNoGroups() {
        RingArea narrow = new RingArea(BlockPos.ZERO, 3, 5);

        assertThat(groups(narrow)).isEmpty();
        assertThat(singles(narrow).size() * 3).isGreaterThanOrEqualTo(narrow.plotCount());
    }

    // ==================== Planting ====================

    @Test
    @DisplayName("single saplings go only on the single-sapling spots")
    void singlesOnTheirSpots() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 64));

        Set<BlockPos> planted = new HashSet<>();
        for (int i = 0; i < RING.plotCount(); i++) {
            PlotAction action = logic().decide(world, RING, RING.plot(i), inputs);
            if (action instanceof PlotAction.Plant plant) {
                assertThat(plant.positions()).containsExactly(RING.plot(i));
                assertThat(plant.germling().is(Items.OAK_SAPLING)).isTrue();
                planted.add(RING.plot(i));
            } else {
                assertThat(action).isEqualTo(PlotAction.NONE);
            }
        }
        assertThat(planted).isEqualTo(singles(RING));
    }

    @Test
    @DisplayName("2×2 saplings go only in whole groups, at their anchors, never singly")
    void groupsAtAnchors() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 64));

        List<List<BlockPos>> planted = new ArrayList<>();
        for (int i = 0; i < RING.plotCount(); i++) {
            PlotAction action = logic().decide(world, RING, RING.plot(i), inputs);
            if (action instanceof PlotAction.Plant plant) {
                assertThat(plant.germling().is(Items.DARK_OAK_SAPLING)).isTrue();
                planted.add(plant.positions());
            } else {
                assertThat(action).isEqualTo(PlotAction.NONE);
            }
        }
        assertThat(planted).containsExactlyInAnyOrderElementsOf(groups(RING));
    }

    @Test
    @DisplayName("an anchor prefers a 2×2 group; other single spots take the single sapling")
    void prefersGroupAtAnchor() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs =
                humusAnd(new ItemStack(Items.OAK_SAPLING, 64), new ItemStack(Items.DARK_OAK_SAPLING, 4));

        assertThat(decide(world, 0, 0, inputs)).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).hasSize(4);
            assertThat(plant.germling().is(Items.DARK_OAK_SAPLING)).isTrue();
        });
        assertThat(decide(world, 0, 2, inputs)).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).containsExactly(cell(0, 2));
            assertThat(plant.germling().is(Items.OAK_SAPLING)).isTrue();
        });
    }

    @Test
    @DisplayName("a blocked group falls back to a single sapling at an anchor that is also a single spot")
    void blockedGroupFallsBackToSingle() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        world.set(cell(1, 0), Blocks.STONE.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 4), new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 0, 0, inputs)).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).containsExactly(cell(0, 0));
            assertThat(plant.germling().is(Items.OAK_SAPLING)).isTrue();
        });
    }

    @Test
    @DisplayName("fewer than four 2×2 saplings ask for the whole group, and never plant one alone")
    void shortGroupAsksForAllFour() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 3));

        assertThat(decide(world, 0, 0, inputs))
                .isInstanceOfSatisfying(PlotAction.Plant.class, plant -> assertThat(plant.positions()).hasSize(4));
        assertThat(decide(world, 0, 2, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(decide(world, 1, 2, inputs)).isEqualTo(PlotAction.NONE);
    }

    @Test
    @DisplayName("with no germlings at all, single spots ask for one and other plots stay quiet")
    void noGermlings() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs = humusAnd();

        assertThat(decide(world, 0, 2, inputs)).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).containsExactly(cell(0, 2));
            assertThat(plant.germling().isEmpty()).isTrue();
        });
        assertThat(decide(world, 0, 1, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(decide(world, 1, 3, inputs)).isEqualTo(PlotAction.NONE);
    }

    @Test
    @DisplayName("without its ring a plot stands alone: singles only")
    void withoutAreaPlotStandsAlone() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        BlockPos plot = cell(0, 1);

        PlotAction oak = logic().decide(world, plot, humusAnd(new ItemStack(Items.OAK_SAPLING, 4)));
        PlotAction darkOak = logic().decide(world, plot, humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 4)));

        assertThat(oak).isInstanceOfSatisfying(
                PlotAction.Plant.class, plant -> assertThat(plant.positions()).containsExactly(plot));
        assertThat(darkOak).isEqualTo(PlotAction.NONE);
    }

    // ==================== Priority ====================

    @Test
    @DisplayName("a grown tree is felled first, even on a plot the pattern never plants")
    void harvestComesFirst() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        BlockPos offPattern = cell(0, 1);
        world.tree(offPattern, NATURAL_LEAVES);
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 0, 1, inputs)).isInstanceOfSatisfying(PlotAction.Harvest.class, harvest -> {
            assertThat(harvest.blocks()).contains(offPattern, offPattern.above(3));
            assertThat(harvest.blocks().get(harvest.blocks().size() - 1)).isEqualTo(offPattern);
        });
    }

    @Test
    @DisplayName("logs under player-placed leaves are a build, not a tree: never felled")
    void playerBuildIsNeverFelled() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        world.tree(cell(0, 0), PLAYER_LEAVES);
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 0, 0, inputs)).isEqualTo(PlotAction.NONE);
    }

    // ==================== Soil ====================

    @Test
    @DisplayName("any open plot gets its soil replaced unless it is Humus, on the pattern or off it")
    void replacesUnpreparedSoil() {
        for (BlockState ground : List.of(
                Blocks.AIR.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(),
                Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.SAND.defaultBlockState(),
                Blocks.STONE.defaultBlockState())) {
            TestWorld world = TestWorld.floor(RING, ground);
            TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

            assertThat(decide(world, 0, 2, inputs)).as("over %s", ground).isEqualTo(new PlotAction.PlaceSoil(cell(0, 2).below()));
            assertThat(decide(world, 0, 1, inputs)).as("over %s", ground).isEqualTo(new PlotAction.PlaceSoil(cell(0, 1).below()));
        }
    }

    @Test
    @DisplayName("Humus is prepared soil even with no soil on hand")
    void humusIsPrepared() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 0, 2, inputs)).isInstanceOf(PlotAction.Plant.class);
    }

    @Test
    @DisplayName("without Humus, dirt still asks for soil, which the farm reports as No soil, and nothing is planted")
    void noSoilAsksForSoil() {
        TestWorld world = TestWorld.floor(RING, Blocks.DIRT.defaultBlockState());
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 0, 2, inputs)).isEqualTo(new PlotAction.PlaceSoil(cell(0, 2).below()));
    }

    @Test
    @DisplayName("a 2×2 group lays soil under each of its cells before planting all four")
    void groupLaysSoilUnderEveryCell() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        BlockPos bare = cell(1, 1);
        world.set(bare.below(), Blocks.DIRT.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 4));

        assertThat(decide(world, 0, 0, inputs)).isEqualTo(new PlotAction.PlaceSoil(bare.below()));
    }

    @Test
    @DisplayName("sand left by a felled tree is replaced with Humus before planting")
    void sandIsReplacedWithHumus() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        world.set(cell(0, 2).below(), Blocks.SAND.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 0, 2, inputs)).isEqualTo(new PlotAction.PlaceSoil(cell(0, 2).below()));
    }

    @Test
    @DisplayName("a felled tree turns the Humus under it to sand and leaves other soil alone")
    void soilAfterHarvest() {
        assertThat(logic().soilAfterHarvest(LogisticsForestry.BLOCK.HUMUS.defaultBlockState()))
                .isEqualTo(Blocks.SAND.defaultBlockState());
        assertThat(logic().soilAfterHarvest(Blocks.DIRT.defaultBlockState())).isNull();
        assertThat(logic().soilAfterHarvest(Blocks.SAND.defaultBlockState())).isNull();
    }

    @Test
    @DisplayName("soil is never dug out from under a planted sapling")
    void neverDigsUnderSapling() {
        TestWorld world = TestWorld.floor(RING, Blocks.DIRT.defaultBlockState());
        world.set(cell(0, 2), Blocks.OAK_SAPLING.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 0, 2, inputs)).isEqualTo(PlotAction.NONE);
    }

    // ==================== Growing ====================

    @Test
    @DisplayName("a planted sapling or 2×2 group is left to grow on its own")
    void plantedSaplingsAreLeftAlone() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        world.set(cell(0, 2), Blocks.BIRCH_SAPLING.defaultBlockState());
        for (BlockPos cell : ArboretumLogic.group(RING, 0, 0)) {
            world.set(cell, Blocks.DARK_OAK_SAPLING.defaultBlockState());
        }
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 0, 2, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(decide(world, 0, 0, inputs)).isEqualTo(PlotAction.NONE);
    }

    @Test
    @DisplayName("its own growth is what the injected block set says")
    void ownGrowth() {
        assertThat(logic().isOwnGrowth(Blocks.OAK_SAPLING.defaultBlockState())).isTrue();
        assertThat(logic().isOwnGrowth(Blocks.OAK_LOG.defaultBlockState())).isTrue();
        assertThat(logic().isOwnGrowth(Blocks.STONE.defaultBlockState())).isFalse();
    }

    // ==================== Fakes ====================

    /** Air unless set; saplings survive anywhere. */
    private static final class TestWorld implements FarmWorld {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();

        static TestWorld floor(RingArea ring, BlockState ground) {
            TestWorld world = new TestWorld();
            for (int i = 0; i < ring.plotCount(); i++) {
                world.set(ring.plot(i).below(), ground);
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
            return true;
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
