package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.area.Columns;
import com.logistics.core.machine.area.RingArea;
import com.logistics.forestry.logic.TreeView;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Farm courtyard guard")
class CourtyardGuardTest extends MinecraftTestEnvironment {

    private static final long RF = 10_000;
    private static final long WATER_MB = 4_000;
    private static final int GERMLINGS = 8;
    private static final int SOIL = 8;
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState NATURAL_LEAVES =
            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
    private static final BlockState PLAYER_LEAVES =
            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    private static final BlockState DRY_FARMLAND =
            Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 0);

    /** Courtyard columns: centre, an edge and a corner of the 7×7, at Ys from deep below to far above the farm. */
    static Stream<BlockPos> courtyardPositions() {
        List<BlockPos> positions = new ArrayList<>();
        for (int y : new int[] {-40, -1, 0, 1, 2, 9, 120}) {
            positions.add(new BlockPos(0, y, 0));
            positions.add(new BlockPos(-3, y, 1));
            positions.add(new BlockPos(3, y, -3));
        }
        return positions.stream();
    }

    /** What stands at a target before the action, and the action aimed at it. */
    private record Case(String name, BlockState before, Function<BlockPos, PlotAction> action) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<Case> CASES = List.of(
            new Case("place soil", STONE, PlotAction.PlaceSoil::new),
            new Case("till", Blocks.DIRT.defaultBlockState(), pos -> new PlotAction.Till(pos, DRY_FARMLAND)),
            new Case("plant", Blocks.AIR.defaultBlockState(),
                    pos -> PlotAction.plant(pos, new ItemStack(Items.OAK_SAPLING))),
            new Case("harvest", Blocks.OAK_LOG.defaultBlockState(), PlotAction::harvest),
            new Case("harvest player-placed leaves", PLAYER_LEAVES, PlotAction::harvest),
            new Case("harvest planks", Blocks.OAK_PLANKS.defaultBlockState(), PlotAction::harvest));

    static Stream<Arguments> everyActionAtEveryCourtyardPosition() {
        return CASES.stream().flatMap(c -> courtyardPositions().map(pos -> Arguments.of(c, pos)));
    }

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A farm working its real ring, the 7×7 courtyard around it, with every input to spend. */
    private static FarmHarness farm(ScriptedFarmLogic logic) {
        FarmHarness farm = new FarmHarness(logic);
        farm.areaOverride(0, 0);
        farm.items().setItem(FarmMachine.GERMLING_START, new ItemStack(Items.OAK_SAPLING, GERMLINGS));
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.DIRT, SOIL));
        farm.machine.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(WATER_MB), false);
        farm.power(RF);
        return farm;
    }

    private static void assertNothingSpentOrTouched(FarmHarness farm, Columns courtyard) {
        assertThat(farm.energy()).as("RF").isEqualTo(RF);
        assertThat(farm.machine.water().tank().getAmount()).as("water").isEqualTo(FluidUnits.mb(WATER_MB));
        assertThat(farm.items().getItem(FarmMachine.GERMLING_START).getCount()).as("germlings").isEqualTo(GERMLINGS);
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).as("soil").isEqualTo(SOIL);
        assertThat(farm.world.broken).as("broken").isEmpty();
        assertThat(farm.world.writeFlags.keySet()).as("written").noneMatch(courtyard::contains);
    }

    @ParameterizedTest(name = "{0} at {1}")
    @MethodSource("everyActionAtEveryCourtyardPosition")
    @DisplayName("every action aimed into the courtyard, at any height, is refused and charges nothing")
    void actionsIntoTheCourtyardAreRefused(Case c, BlockPos target) {
        ScriptedFarmLogic logic = new ScriptedFarmLogic(true, true).script((world, plot) -> c.action().apply(target));
        FarmHarness farm = farm(logic);
        farm.world.set(target, c.before());
        Columns courtyard = farm.area().keepOut();
        assertThat(courtyard).isEqualTo(Columns.square(BlockPos.ZERO, FarmGeometry.COURTYARD));

        farm.tick();

        assertThat(logic.decisions).isEqualTo(1);
        assertThat(farm.world.blocks.getOrDefault(target, Blocks.AIR.defaultBlockState())).isEqualTo(c.before());
        assertNothingSpentOrTouched(farm, courtyard);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.BLOCKED);
    }

    @Test
    @DisplayName("a felling breaks its own natural leaves hanging into the courtyard, and nothing else there")
    void harvestTakesItsNaturalLeavesInTheCourtyard() {
        RingArea ring = FarmGeometry.ring(BlockPos.ZERO.above(), FarmHarness.PROFILE.outerSize());
        BlockPos plot = ring.plot(0);
        BlockPos leaf = new BlockPos(-3, 4, -3);
        BlockPos playerLeaf = new BlockPos(-3, 4, -2);
        BlockPos log = new BlockPos(-2, 4, -3);
        BlockPos planks = new BlockPos(-3, 5, -3);
        List<BlockPos> blocks = List.of(leaf, playerLeaf, log, planks, plot.above(), plot);
        ScriptedFarmLogic logic = new ScriptedFarmLogic(true, true)
                .script((world, p) -> p.equals(plot) && world.state(plot).is(Blocks.OAK_LOG)
                        ? new PlotAction.Harvest(blocks)
                        : PlotAction.NONE);
        FarmHarness farm = farm(logic);
        farm.world.set(plot.above(), Blocks.OAK_LOG.defaultBlockState());
        farm.world.set(plot, Blocks.OAK_LOG.defaultBlockState());
        farm.world.set(leaf, NATURAL_LEAVES);
        farm.world.set(playerLeaf, PLAYER_LEAVES);
        farm.world.set(log, Blocks.OAK_LOG.defaultBlockState());
        farm.world.set(planks, Blocks.OAK_PLANKS.defaultBlockState());

        farm.ticks(10);

        assertThat(farm.world.broken).containsExactly(leaf, plot.above(), plot);
        assertThat(farm.world.blocks)
                .containsEntry(playerLeaf, PLAYER_LEAVES)
                .containsEntry(log, Blocks.OAK_LOG.defaultBlockState())
                .containsEntry(planks, Blocks.OAK_PLANKS.defaultBlockState());
    }

    @ParameterizedTest(name = "at {0}")
    @MethodSource("courtyardPositions")
    @DisplayName("in the courtyard the guard breaks natural leaves as canopy, and nothing else, by no other path")
    void guardBreaksOnlyNaturalLeavesAsCanopy(BlockPos target) {
        Columns courtyard = Columns.square(BlockPos.ZERO, FarmGeometry.COURTYARD);
        for (BlockState other : List.of(STONE, PLAYER_LEAVES, Blocks.OAK_LOG.defaultBlockState(), Blocks.AIR.defaultBlockState())) {
            FakeFarmWorld world = new FakeFarmWorld();
            world.set(target, other);
            FarmWorldAccess guard = new CourtyardGuard(world, courtyard);

            assertThat(guard.canBreakLeaves(target)).as("%s", other).isFalse();
            assertThat(guard.breakLeaves(target)).as("%s", other).isNull();
            assertThat(world.broken).as("%s", other).isEmpty();
        }

        FakeFarmWorld world = new FakeFarmWorld();
        world.set(target, NATURAL_LEAVES);
        FarmWorldAccess guard = new CourtyardGuard(world, courtyard);

        assertThat(guard.canModify(target)).isFalse();
        assertThat(guard.breakBlock(target)).isNull();
        assertThat(guard.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)).isFalse();
        assertThat(world.broken).isEmpty();
        assertThat(world.writeFlags).isEmpty();

        world.claimed.add(target);
        assertThat(guard.breakLeaves(target)).as("claim protection still holds").isNull();
        world.claimed.clear();

        assertThat(guard.canBreakLeaves(target)).isTrue();
        assertThat(guard.breakLeaves(target)).isNotNull();
        assertThat(world.broken).containsExactly(target);
    }

    @Test
    @DisplayName("the guard's tree view sees natural leaves in the courtyard as canopy, anything else there never touched")
    void guardTreeViewSeesOnlyLeavesInTheCourtyard() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.trees = FakeFarmWorld.OAK_TREES;
        BlockPos leaf = new BlockPos(3, 4, 0);
        BlockPos log = new BlockPos(3, 3, 0);
        world.set(leaf, NATURAL_LEAVES.setValue(LeavesBlock.DISTANCE, 2));
        world.set(log, Blocks.OAK_LOG.defaultBlockState());
        TreeView view = new CourtyardGuard(world, Columns.square(BlockPos.ZERO, FarmGeometry.COURTYARD)).treeView();

        assertThat(view.kind(leaf)).isEqualTo(TreeView.Kind.CANOPY);
        assertThat(view.isLeaves(leaf)).isTrue();
        assertThat(view.kind(log)).isEqualTo(TreeView.Kind.NEVER_TOUCH);
    }

    @Test
    @DisplayName("a group reaching into the courtyard is planted nowhere, its ring cell included")
    void groupReachingIntoTheCourtyardIsRefused() {
        BlockPos inside = new BlockPos(-3, 1, -3);
        ScriptedFarmLogic logic = new ScriptedFarmLogic(true, true)
                .script((world, plot) -> new PlotAction.Plant(List.of(plot, inside), new ItemStack(Items.OAK_SAPLING)));
        FarmHarness farm = farm(logic);

        farm.tick();

        assertThat(farm.world.blocks).isEmpty();
        assertNothingSpentOrTouched(farm, farm.area().keepOut());
    }

    @Test
    @DisplayName("a felling that reaches into the courtyard breaks only what lies outside it")
    void harvestSkipsTheCourtyard() {
        RingArea ring = FarmGeometry.ring(BlockPos.ZERO.above(), FarmHarness.PROFILE.outerSize());
        BlockPos plot = ring.plot(0);
        BlockPos inside = new BlockPos(-3, 3, -3);
        List<BlockPos> blocks = List.of(plot.above(2), inside, plot.above(), plot);
        ScriptedFarmLogic logic = new ScriptedFarmLogic(true, true)
                .script((world, p) -> p.equals(plot) && world.state(plot).is(Blocks.OAK_LOG)
                        ? new PlotAction.Harvest(blocks)
                        : PlotAction.NONE);
        FarmHarness farm = farm(logic);
        blocks.forEach(pos -> farm.world.set(pos, Blocks.OAK_LOG.defaultBlockState()));

        farm.ticks(10);

        assertThat(farm.world.broken).containsExactly(plot.above(2), plot.above(), plot);
        assertThat(farm.world.blocks).containsEntry(inside, Blocks.OAK_LOG.defaultBlockState());
        assertThat(farm.energy()).isEqualTo(RF - 3 * FarmHarness.SETTINGS.cost(FarmCost.HARVEST_LOG));
    }

    // ==================== The guard itself: clearing, draining, restoring soil ====================

    @ParameterizedTest(name = "at {0}")
    @MethodSource("courtyardPositions")
    @DisplayName("the guard refuses every write in a courtyard column, the reads still pass through")
    void guardRefusesEveryWrite(BlockPos target) {
        FakeFarmWorld world = new FakeFarmWorld();
        world.set(target, STONE);
        FarmWorldAccess guard = new CourtyardGuard(world, Columns.square(BlockPos.ZERO, FarmGeometry.COURTYARD));

        assertThat(guard.canModify(target)).isFalse();
        assertThat(guard.breakBlock(target)).as("clearing").isNull();
        guard.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL); // draining
        guard.setBlock(target, Blocks.SAND.defaultBlockState(), Block.UPDATE_ALL); // restoring soil
        assertThat(guard.treeView().kind(target)).isEqualTo(TreeView.Kind.NEVER_TOUCH);

        assertThat(world.blocks).containsEntry(target, STONE);
        assertThat(world.broken).isEmpty();
        assertThat(world.writeFlags).isEmpty();
        assertThat(guard.state(target)).isEqualTo(STONE);
    }

    @Test
    @DisplayName("the guard's tree view tells leaves from the one read of their kind")
    void guardTreeViewTellsLeaves() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.trees = FakeFarmWorld.OAK_TREES;
        BlockPos leaf = new BlockPos(FarmGeometry.COURTYARD, 1, 0);
        world.set(leaf, Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(LeavesBlock.DISTANCE, 3));
        TreeView view = new CourtyardGuard(world, Columns.square(BlockPos.ZERO, FarmGeometry.COURTYARD)).treeView();

        assertThat(view.kind(leaf)).isEqualTo(TreeView.Kind.CANOPY);
        assertThat(view.isLeaves(leaf)).isTrue();
        assertThat(world.reads).containsExactly(leaf);
    }

    @Test
    @DisplayName("outside the courtyard the guard passes writes through")
    void guardPassesTheRingThrough() {
        FakeFarmWorld world = new FakeFarmWorld();
        BlockPos ring = new BlockPos(4, 1, 0);
        world.set(ring, STONE);
        FarmWorldAccess guard = new CourtyardGuard(world, Columns.square(BlockPos.ZERO, FarmGeometry.COURTYARD));

        assertThat(guard.canModify(ring)).isTrue();
        assertThat(guard.breakBlock(ring)).isNotNull();
        guard.setBlock(ring, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);

        assertThat(world.broken).containsExactly(ring);
        assertThat(world.blocks).containsEntry(ring, Blocks.DIRT.defaultBlockState());
        world.claimed.add(ring);
        assertThat(guard.canModify(ring)).as("claim protection still applies").isFalse();
    }

    @Test
    @DisplayName("the guard never sweeps items lying in the courtyard")
    void guardKeepsCourtyardItems() {
        FakeFarmWorld world = new FakeFarmWorld();
        world.drop(new BlockPos(2, 1, 2), new ItemStack(Items.APPLE));
        world.drop(new BlockPos(5, 1, 2), new ItemStack(Items.CARROT));
        FarmWorldAccess guard = new CourtyardGuard(world, Columns.square(BlockPos.ZERO, FarmGeometry.COURTYARD));

        List<ItemStack> swept = guard.collectItems(new AABB(-10, -10, -10, 10, 10, 10), pos -> true);

        assertThat(swept).singleElement().satisfies(stack -> assertThat(stack.is(Items.CARROT)).isTrue());
        assertThat(world.loose).singleElement().satisfies(item -> assertThat(item.stack().is(Items.APPLE)).isTrue());
    }
}
