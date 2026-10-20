package com.logistics.forestry.farm.arboretum;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.area.PlotLayout;
import com.logistics.core.machine.area.RingArea;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.FarmGeometry;
import com.logistics.forestry.farm.FarmInputs;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.FarmTierProfile;
import com.logistics.forestry.farm.PlotAction;
import com.logistics.forestry.logic.TreeView;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.BitSet;
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

    /** Each tier with its spot count. */
    static Stream<Arguments> tiers() {
        return Stream.of(
                Arguments.of(MachineTier.BASIC, 20),
                Arguments.of(MachineTier.INDUSTRIAL, 48),
                Arguments.of(MachineTier.PRECISION, 84),
                Arguments.of(MachineTier.INFERNAL, 128));
    }

    /** A farm away from the origin, so nothing depends on where it stands. */
    private static final BlockPos FARM = new BlockPos(-123, 64, 457);

    private static RingArea tierRing(MachineTier tier) {
        return FarmGeometry.ring(FARM.above(), FarmTierProfile.defaults(tier).outerSize());
    }

    private Set<BlockPos> spots(PlotLayout region) {
        Set<BlockPos> spots = new HashSet<>();
        BitSet indices = logic().spots(region);
        for (int i = indices.nextSetBit(0); i >= 0; i = indices.nextSetBit(i + 1)) {
            BlockPos plot = region.plot(region.lane(i), region.step(i));
            assertThat(spots.add(plot)).as("spot %s listed once", plot).isTrue();
        }
        return spots;
    }

    private static int chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getZ() - b.getZ()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tiers")
    @DisplayName("every two spots stand at least two apart, diagonals and ring corners included")
    void spotsKeepSpacing(MachineTier tier, int count) {
        List<BlockPos> spots = new ArrayList<>(spots(tierRing(tier)));
        for (int a = 0; a < spots.size(); a++) {
            for (int b = a + 1; b < spots.size(); b++) {
                assertThat(chebyshev(spots.get(a), spots.get(b)))
                        .as("%s and %s", spots.get(a), spots.get(b))
                        .isGreaterThanOrEqualTo(ArboretumLogic.SPOT_SPACING);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tiers")
    @DisplayName("no spot is in lane 0 or the courtyard")
    void spotsSkipLaneZeroAndCourtyard(MachineTier tier, int count) {
        RingArea ring = tierRing(tier);
        for (BlockPos spot : spots(ring)) {
            assertThat(ring.lane(ring.indexOf(spot))).as("lane of %s", spot).isGreaterThan(0);
            assertThat(ring.keepOut().contains(spot)).as("%s in the courtyard", spot).isFalse();
            assertThat(chebyshev(spot, ring.origin()))
                    .as("%s beside the courtyard", spot)
                    .isGreaterThan(FarmGeometry.COURTYARD / 2 + 1);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tiers")
    @DisplayName("each tier has its spot count, and no plot past lane 0 has room for another")
    void spotsFillTheRing(MachineTier tier, int count) {
        RingArea ring = tierRing(tier);
        Set<BlockPos> spots = spots(ring);

        assertThat(spots).hasSize(count);
        for (int i = 0; i < ring.plotCount(); i++) {
            BlockPos plot = ring.plot(i);
            if (ring.lane(i) > 0 && !spots.contains(plot)) {
                assertThat(spots).as("a spot beside %s", plot).anyMatch(spot -> chebyshev(spot, plot) < 2);
            }
        }
    }

    @Test
    @DisplayName("each tier's spots include every spot of the tier below: an upgrade moves no tree")
    void spotsAreLaneStable() {
        List<MachineTier> tiers =
                List.of(MachineTier.BASIC, MachineTier.INDUSTRIAL, MachineTier.PRECISION, MachineTier.INFERNAL);
        for (int i = 1; i < tiers.size(); i++) {
            assertThat(spots(tierRing(tiers.get(i))))
                    .as("%s over %s", tiers.get(i), tiers.get(i - 1))
                    .containsAll(spots(tierRing(tiers.get(i - 1))));
        }
    }

    @Test
    @DisplayName("Basic plants lane 1 on every other step")
    void basicIsEveryOtherStepOfLaneOne() {
        RingArea ring = tierRing(MachineTier.BASIC);
        Set<BlockPos> expected = new HashSet<>();
        for (int step = 0; step < ring.laneLength(1); step += 2) {
            expected.add(ring.plot(1, step));
        }

        assertThat(spots(ring)).isEqualTo(expected);
    }

    @Test
    @DisplayName("spots are the same wherever the farm stands")
    void spotsFollowTheFarm() {
        RingArea here = FarmGeometry.ring(BlockPos.ZERO, 23);
        RingArea there = FarmGeometry.ring(FARM.above(), 23);

        assertThat(logic().spots(here)).isEqualTo(logic().spots(there));
    }

    // ==================== Planting ====================

    /** The spots of the test ring: lane 1, every even step. */
    private static Set<BlockPos> testRingSpots() {
        Set<BlockPos> spots = new HashSet<>();
        for (int step = 0; step < RING.laneLength(1); step += 2) {
            spots.add(cell(1, step));
        }
        return spots;
    }

    @Test
    @DisplayName("saplings go only on the spots, one cell each")
    void saplingsOnTheirSpots() {
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
        assertThat(planted).isEqualTo(testRingSpots());
    }

    @Test
    @DisplayName("2×2 saplings are not germlings, and nothing plants them or any group")
    void no2x2Planting() {
        assertThat(logic().isGermling(new ItemStack(Items.DARK_OAK_SAPLING))).isFalse();
        assertThat(logic().isGermling(new ItemStack(Items.OAK_SAPLING))).isTrue();

        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        for (TestInputs inputs : List.of(
                humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 64)),
                humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 64), new ItemStack(Items.OAK_SAPLING, 64)))) {
            for (int i = 0; i < RING.plotCount(); i++) {
                PlotAction action = logic().decide(world, RING, RING.plot(i), inputs);
                if (action instanceof PlotAction.Plant plant) {
                    assertThat(plant.positions()).containsExactly(RING.plot(i));
                    assertThat(plant.germling().is(Items.DARK_OAK_SAPLING)).isFalse();
                }
            }
        }
        assertThat(decide(world, 1, 0, humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 64))))
                .isInstanceOfSatisfying(
                        PlotAction.Plant.class, plant -> assertThat(plant.germling().isEmpty()).isTrue());
    }

    @Test
    @DisplayName("with no germlings at all, spots ask for one and other plots stay quiet")
    void noGermlings() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs = humusAnd();

        assertThat(decide(world, 1, 2, inputs)).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).containsExactly(cell(1, 2));
            assertThat(plant.germling().isEmpty()).isTrue();
        });
        assertThat(decide(world, 1, 1, inputs)).isEqualTo(PlotAction.NONE);
        assertThat(decide(world, 0, 2, inputs)).isEqualTo(PlotAction.NONE);
    }

    @Test
    @DisplayName("without its ring a plot stands alone: a spot")
    void withoutAreaPlotStandsAlone() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        BlockPos plot = cell(0, 1);

        PlotAction oak = logic().decide(world, plot, humusAnd(new ItemStack(Items.OAK_SAPLING, 4)));
        PlotAction darkOak = logic().decide(world, plot, humusAnd(new ItemStack(Items.DARK_OAK_SAPLING, 4)));

        assertThat(oak).isInstanceOfSatisfying(PlotAction.Plant.class, plant -> {
            assertThat(plant.positions()).containsExactly(plot);
            assertThat(plant.germling().is(Items.OAK_SAPLING)).isTrue();
        });
        assertThat(darkOak)
                .isInstanceOfSatisfying(
                        PlotAction.Plant.class, plant -> assertThat(plant.germling().isEmpty()).isTrue());
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
    @DisplayName("off the spots, a branch hanging over a plot is not felled from it; on a spot it is")
    void branchOverhangFelledOnlyFromItsTrunk() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));
        for (int lane = 0; lane < 2; lane++) {
            BlockPos plot = cell(lane, 1);
            world.tree(plot.above(2), NATURAL_LEAVES);
        }

        assertThat(decide(world, 0, 1, inputs)).as("lane 0, off the spots").isNotInstanceOf(PlotAction.Harvest.class);
        assertThat(decide(world, 1, 1, inputs)).as("lane 1, off the spots").isNotInstanceOf(PlotAction.Harvest.class);

        world.tree(cell(1, 2).above(2), NATURAL_LEAVES);
        assertThat(decide(world, 1, 2, inputs)).as("a spot").isInstanceOf(PlotAction.Harvest.class);
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
    @DisplayName("any open plot gets its soil replaced unless it is Humus, on a spot or off it, lane 0 included")
    void replacesUnpreparedSoil() {
        for (BlockState ground : List.of(
                Blocks.AIR.defaultBlockState(),
                Blocks.DIRT.defaultBlockState(),
                Blocks.GRASS_BLOCK.defaultBlockState(),
                Blocks.SAND.defaultBlockState(),
                Blocks.STONE.defaultBlockState())) {
            TestWorld world = TestWorld.floor(RING, ground);
            TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

            for (BlockPos plot : List.of(cell(1, 2), cell(1, 1), cell(0, 2))) {
                assertThat(logic().decide(world, RING, plot, inputs))
                        .as("%s over %s", plot, ground)
                        .isEqualTo(new PlotAction.PlaceSoil(plot.below()));
            }
        }
    }

    @Test
    @DisplayName("Humus is prepared soil even with no soil on hand")
    void humusIsPrepared() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 1, 2, inputs)).isInstanceOf(PlotAction.Plant.class);
    }

    @Test
    @DisplayName("without Humus, dirt still asks for soil, which the farm reports as No soil, and nothing is planted")
    void noSoilAsksForSoil() {
        TestWorld world = TestWorld.floor(RING, Blocks.DIRT.defaultBlockState());
        TestInputs inputs = new TestInputs().germlings(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 1, 2, inputs)).isEqualTo(new PlotAction.PlaceSoil(cell(1, 2).below()));
    }

    @Test
    @DisplayName("sand left by a felled tree is replaced with Humus before planting")
    void sandIsReplacedWithHumus() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        world.set(cell(1, 2).below(), Blocks.SAND.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 1, 2, inputs)).isEqualTo(new PlotAction.PlaceSoil(cell(1, 2).below()));
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
        world.set(cell(1, 2), Blocks.OAK_SAPLING.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 1, 2, inputs)).isEqualTo(PlotAction.NONE);
    }

    // ==================== Growing ====================

    @Test
    @DisplayName("a planted sapling is left to grow on its own")
    void plantedSaplingsAreLeftAlone() {
        TestWorld world = TestWorld.floor(RING, LogisticsForestry.BLOCK.HUMUS.defaultBlockState());
        world.set(cell(1, 2), Blocks.BIRCH_SAPLING.defaultBlockState());
        TestInputs inputs = humusAnd(new ItemStack(Items.OAK_SAPLING, 4));

        assertThat(decide(world, 1, 2, inputs)).isEqualTo(PlotAction.NONE);
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
    }
}
