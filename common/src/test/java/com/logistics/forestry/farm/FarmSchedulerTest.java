package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.core.machine.area.RingArea;
import com.logistics.core.machine.tier.MachineTier;
import com.logistics.forestry.farm.crop.CropFarmLogic;
import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Farm scheduler: per-tier budgets, lanes, tree planning and the benchmark scenarios")
class FarmSchedulerTest extends MinecraftTestEnvironment {

    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
    private static final BlockState WET_FARMLAND = Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7);
    private static final BlockState DRY_FARMLAND = Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 0);
    private static final BlockState RIPE_WHEAT = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
    private static final BlockState SEEDED_WHEAT = Blocks.WHEAT.defaultBlockState();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    // ==================== Rigs ====================

    /** A farm on the shipped settings, working its tier's own ring, with full buffers. */
    private static FarmHarness shipped(FarmLogic logic, MachineTier tier) {
        return shipped(logic, tier, FarmSettings.defaults());
    }

    private static FarmHarness shipped(FarmLogic logic, MachineTier tier, FarmSettings settings) {
        FarmHarness farm = new FarmHarness(logic, settings);
        farm.areaOverride(0, 0);
        farm.machine.tier().setTier(tier);
        FarmTierProfile profile = farm.settings.profile(tier);
        farm.power(profile.energyCapacity());
        if (farm.machine.water() != null) {
            farm.machine.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(profile.waterCapacityMb()), false);
        }
        return farm;
    }

    private static long waterMb(FarmHarness farm) {
        return FluidUnits.toMillibuckets(farm.machine.water().tank().getAmount());
    }

    private static long actions(FarmHarness farm) {
        return farm.farm().harvestActions() + farm.farm().cultivateActions();
    }

    /**
     * Ticks until {@code done}, asserting every tick stays within the tier's ceilings; returns the
     * ticks taken.
     */
    private static int runUntil(FarmHarness farm, BooleanSupplier done, int maxTicks) {
        for (int tick = 1; tick <= maxTicks; tick++) {
            tickWithinCeilings(farm);
            if (done.getAsBoolean()) {
                return tick;
            }
        }
        throw new AssertionError("not done within " + maxTicks + " ticks");
    }

    private static void tickWithinCeilings(FarmHarness farm) {
        FarmTierProfile profile = farm.settings.profile(farm.machine.tier().tier());
        long actions = actions(farm);
        long reads = farm.world.readCount;
        farm.tick();
        assertThat(actions(farm) - actions)
                .as("actions in one tick")
                .isLessThanOrEqualTo(WorkBudget.actionCeiling(profile.actionsPerSecond()));
        assertThat(farm.world.readCount - reads)
                .as("block reads in one tick")
                .isLessThanOrEqualTo(WorkBudget.readCeiling(profile.checksPerSecond(), farm.settings.readsPerCheck()));
    }

    /** The real Crop Farm decisions; wheat seeds and dirt stand in for the item tags, grass and dirt till. */
    private static final class Cropland implements FarmLogic {
        private final CropFarmLogic crops =
                new CropFarmLogic(state -> state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT));

        @Override
        public int clearanceHeight() {
            return crops.clearanceHeight();
        }

        @Override
        public boolean isOwnGrowth(BlockState state) {
            return crops.isOwnGrowth(state);
        }

        @Override
        public boolean usesWater() {
            return true;
        }

        @Override
        public boolean laysSoil() {
            return true;
        }

        @Override
        public boolean isGermling(ItemStack stack) {
            return stack.is(Items.WHEAT_SEEDS);
        }

        @Override
        public boolean isSoil(ItemStack stack) {
            return stack.is(Items.DIRT);
        }

        @Override
        public boolean replantsFromOutput() {
            return true;
        }

        @Override
        public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
            return crops.decide(world, plot, inputs);
        }

        @Override
        @Nullable
        public BlockState soilAfterHarvest(BlockState soil) {
            return crops.soilAfterHarvest(soil);
        }
    }

    /** Fells any tree it finds through the farm's planner, and tills dry farmland into wet. */
    private static ScriptedFarmLogic treesAndTilling() {
        ScriptedFarmLogic logic = new ScriptedFarmLogic().script((world, plot) -> {
            FellPlan tree = world.fellPlan(plot, TreeFeller.Limits.around(plot, 8));
            if (tree.isFound()) {
                return new PlotAction.Harvest(tree.blocks());
            }
            BlockState soil = world.state(plot.below());
            return soil == DRY_FARMLAND ? new PlotAction.Till(plot.below(), WET_FARMLAND) : PlotAction.NONE;
        });
        logic.ownGrowth = state -> state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_LEAVES);
        return logic;
    }

    /** Five logs on {@code base} and sixty natural leaves, all on its -Z side, clear of a courtyard to +Z. */
    private static List<BlockPos> oak(FakeFarmWorld world, BlockPos base) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int dy = 0; dy < 5; dy++) {
            world.set(base.above(dy), LOG);
            blocks.add(base.above(dy));
        }
        for (int dy = 2; dy <= 4; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        BlockPos pos = base.offset(dx, dy, dz);
                        world.set(pos, LEAVES);
                        blocks.add(pos);
                    }
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            BlockPos pos = base.offset(dx, 5, 0);
            world.set(pos, LEAVES);
            blocks.add(pos);
        }
        return blocks;
    }

    /**
     * A tall tree: a 20-log trunk on {@code base} under eleven layers of a radius-6 diamond of
     * leaves, leaving out the columns {@code skip} rejects.
     */
    private static List<BlockPos> bigTree(FakeFarmWorld world, BlockPos base, java.util.function.Predicate<BlockPos> skip) {
        List<BlockPos> blocks = new ArrayList<>();
        for (int dy = 0; dy < 20; dy++) {
            world.set(base.above(dy), LOG);
            blocks.add(base.above(dy));
        }
        for (int dy = 9; dy < 20; dy++) {
            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    if (Math.abs(dx) + Math.abs(dz) <= 6 && (dx != 0 || dz != 0) && !skip.test(pos)) {
                        world.set(pos, LEAVES);
                        blocks.add(pos);
                    }
                }
            }
        }
        return blocks;
    }

    // ==================== Ceilings ====================

    @ParameterizedTest(name = "{0}")
    @EnumSource(MachineTier.class)
    @DisplayName("no tick exceeds two ticks' worth of actions or reads: busy, right after a long idle, and after a reload")
    void perTickCeilings(MachineTier tier) {
        FarmHarness farm = shipped(new Cropland(), tier);
        RingArea ring = farm.area();
        for (int i = 0; i < ring.plotCount(); i++) {
            farm.world.set(ring.plot(i).below(), WET_FARMLAND);
            farm.world.set(ring.plot(i), SEEDED_WHEAT);
        }
        // A long idle: nothing to do, the buckets as full as they get.
        for (int tick = 0; tick < 2_000; tick++) {
            tickWithinCeilings(farm);
        }
        assertThat(actions(farm)).isZero();

        for (int i = 0; i < ring.plotCount(); i++) {
            farm.world.set(ring.plot(i), RIPE_WHEAT);
        }
        for (int tick = 0; tick < 400; tick++) {
            tickWithinCeilings(farm);
        }
        assertThat(actions(farm)).isPositive();

        FarmHarness reloaded = shipped(new Cropland(), tier);
        reloaded.world.blocks.putAll(farm.world.blocks);
        reloaded.load(farm.save());
        reloaded.power(farm.settings.profile(tier).energyCapacity());
        for (int tick = 0; tick < 400; tick++) {
            tickWithinCeilings(reloaded);
        }
        assertThat(actions(reloaded)).isPositive();
    }

    // ==================== Lanes ====================

    /** A Basic farm felling a ~800-block tree while every other plot's farmland keeps drying out. */
    private static double cultivateShareWhileFelling(double harvestShare) {
        FarmHarness farm =
                shipped(treesAndTilling(), MachineTier.BASIC, FarmSettings.defaults().withHarvestShare(harvestShare));
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        RingArea ring = farm.area();
        BlockPos base = new BlockPos(0, 1, -5);
        List<BlockPos> tree = bigTree(farm.world, base, pos -> ring.keepOut().contains(pos));
        assertThat(tree.size()).isGreaterThan(600);
        for (int i = 0; i < ring.plotCount(); i++) {
            if (!ring.plot(i).equals(base)) {
                farm.world.set(ring.plot(i).below(), DRY_FARMLAND);
            }
        }
        runUntil(farm, () -> farm.farm().isHarvesting(), 4_000);

        long harvest = farm.farm().harvestActions();
        long cultivate = farm.farm().cultivateActions();
        for (int tick = 0; tick < 2_000; tick++) {
            tickWithinCeilings(farm);
            for (int i = 0; i < ring.plotCount(); i++) {
                BlockPos soil = ring.plot(i).below();
                if (farm.world.blocks.get(soil) == WET_FARMLAND) {
                    farm.world.set(soil, DRY_FARMLAND);
                }
            }
            assertThat(farm.farm().isHarvesting()).as("still felling").isTrue();
        }
        long harvested = farm.farm().harvestActions() - harvest;
        long cultivated = farm.farm().cultivateActions() - cultivate;
        assertThat(harvested + cultivated).as("every action of 100 s spent").isEqualTo(400);
        return (double) cultivated / (harvested + cultivated);
    }

    @Test
    @DisplayName("felling a large tree never starves cultivation: half the actions each by default")
    void fairShareByDefault() {
        assertThat(cultivateShareWhileFelling(FarmSettings.DEFAULT_HARVEST_SHARE)).isBetween(0.49, 0.51);
    }

    @Test
    @DisplayName("at the largest harvest share cultivation still gets a quarter of the actions")
    void cultivationKeepsAQuarter() {
        assertThat(cultivateShareWhileFelling(1.0)).isBetween(0.25, 0.26);
    }

    @Test
    @DisplayName("felling alone takes every action: four a second at Basic")
    void harvestTakesTheWholeBudgetAlone() {
        FarmHarness farm = shipped(treesAndTilling(), MachineTier.BASIC);
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        RingArea ring = farm.area();
        bigTree(farm.world, new BlockPos(0, 1, -5), pos -> ring.keepOut().contains(pos));
        runUntil(farm, () -> farm.farm().isHarvesting(), 4_000);

        long before = farm.farm().harvestActions();
        for (int tick = 0; tick < 400; tick++) {
            tickWithinCeilings(farm);
        }

        assertThat(farm.farm().harvestActions() - before).isEqualTo(80);
    }

    @Test
    @DisplayName("cultivation alone takes every action: four a second at Basic")
    void cultivationTakesTheWholeBudgetAlone() {
        FarmHarness farm = shipped(treesAndTilling(), MachineTier.BASIC);
        RingArea ring = farm.area();
        for (int i = 0; i < ring.plotCount(); i++) {
            farm.world.set(ring.plot(i).below(), DRY_FARMLAND);
        }
        farm.ticks(20);

        long before = farm.farm().cultivateActions();
        for (int tick = 0; tick < 400; tick++) {
            tickWithinCeilings(farm);
            for (int i = 0; i < ring.plotCount(); i++) {
                farm.world.set(ring.plot(i).below(), DRY_FARMLAND);
            }
        }

        assertThat(farm.farm().cultivateActions() - before).isEqualTo(80);
        assertThat(farm.farm().harvestActions()).isZero();
    }

    @Test
    @DisplayName("an idle farm draws no RF and backs off: a full pass, then 20, 40, 80, 160, 200 ticks between passes")
    void idleFarmBacksOff() {
        ScriptedFarmLogic logic = new ScriptedFarmLogic();
        FarmHarness farm = shipped(logic, MachineTier.BASIC);
        long energy = farm.energy();
        int plots = farm.area().plotCount();

        List<Integer> gaps = new ArrayList<>();
        int lastDecision = 0;
        int seen = 0;
        for (int tick = 1; tick <= 2_000; tick++) {
            farm.tick();
            if (logic.decisions > seen) {
                if (seen % plots == 0 && seen > 0) {
                    gaps.add(tick - lastDecision - 1);
                }
                seen = logic.decisions;
                lastDecision = tick;
            }
        }

        assertThat(farm.energy()).isEqualTo(energy);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.IDLE);
        assertThat(gaps).startsWith(20, 40, 80, 160, 200, 200);
    }

    // ==================== Tree planning ====================

    @Test
    @DisplayName("a tree is planned over several ticks within the read budget, then felled exactly as planned in one go")
    void plansIncrementally() {
        FarmHarness farm = shipped(treesAndTilling(), MachineTier.BASIC);
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        BlockPos base = new BlockPos(0, 1, -5);
        oak(farm.world, base);
        // What a single uninterrupted plan of the guarded world gives.
        CourtyardGuard guard = new CourtyardGuard(farm.world, farm.area().keepOut());
        FellPlan expected = TreeFeller.plan(guard.treeView(), base, TreeFeller.Limits.around(base, 8));
        assertThat(expected.blocks()).hasSize(65);

        int planningTicks = 0;
        for (int tick = 0; tick < 4_000 && (farm.world.broken.size() < 65); tick++) {
            tickWithinCeilings(farm);
            if (farm.farm().isPlanning()) {
                planningTicks++;
            }
        }

        assertThat(planningTicks).as("planned across ticks").isGreaterThan(1);
        assertThat(farm.world.broken).containsExactlyElementsOf(expected.blocks());
    }

    @Test
    @DisplayName("a planned block changed before it is broken drops the plan; the tree is planned again without it")
    void changedBlockInvalidatesThePlan() {
        FarmHarness farm = shipped(treesAndTilling(), MachineTier.BASIC);
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        BlockPos base = new BlockPos(0, 1, -5);
        List<BlockPos> tree = oak(farm.world, base);
        runUntil(farm, () -> farm.world.broken.size() == 3, 4_000);
        // The last leaf of its layer, so the rest of the layer is felled before the change is met.
        BlockPos changed = base.offset(2, 2, 1);
        assertThat(farm.world.broken).doesNotContain(changed);
        farm.world.set(changed, Blocks.STONE.defaultBlockState());

        runUntil(farm, () -> !farm.farm().isHarvesting() && farm.world.broken.size() >= 64, 4_000);

        assertThat(farm.world.blocks).containsEntry(changed, Blocks.STONE.defaultBlockState());
        assertThat(farm.world.broken).doesNotContain(changed).doesNotHaveDuplicates().hasSize(64);
        tree.stream().filter(pos -> !pos.equals(changed)).forEach(pos -> assertThat(farm.world.blocks).doesNotContainKey(pos));
    }

    @Test
    @DisplayName("a second tree found while one is felled waits: one harvest at a time, both felled in the end")
    void secondTreeIsDeferred() {
        FarmHarness farm = shipped(treesAndTilling(), MachineTier.BASIC);
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        List<BlockPos> first = oak(farm.world, new BlockPos(-3, 1, -5));
        List<BlockPos> second = oak(farm.world, new BlockPos(3, 1, -5)).stream()
                .filter(pos -> !first.contains(pos))
                .toList();

        runUntil(farm, () -> farm.world.broken.size() == first.size() + second.size(), 10_000);

        List<BlockPos> broken = farm.world.broken;
        boolean firstFirst = first.contains(broken.get(0));
        List<BlockPos> early = firstFirst ? first : second;
        int earlyCount = (int) broken.stream().filter(early::contains).count();
        assertThat(broken.subList(0, earlyCount)).as("one tree wholly before the other").allMatch(early::contains);
    }

    // ==================== Benchmark scenarios ====================

    private record Measured(int ticks, long rf, long waterMb) {}

    private static Measured matureCropland(MachineTier tier) {
        FarmHarness farm = shipped(new Cropland(), tier);
        RingArea ring = farm.area();
        for (int i = 0; i < ring.plotCount(); i++) {
            farm.world.set(ring.plot(i).below(), WET_FARMLAND);
            farm.world.set(ring.plot(i), RIPE_WHEAT);
        }
        long energy = farm.energy();
        long water = waterMb(farm);
        int ticks = runUntil(farm, () -> replanted(farm, ring), 10_000);
        return new Measured(ticks, energy - farm.energy(), water - waterMb(farm));
    }

    @Test
    @DisplayName("a harvest leaves each plot's farmland at the moisture it had, and replants on it without a till")
    void harvestKeepsFarmlandMoisture() {
        FarmHarness farm = shipped(new Cropland(), MachineTier.BASIC);
        RingArea ring = farm.area();
        for (int i = 0; i < ring.plotCount(); i++) {
            farm.world.set(ring.plot(i).below(), WET_FARMLAND.setValue(BlockStateProperties.MOISTURE, i % 8));
            farm.world.set(ring.plot(i), RIPE_WHEAT);
        }

        runUntil(farm, () -> replanted(farm, ring), 10_000);

        for (int i = 0; i < ring.plotCount(); i++) {
            assertThat(farm.world.blocks.get(ring.plot(i).below()))
                    .as("plot %s", i)
                    .isEqualTo(WET_FARMLAND.setValue(BlockStateProperties.MOISTURE, i % 8));
        }
    }

    private static boolean replanted(FarmHarness farm, RingArea ring) {
        for (int i = 0; i < ring.plotCount(); i++) {
            BlockPos plot = ring.plot(i);
            if (farm.world.blocks.get(plot) != SEEDED_WHEAT || !farm.world.blocks.get(plot.below()).is(Blocks.FARMLAND)) {
                return false;
            }
        }
        return true;
    }

    @Test
    @DisplayName("mature cropland, Basic: 72 plots harvested and replanted on their farmland in 36 s for 5 400 RF and no water")
    void matureCroplandBasic() {
        Measured measured = matureCropland(MachineTier.BASIC);

        assertThat(measured.rf()).isEqualTo(5_400);
        assertThat(measured.waterMb()).as("working never spends water").isZero();
        // A harvest and a replant a plot, one action per 5 ticks.
        assertThat(measured.ticks()).isEqualTo(720);
    }

    @Test
    @DisplayName("mature cropland, Infernal: 480 plots in 30 s for 36 000 RF")
    void matureCroplandInfernal() {
        Measured measured = matureCropland(MachineTier.INFERNAL);

        assertThat(measured.rf()).isEqualTo(36_000);
        assertThat(measured.waterMb()).isZero();
        assertThat(measured.ticks()).isEqualTo(600);
    }

    @Test
    @DisplayName("first preparation on grass, Basic: half the plots cleared of tall grass, all tilled and planted in ~47 s for 4 500 RF and no water")
    void grassPrepBasic() {
        assertGrassPrep(shipped(new Cropland(), MachineTier.BASIC));
    }

    @Test
    @DisplayName("an empty tank prepares the same field just as fast: tilling and planting never wait for water")
    void grassPrepWithoutWater() {
        FarmHarness farm = shipped(new Cropland(), MachineTier.BASIC);
        farm.machine.water().tank().setContents(SimpleFluidKey.of(Fluids.WATER), 0);

        assertGrassPrep(farm);
    }

    private static void assertGrassPrep(FarmHarness farm) {
        RingArea ring = farm.area();
        for (int i = 0; i < ring.plotCount(); i++) {
            farm.world.set(ring.plot(i).below(), Blocks.GRASS_BLOCK.defaultBlockState());
            if (i % 2 == 0) {
                farm.world.set(ring.plot(i), Blocks.SHORT_GRASS.defaultBlockState());
            }
        }
        farm.items().setItem(FarmMachine.GERMLING_START, new ItemStack(Items.WHEAT_SEEDS, 64));
        farm.items().setItem(FarmMachine.GERMLING_START + 1, new ItemStack(Items.WHEAT_SEEDS, 8));
        long energy = farm.energy();
        long water = waterMb(farm);

        int ticks = runUntil(farm, () -> replanted(farm, ring), 10_000);

        assertThat(energy - farm.energy()).isEqualTo(4_500);
        assertThat(water - waterMb(farm)).isZero();
        // 2.5 actions a plot is 180 actions, one per 5 ticks, plus the scan reaching them.
        assertThat(ticks).isEqualTo(935);
    }

    @Test
    @DisplayName("an oak of 5 logs and 60 leaves on an idle Basic farm: felled in ~16 s for 425 RF")
    void oakFellingBasic() {
        FarmHarness farm = shipped(treesAndTilling(), MachineTier.BASIC);
        farm.world.trees = FakeFarmWorld.OAK_TREES;
        BlockPos base = new BlockPos(0, 1, -5);
        oak(farm.world, base);
        long energy = farm.energy();

        int found = runUntil(farm, () -> farm.farm().isPlanning(), 4_000);
        int felled = runUntil(farm, () -> farm.world.broken.size() == 65, 4_000);

        assertThat(energy - farm.energy()).isEqualTo(425);
        assertThat(felled).as("ticks from finding the tree to its last block").isBetween(292, 358);
        // 64 harvest-lane actions at one per 5 ticks after the first block, plus planning.
        assertThat(felled).isEqualTo(323);
        assertThat(found).as("the scan reaching the tree").isPositive();
    }
}
