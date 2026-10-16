package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.machine.area.PlotLayout;
import com.logistics.core.machine.area.RingArea;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The {@link FarmPlan} seam, with a two-region plan no farm ships: the ring between 1×1 and 5×5,
 * lane 0 worked by one logic and lane 1 by another.
 */
@DisplayName("Farm plan seam")
class FarmPlanTest extends MinecraftTestEnvironment {

    private static final BlockState WHEAT = Blocks.WHEAT.defaultBlockState();
    private static final BlockState COBBLE = Blocks.COBBLESTONE.defaultBlockState();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** Lane 0 of {@code ring} to {@code inside}, lane 1 to {@code outside}; each sees only its own lane. */
    private record TwoLanePlan(RingArea ring, FarmLogic inside, FarmLogic outside) implements FarmPlan {
        RingArea insideRegion() {
            return new RingArea(ring.origin(), ring.inner(), ring.inner() + 2);
        }

        RingArea outsideRegion() {
            return new RingArea(ring.origin(), ring.inner() + 2, ring.inner() + 4);
        }

        @Override
        public FarmLogic logicFor(int plotIndex) {
            return ring.lane(plotIndex) == 0 ? inside : outside;
        }

        @Override
        public PlotLayout regionFor(int plotIndex) {
            return ring.lane(plotIndex) == 0 ? insideRegion() : outsideRegion();
        }
    }

    /** Plants the first germling its logic accepts into every empty plot. */
    private static ScriptedFarmLogic planting() {
        ScriptedFarmLogic logic = new ScriptedFarmLogic();
        logic.script((world, plot) -> {
            List<ItemStack> germlings = logic.lastInputs.germlings();
            return world.state(plot).isAir() && !germlings.isEmpty()
                    ? PlotAction.plant(plot, germlings.get(0))
                    : PlotAction.NONE;
        });
        return logic;
    }

    private static FarmHarness farm(ScriptedFarmLogic inside, ScriptedFarmLogic outside) {
        FarmHarness farm = new FarmHarness(inside);
        farm.areaOverride(1, 5);
        farm.farm().setPlanOverride(ring -> new TwoLanePlan(ring, inside, outside));
        farm.power(10_000);
        return farm;
    }

    private static List<BlockPos> lane(RingArea ring, int lane) {
        List<BlockPos> plots = new ArrayList<>();
        for (int step = 0; step < ring.laneLength(lane); step++) {
            plots.add(ring.plot(lane, step));
        }
        return plots;
    }

    @Test
    @DisplayName("each plot is decided by its own region's logic, which sees its own region and accepts its own germlings")
    void routesDecisionsAndInputsByPlot() {
        ScriptedFarmLogic inside = planting();
        inside.germlings = stack -> stack.is(Items.OAK_SAPLING);
        ScriptedFarmLogic outside = planting();
        outside.germlings = stack -> stack.is(Items.WHEAT_SEEDS);
        FarmHarness farm = farm(inside, outside);
        farm.items().setItem(FarmMachine.GERMLING_START, new ItemStack(Items.OAK_SAPLING, 32));
        farm.items().setItem(FarmMachine.GERMLING_START + 1, new ItemStack(Items.WHEAT_SEEDS, 32));
        RingArea ring = farm.area();
        TwoLanePlan plan = new TwoLanePlan(ring, inside, outside);

        farm.ticks(ring.plotCount());

        assertThat(inside.decisions).isEqualTo(8);
        assertThat(outside.decisions).isEqualTo(16);
        assertThat(inside.lastArea).isEqualTo(plan.insideRegion());
        assertThat(outside.lastArea).isEqualTo(plan.outsideRegion());
        assertThat(lane(ring, 0)).allSatisfy(plot ->
                assertThat(farm.world.blocks).containsEntry(plot, Blocks.OAK_SAPLING.defaultBlockState()));
        assertThat(lane(ring, 1)).allSatisfy(plot -> assertThat(farm.world.blocks).containsEntry(plot, WHEAT));
        assertThat(farm.items().getItem(FarmMachine.GERMLING_START).getCount()).isEqualTo(32 - 8);
        assertThat(farm.items().getItem(FarmMachine.GERMLING_START + 1).getCount()).isEqualTo(32 - 16);
    }

    @Test
    @DisplayName("clearance height and own growth come from the plot's own logic")
    void routesClearingByPlot() {
        ScriptedFarmLogic inside = new ScriptedFarmLogic();
        inside.clearanceHeight = 1;
        ScriptedFarmLogic outside = new ScriptedFarmLogic();
        outside.clearanceHeight = 2;
        outside.ownGrowth = state -> state.is(Blocks.COBBLESTONE);
        FarmHarness farm = farm(inside, outside);
        RingArea ring = farm.area();
        BlockPos insidePlot = ring.plot(0, 0);
        BlockPos outsidePlot = ring.plot(1, 0);
        BlockPos outsideTall = ring.plot(1, 1);
        farm.world.set(insidePlot.above(), COBBLE);
        farm.world.set(outsidePlot, COBBLE);
        farm.world.set(outsideTall.above(), Blocks.DIRT.defaultBlockState());

        farm.ticks(ring.plotCount());

        assertThat(farm.world.blocks).as("above the inside lane's one-block clearance").containsEntry(insidePlot.above(), COBBLE);
        assertThat(farm.world.blocks).as("the outside logic's own growth").containsEntry(outsidePlot, COBBLE);
        assertThat(farm.world.broken).containsExactly(outsideTall.above());
    }

    @Test
    @DisplayName("the soil a harvest leaves comes from the harvested plot's logic")
    void routesSoilAfterHarvestByPlot() {
        ScriptedFarmLogic inside = new ScriptedFarmLogic()
                .script((world, plot) -> world.state(plot).is(Blocks.WHEAT) ? PlotAction.harvest(plot) : PlotAction.NONE);
        inside.afterHarvest = soil -> Blocks.PODZOL.defaultBlockState();
        ScriptedFarmLogic outside = new ScriptedFarmLogic()
                .script((world, plot) -> world.state(plot).is(Blocks.WHEAT) ? PlotAction.harvest(plot) : PlotAction.NONE);
        outside.afterHarvest = soil -> Blocks.SAND.defaultBlockState();
        FarmHarness farm = farm(inside, outside);
        RingArea ring = farm.area();
        BlockPos insidePlot = ring.plot(0, 2);
        BlockPos outsidePlot = ring.plot(1, 3);
        for (BlockPos plot : List.of(insidePlot, outsidePlot)) {
            farm.world.set(plot, WHEAT);
            farm.world.set(plot.below(), Blocks.DIRT.defaultBlockState());
        }

        farm.ticks(ring.plotCount());

        assertThat(farm.world.blocks).containsEntry(insidePlot.below(), Blocks.PODZOL.defaultBlockState());
        assertThat(farm.world.blocks).containsEntry(outsidePlot.below(), Blocks.SAND.defaultBlockState());
    }

    @Test
    @DisplayName("without an override the block's one logic works the whole ring as one region")
    void singleLogicPlanByDefault() {
        ScriptedFarmLogic logic = new ScriptedFarmLogic();
        FarmHarness farm = new FarmHarness(logic);
        farm.areaOverride(1, 5);
        farm.power(10_000);

        farm.ticks(24);

        assertThat(logic.decisions).isEqualTo(24);
        assertThat(logic.lastArea).isEqualTo(farm.area());
        SingleLogicPlan plan = new SingleLogicPlan(logic, farm.area());
        for (int i = 0; i < 24; i++) {
            assertThat(plan.logicFor(i)).isSameAs(logic);
            assertThat(plan.regionFor(i)).isEqualTo(farm.area());
        }
    }
}
