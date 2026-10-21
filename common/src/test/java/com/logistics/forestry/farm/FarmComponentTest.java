package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.machine.area.RingArea;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Farm work loop")
class FarmComponentTest extends MinecraftTestEnvironment {

    private static final BlockState SAPLING = Blocks.OAK_SAPLING.defaultBlockState();
    private static final BlockState WHEAT = Blocks.WHEAT.defaultBlockState();
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static ScriptedFarmLogic plantingEverywhere() {
        return new ScriptedFarmLogic()
                .script((world, plot) -> world.state(plot).isAir()
                        ? PlotAction.plant(plot, new ItemStack(Items.OAK_SAPLING))
                        : PlotAction.NONE);
    }

    private static ScriptedFarmLogic harvestingWheat() {
        return new ScriptedFarmLogic()
                .script((world, plot) -> world.state(plot).is(Blocks.WHEAT) ? PlotAction.harvest(plot) : PlotAction.NONE);
    }

    private static void fillOutput(FarmHarness farm, ItemStack stack) {
        for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END; slot++) {
            farm.items().setItem(slot, stack.copy());
        }
    }

    // ==================== Gates ====================

    @Test
    @DisplayName("without power nothing changes and the farm reports No power")
    void noPowerChangesNothing() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));

        farm.ticks(3);

        assertThat(farm.world.blocks).isEmpty();
        assertThat(farm.items().getItem(0).getCount()).isEqualTo(5);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_POWER);
        assertThat(farm.host.lit()).isFalse();
    }

    @Test
    @DisplayName("planting spends one germling and its RF, and lights the farm")
    void plantsWhenPowered() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(1_000);

        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0), SAPLING);
        assertThat(farm.items().getItem(0).getCount()).isEqualTo(4);
        assertThat(farm.energy()).isEqualTo(1_000 - FarmHarness.SETTINGS.cost(FarmCost.PLANT));
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);
        assertThat(farm.host.lit()).isTrue();
    }

    @Test
    @DisplayName("power that covers only part of an op leaves the op undone")
    void noPartialOps() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic()
                .script((world, plot) -> new PlotAction.Plant(List.of(plot, plot.east()), new ItemStack(Items.OAK_SAPLING))));
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(FarmHarness.SETTINGS.cost(FarmCost.PLANT)); // pays for one sapling, the group needs two

        farm.tick();

        assertThat(farm.world.blocks).isEmpty();
        assertThat(farm.items().getItem(0).getCount()).isEqualTo(5);
        assertThat(farm.energy()).isEqualTo(FarmHarness.SETTINGS.cost(FarmCost.PLANT));
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_POWER);
    }

    @Test
    @DisplayName("with no germlings the farm reports No germlings, and stops as soon as some arrive")
    void noGermlings() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.power(10_000);

        farm.tick();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_GERMLINGS);
        assertThat(farm.world.blocks).isEmpty();

        farm.items().setItem(2, new ItemStack(Items.OAK_SAPLING, 1));
        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(1), SAPLING);
        assertThat(farm.items().getItem(2).isEmpty()).isTrue();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);
    }

    @Test
    @DisplayName("a farm its redstone control disables pauses until it is enabled again")
    void redstonePauses() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(1_000);
        farm.host.workEnabled = false;

        farm.ticks(5);

        assertThat(farm.world.blocks).isEmpty();
        assertThat(farm.logic.decisions).isZero();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.PAUSED);
        assertThat(farm.host.lit()).as("paused, with RF in the buffer").isTrue();

        farm.host.workEnabled = true;
        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0), SAPLING);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);
    }

    @Test
    @DisplayName("an unloaded plot is neither read nor decided on")
    void unloadedPlotIsNeverRead() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(1_000);
        farm.world.unloaded.add(farm.plot(0));

        farm.tick();

        assertThat(farm.logic.decisions).isZero();
        assertThat(farm.world.reads).isEmpty();
        assertThat(farm.world.writeFlags).isEmpty();

        farm.tick();

        assertThat(farm.logic.decisions).isEqualTo(1);
        assertThat(farm.world.blocks).containsOnlyKeys(farm.plot(1));
    }

    @Test
    @DisplayName("a plot claim protection refuses is reported as Blocked and left alone")
    void claimedPlotIsBlocked() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(1_000);
        farm.world.claimed.add(farm.plot(0));

        farm.tick();

        assertThat(farm.world.blocks).isEmpty();
        assertThat(farm.items().getItem(0).getCount()).isEqualTo(5);
        assertThat(farm.energy()).isEqualTo(1_000);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.BLOCKED);
    }

    @Test
    @DisplayName("a Blocked answer from the logic is reported as Too dark or Blocked")
    void blockedReasons() {
        BlockPos dark = new FarmHarness(new ScriptedFarmLogic()).plot(1);
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) -> plot.equals(dark)
                ? PlotAction.blocked(PlotAction.Reason.TOO_DARK)
                : PlotAction.NONE));
        farm.power(1_000);

        farm.tick();
        farm.tick();

        assertThat(farm.farm().status()).isEqualTo(FarmStatus.TOO_DARK);
    }

    // ==================== Output ====================

    @Test
    @DisplayName("with the output full the farm skips harvests but keeps planting")
    void outputFullSkipsHarvestOnly() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) -> {
            BlockState state = world.state(plot);
            if (state.is(Blocks.WHEAT)) {
                return PlotAction.harvest(plot);
            }
            return state.isAir() ? PlotAction.plant(plot, new ItemStack(Items.OAK_SAPLING)) : PlotAction.NONE;
        }));
        farm.world.set(farm.plot(0), WHEAT);
        fillOutput(farm, new ItemStack(Items.COBBLESTONE, 64));
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(10_000);

        farm.tick();
        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0), WHEAT);
        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.blocks).containsEntry(farm.plot(1), SAPLING);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.OUTPUT_FULL);
    }

    @Test
    @DisplayName("drops that do not fit wait in the overflow, which survives a reload and drains first")
    void overflowDrainsFirstAndPersists() {
        ScriptedFarmLogic logic = harvestingWheat();
        FarmHarness farm = new FarmHarness(logic);
        farm.world.drops = state -> List.of(new ItemStack(Items.WHEAT, 3));
        for (int i = 0; i < 8; i++) {
            farm.world.set(farm.plot(i), WHEAT);
        }
        fillOutput(farm, new ItemStack(Items.COBBLESTONE, 64));
        farm.items().setItem(FarmMachine.OUTPUT_END - 1, new ItemStack(Items.WHEAT, 62));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.broken).containsExactly(farm.plot(0));
        assertThat(farm.items().getItem(FarmMachine.OUTPUT_END - 1).getCount()).isEqualTo(64);
        assertThat(farm.farm().overflow()).singleElement().satisfies(stack -> {
            assertThat(stack.is(Items.WHEAT)).isTrue();
            assertThat(stack.getCount()).isEqualTo(1);
        });

        CompoundTag saved = farm.save();
        FarmHarness reloaded = new FarmHarness(harvestingWheat());
        reloaded.world.drops = farm.world.drops;
        reloaded.world.blocks.putAll(farm.world.blocks);
        reloaded.load(saved);
        reloaded.power(10_000);

        assertThat(reloaded.farm().overflow()).singleElement().satisfies(stack -> assertThat(stack.getCount()).isEqualTo(1));

        reloaded.tick();

        assertThat(reloaded.world.broken).as("no harvesting while the overflow holds anything").isEmpty();
        assertThat(reloaded.farm().status()).isEqualTo(FarmStatus.OUTPUT_FULL);

        reloaded.items().setItem(FarmMachine.OUTPUT_END - 1, ItemStack.EMPTY);
        reloaded.tick();

        assertThat(reloaded.farm().overflow()).isEmpty();
        assertThat(reloaded.world.broken).containsExactly(reloaded.plot(2));
        // The drained overflow went in before the new drops.
        assertThat(reloaded.items().getItem(FarmMachine.OUTPUT_END - 1).getCount()).isEqualTo(1 + 3);
    }

    @Test
    @DisplayName("loose items lying over the ring are swept into the output after a harvest; the centre's are left")
    void sweepsLooseItemsAfterHarvest() {
        FarmHarness farm = new FarmHarness(harvestingWheat());
        farm.world.set(farm.plot(0), WHEAT);
        farm.world.drop(farm.plot(5).above(2), new ItemStack(Items.APPLE, 2));
        farm.world.drop(farm.host.pos().above(), new ItemStack(Items.CARROT, 1));
        farm.power(1_000);

        farm.tick();

        assertThat(output(farm)).anySatisfy(stack -> assertThat(stack.is(Items.APPLE) && stack.getCount() == 2).isTrue());
        assertThat(output(farm)).noneSatisfy(stack -> assertThat(stack.is(Items.CARROT)).isTrue());
        assertThat(farm.world.loose).singleElement().satisfies(item -> assertThat(item.stack().is(Items.CARROT)).isTrue());
    }

    private static List<ItemStack> output(FarmHarness farm) {
        List<ItemStack> output = new ArrayList<>();
        for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END; slot++) {
            output.add(farm.items().getItem(slot));
        }
        return output;
    }

    // ==================== Clearing ====================

    @Test
    @DisplayName("the column above a plot is cleared top-down, one block per visit, before the logic is asked")
    void clearsColumnTopDown() {
        ScriptedFarmLogic logic = plantingEverywhere();
        logic.clearanceHeight = 4;
        FarmHarness farm = new FarmHarness(logic);
        BlockPos plot = farm.plot(0);
        farm.world.set(plot, Blocks.SAND.defaultBlockState());
        farm.world.set(plot.above(2), Blocks.COBBLESTONE.defaultBlockState());
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 64));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.broken).containsExactly(plot.above(2));
        assertThat(farm.logic.decisions).isZero();
        assertThat(farm.energy()).isEqualTo(10_000 - FarmHarness.SETTINGS.cost(FarmCost.CLEAR));
        assertThat(output(farm)).anySatisfy(stack -> assertThat(stack.is(Items.COBBLESTONE)).isTrue());
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);

        farm.ticks(8); // back round to plot 0

        assertThat(farm.world.broken).containsExactly(plot.above(2), plot);
        assertThat(farm.world.blocks).doesNotContainKey(plot);

        farm.ticks(8);

        assertThat(farm.world.blocks).containsEntry(plot, SAPLING);
    }

    @Test
    @DisplayName("the farm's own growth is passed over by clearing, and blocks above it are still cleared")
    void ownGrowthIsNotCleared() {
        ScriptedFarmLogic logic = harvestingWheat();
        logic.clearanceHeight = 3;
        FarmHarness farm = new FarmHarness(logic);
        BlockPos plot = farm.plot(0);
        farm.world.set(plot, WHEAT);
        farm.world.set(plot.above(2), Blocks.TORCH.defaultBlockState());
        farm.power(10_000);

        farm.tick();
        assertThat(farm.world.broken).containsExactly(plot.above(2));
        assertThat(farm.logic.decisions).isZero();

        farm.ticks(8);
        assertThat(farm.world.broken).containsExactly(plot.above(2), plot);
        assertThat(farm.logic.decisions).as("the wheat is the logic's to harvest").isEqualTo(8);
    }

    @Test
    @DisplayName("a block entity or an unbreakable block in the column is never cleared, and the plot is Blocked")
    void protectedBlocksAreNeverCleared() {
        for (BlockState protectedState : List.of(Blocks.CHEST.defaultBlockState(), Blocks.BEDROCK.defaultBlockState())) {
            ScriptedFarmLogic logic = plantingEverywhere();
            logic.clearanceHeight = 3;
            FarmHarness farm = new FarmHarness(logic);
            BlockPos plot = farm.plot(0);
            farm.world.set(plot, Blocks.DIRT.defaultBlockState());
            farm.world.set(plot.above(1), protectedState);
            farm.power(10_000);

            farm.tick();

            assertThat(farm.world.broken).as("%s", protectedState).isEmpty();
            assertThat(farm.world.blocks).as("%s", protectedState).containsEntry(plot.above(1), protectedState);
            assertThat(farm.world.blocks).as("%s", protectedState).containsEntry(plot, Blocks.DIRT.defaultBlockState());
            assertThat(farm.logic.decisions).as("%s", protectedState).isZero();
            assertThat(farm.energy()).as("%s", protectedState).isEqualTo(10_000);
            assertThat(farm.farm().status()).as("%s", protectedState).isEqualTo(FarmStatus.BLOCKED);
        }
    }

    @Test
    @DisplayName("a fluid in the column, source or flowing, is drained to air without drops")
    void drainsFluids() {
        for (BlockState fluid : List.of(
                Blocks.WATER.defaultBlockState(), Fluids.FLOWING_WATER.getFlowing(3, false).createLegacyBlock())) {
            FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
            BlockPos plot = farm.plot(0);
            farm.world.set(plot, fluid);
            farm.power(10_000);

            farm.tick();

            assertThat(farm.world.blocks).as("%s", fluid).containsEntry(plot, Blocks.AIR.defaultBlockState());
            assertThat(farm.world.broken).as("%s", fluid).isEmpty();
            assertThat(output(farm)).as("%s", fluid).allSatisfy(stack -> assertThat(stack.isEmpty()).isTrue());
            assertThat(farm.energy()).as("%s", fluid).isEqualTo(10_000 - FarmHarness.SETTINGS.cost(FarmCost.CLEAR));
        }
    }

    @Test
    @DisplayName("clearing waits for room in the output and for power, and respects claim protection")
    void clearingGates() {
        FarmHarness full = new FarmHarness(new ScriptedFarmLogic());
        full.world.set(full.plot(0), Blocks.COBBLESTONE.defaultBlockState());
        fillOutput(full, new ItemStack(Items.STONE, 64));
        full.power(10_000);
        full.tick();
        assertThat(full.world.broken).isEmpty();
        assertThat(full.farm().status()).isEqualTo(FarmStatus.OUTPUT_FULL);

        FarmHarness unpowered = new FarmHarness(new ScriptedFarmLogic());
        unpowered.world.set(unpowered.plot(0), Blocks.COBBLESTONE.defaultBlockState());
        unpowered.power(FarmHarness.SETTINGS.cost(FarmCost.CLEAR) - 1);
        unpowered.tick();
        assertThat(unpowered.world.broken).isEmpty();
        assertThat(unpowered.farm().status()).isEqualTo(FarmStatus.NO_POWER);

        FarmHarness claimed = new FarmHarness(new ScriptedFarmLogic());
        claimed.world.set(claimed.plot(0), Blocks.COBBLESTONE.defaultBlockState());
        claimed.world.claimed.add(claimed.plot(0));
        claimed.power(10_000);
        claimed.tick();
        assertThat(claimed.world.broken).isEmpty();
        assertThat(claimed.energy()).isEqualTo(10_000);
        assertThat(claimed.farm().status()).isEqualTo(FarmStatus.BLOCKED);
    }

    @Test
    @DisplayName("the square inside the ring, the farm's own column included, is never touched")
    void innerSquareIsNeverTouched() {
        ScriptedFarmLogic logic = plantingEverywhere();
        logic.clearanceHeight = 3;
        FarmHarness farm = new FarmHarness(logic);
        farm.areaOverride(3, 5);
        BlockPos inner = farm.host.pos().offset(1, 1, 0);
        farm.world.set(inner, Blocks.COBBLESTONE.defaultBlockState());
        farm.world.set(farm.host.pos().above(), Blocks.COBBLESTONE.defaultBlockState());
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 64));
        farm.power(10_000);

        farm.ticks(2 * 16);

        assertThat(farm.world.blocks).containsEntry(inner, Blocks.COBBLESTONE.defaultBlockState());
        assertThat(farm.world.blocks).containsEntry(farm.host.pos().above(), Blocks.COBBLESTONE.defaultBlockState());
        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.writeFlags).hasSize(16);
        assertThat(farm.world.writeFlags.keySet()).allMatch(new RingArea(farm.host.pos().above(), 3, 5)::contains);
    }

    // ==================== Multi-block harvests ====================

    private static List<BlockPos> trunk(BlockPos base, int height) {
        List<BlockPos> plan = new ArrayList<>();
        for (int dy = height - 1; dy >= 0; dy--) {
            plan.add(base.above(dy));
        }
        return plan;
    }

    private static ScriptedFarmLogic felling(BlockPos base, List<BlockPos> plan) {
        return new ScriptedFarmLogic().script((world, plot) ->
                plot.equals(base) && world.state(plot).is(Blocks.OAK_LOG) ? new PlotAction.Harvest(plan) : PlotAction.NONE);
    }

    @Test
    @DisplayName("a tree is felled in plan order, one block per action, each charged as a log")
    void fellsOneBlockPerAction() {
        BlockPos base = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        List<BlockPos> plan = trunk(base, 10);
        FarmHarness farm = new FarmHarness(felling(base, plan));
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.power(10_000);

        farm.tick();
        assertThat(farm.world.broken).containsExactlyElementsOf(plan.subList(0, 1));
        assertThat(farm.farm().isHarvesting()).isTrue();

        for (int tick = 0; tick < 20 && farm.farm().isHarvesting(); tick++) {
            int before = farm.world.broken.size();
            farm.tick();
            assertThat(farm.world.broken.size() - before).as("one action a tick").isLessThanOrEqualTo(1);
        }
        assertThat(farm.world.broken).containsExactlyElementsOf(plan);
        assertThat(farm.farm().isHarvesting()).isFalse();
        assertThat(farm.energy()).isEqualTo(10_000 - plan.size() * FarmHarness.SETTINGS.cost(FarmCost.HARVEST_LOG));
    }

    @Test
    @DisplayName("a block changed since the plan was made is never broken: the rest of the plan is dropped and made again")
    void dropsThePlanWhenATargetChanged() {
        BlockPos base = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        List<BlockPos> plan = trunk(base, 6);
        // Plans the logs standing on the base, top-down, as they are when asked.
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) -> {
            if (!plot.equals(base) || !world.state(plot).is(Blocks.OAK_LOG)) {
                return PlotAction.NONE;
            }
            int height = 0;
            while (world.state(plot.above(height)).is(Blocks.OAK_LOG)) {
                height++;
            }
            return new PlotAction.Harvest(trunk(base, height));
        }));
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.power(10_000);

        farm.tick();
        BlockPos changed = plan.get(2);
        farm.world.set(changed, Blocks.STONE.defaultBlockState());
        farm.ticks(30);

        assertThat(farm.world.blocks).containsEntry(changed, Blocks.STONE.defaultBlockState());
        assertThat(farm.world.broken).doesNotContain(changed);
        assertThat(farm.world.broken).containsExactly(plan.get(0), plan.get(1), plan.get(3), plan.get(4), plan.get(5));
        assertThat(farm.farm().isHarvesting()).isFalse();
    }

    @Test
    @DisplayName("a felling in progress survives a reload by re-planning from its plot")
    void harvestResumesAfterReload() {
        BlockPos base = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        List<BlockPos> plan = trunk(base, 10);
        FarmHarness farm = new FarmHarness(felling(base, plan));
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.power(10_000);
        farm.tick();

        FarmHarness reloaded = new FarmHarness(felling(base, plan));
        reloaded.world.blocks.putAll(farm.world.blocks);
        reloaded.load(farm.save());
        reloaded.power(10_000);
        reloaded.tick();

        assertThat(reloaded.world.broken).containsExactly(plan.get(1));
        assertThat(reloaded.farm().isHarvesting()).isTrue();
    }

    @Test
    @DisplayName("a felling that runs out of power waits, and finishes once power returns")
    void harvestStallsWithoutPower() {
        BlockPos base = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        List<BlockPos> plan = trunk(base, 6);
        FarmHarness farm = new FarmHarness(felling(base, plan));
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.power(2 * FarmHarness.SETTINGS.cost(FarmCost.HARVEST_LOG));

        farm.ticks(5);

        assertThat(farm.world.broken).hasSize(2);
        assertThat(farm.farm().isHarvesting()).isTrue();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_POWER);

        farm.power(10_000);
        farm.ticks(8);

        assertThat(farm.world.broken).containsExactlyElementsOf(plan);
    }

    // ==================== Waiting for power ====================

    /** Every plot's dirt waiting to be tilled, with less RF than one till. */
    private static FarmHarness tillingShortOfPower() {
        FarmHarness farm = new FarmHarness(tillingDirt());
        for (int i = 0; i < 8; i++) {
            farm.world.set(farm.plot(i).below(), Blocks.DIRT.defaultBlockState());
        }
        farm.power(FarmHarness.SETTINGS.cost(FarmCost.TILL) - 1);
        return farm;
    }

    @Test
    @DisplayName("work the buffer cannot pay for holds the scan on that plot: no later plot is read or touched")
    void lowPowerHoldsThePlot() {
        FarmHarness farm = tillingShortOfPower();

        farm.tick();
        assertThat(farm.farm().heldPlot()).isZero();
        assertThat(farm.farm().scanCursor()).as("the cursor stays put").isZero();
        long reads = farm.world.readCount;
        int decisions = farm.logic.decisions;

        farm.ticks(500);

        assertThat(farm.farm().scanCursor()).isZero();
        assertThat(farm.world.readCount).as("nothing read while waiting").isEqualTo(reads);
        assertThat(farm.logic.decisions).as("no plot asked about").isEqualTo(decisions);
        assertThat(farm.world.mutations).isEmpty();
        assertThat(farm.energy()).isEqualTo(FarmHarness.SETTINGS.cost(FarmCost.TILL) - 1);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_POWER);
    }

    @Test
    @DisplayName("once the RF arrives the held plot is worked first, at once, and the scan moves on")
    void powerReleasesTheHeldPlot() {
        FarmHarness farm = tillingShortOfPower();
        farm.ticks(500); // far past a pass and any idle back-off

        farm.power(FarmHarness.SETTINGS.cost(FarmCost.TILL));
        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), farmland(0));
        assertThat(farm.world.mutations).hasSize(1);
        assertThat(farm.farm().scanCursor()).isEqualTo(1);
        assertThat(farm.farm().heldPlot()).as("out of RF again, on the next plot").isEqualTo(1);

        farm.power(10_000);
        farm.ticks(8);
        for (int i = 0; i < 8; i++) {
            assertThat(farm.world.blocks).as("plot %s", i).containsEntry(farm.plot(i).below(), farmland(0));
        }
        assertThat(farm.farm().heldPlot()).isEqualTo(-1);
    }

    @Test
    @DisplayName("the held plot is inspected again when the RF arrives: work it no longer needs is not done")
    void heldPlotIsInspectedAgain() {
        FarmHarness farm = tillingShortOfPower();
        farm.tick();
        farm.world.set(farm.plot(0).below(), farmland(3)); // tilled by hand meanwhile

        farm.power(FarmHarness.SETTINGS.cost(FarmCost.TILL));
        farm.ticks(2);

        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), farmland(3));
        assertThat(farm.world.blocks).as("the RF went to the next plot").containsEntry(farm.plot(1).below(), farmland(0));
        assertThat(farm.farm().scanCursor()).isEqualTo(2);
    }

    @Test
    @DisplayName("a reload while held resumes on the held plot")
    void heldPlotSurvivesReload() {
        FarmHarness farm = tillingShortOfPower();
        farm.power(FarmHarness.SETTINGS.cost(FarmCost.TILL) * 2);
        farm.ticks(3); // tills plots 0 and 1, then holds on 2
        assertThat(farm.farm().heldPlot()).isEqualTo(2);

        FarmHarness reloaded = new FarmHarness(tillingDirt());
        reloaded.world.blocks.putAll(farm.world.blocks);
        reloaded.load(farm.save());
        assertThat(reloaded.farm().scanCursor()).isEqualTo(2);
        reloaded.power(FarmHarness.SETTINGS.cost(FarmCost.TILL));
        reloaded.tick();

        assertThat(reloaded.world.mutations).hasSize(1);
        assertThat(reloaded.world.mutations.get(0).pos()).isEqualTo(reloaded.plot(2).below());
    }

    @Test
    @DisplayName("other shortages do not hold the scan: with no germlings it moves on to the next plot")
    void otherShortagesMoveOn() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.power(10_000);

        farm.ticks(3);

        assertThat(farm.farm().heldPlot()).isEqualTo(-1);
        assertThat(farm.farm().scanCursor()).isEqualTo(3);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_GERMLINGS);
    }

    @Test
    @DisplayName("the harvest lane waits on its current block for RF, then breaks that same block")
    void harvestWaitsOnItsBlock() {
        BlockPos base = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        List<BlockPos> plan = trunk(base, 6);
        FarmHarness farm = new FarmHarness(felling(base, plan));
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.power(FarmHarness.SETTINGS.cost(FarmCost.HARVEST_LOG));
        farm.ticks(3);
        assertThat(farm.world.broken).containsExactly(plan.get(0));
        int remaining = farm.farm().harvestRemaining();

        farm.ticks(200);
        assertThat(farm.farm().harvestRemaining()).isEqualTo(remaining);
        assertThat(farm.world.broken).hasSize(1);

        farm.power(FarmHarness.SETTINGS.cost(FarmCost.HARVEST_LOG));
        farm.tick();
        assertThat(farm.world.broken).containsExactly(plan.get(0), plan.get(1));
    }

    // ==================== Soil and water ====================

    @Test
    @DisplayName("laying soil consumes a soil item; with none the farm reports No soil")
    void laysSoil() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(true).script((world, plot) ->
                world.state(plot.below()).is(Blocks.DIRT) ? PlotAction.NONE : new PlotAction.PlaceSoil(plot.below())));
        farm.power(10_000);

        farm.tick();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_SOIL);
        assertThat(farm.world.blocks).isEmpty();

        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.DIRT, 2));
        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(1).below(), Blocks.DIRT.defaultBlockState());
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).isEqualTo(1);
        assertThat(farm.energy()).isEqualTo(10_000 - FarmHarness.SETTINGS.cost(FarmCost.SOIL));
    }

    /** Lays podzol under every plot that has none yet. */
    private static ScriptedFarmLogic layingPodzol() {
        return new ScriptedFarmLogic(true).script((world, plot) ->
                world.state(plot.below()).is(Blocks.PODZOL) ? PlotAction.NONE : new PlotAction.PlaceSoil(plot.below()));
    }

    @Test
    @DisplayName("soil is laid straight onto air")
    void soilOnAir() {
        FarmHarness farm = new FarmHarness(layingPodzol());
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), Blocks.PODZOL.defaultBlockState());
        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("ground that is not the soil, such as dirt, is broken for soil, and its drops are kept")
    void soilReplacesDirtKeepingDrops() {
        FarmHarness farm = new FarmHarness(layingPodzol());
        farm.world.set(farm.plot(0).below(), Blocks.DIRT.defaultBlockState());
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.broken).containsExactly(farm.plot(0).below());
        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), Blocks.PODZOL.defaultBlockState());
        assertThat(farm.items().getItem(FarmMachine.OUTPUT_START).is(Items.DIRT)).isTrue();
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).isEqualTo(1);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);
    }

    @Test
    @DisplayName("soil replaces any other floor too, such as stone, keeping its drops")
    void soilReplacesStone() {
        FarmHarness farm = new FarmHarness(layingPodzol());
        farm.world.set(farm.plot(0).below(), Blocks.STONE.defaultBlockState());
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.broken).containsExactly(farm.plot(0).below());
        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), Blocks.PODZOL.defaultBlockState());
        assertThat(farm.items().getItem(FarmMachine.OUTPUT_START).is(Items.STONE)).isTrue();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);
    }

    @Test
    @DisplayName("with the output full, ground that must be broken for soil is left in place")
    void soilWaitsForRoomInOutput() {
        FarmHarness farm = new FarmHarness(layingPodzol());
        farm.world.set(farm.plot(0).below(), Blocks.SAND.defaultBlockState());
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
        fillOutput(farm, new ItemStack(Items.STONE, 64));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), Blocks.SAND.defaultBlockState());
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).isEqualTo(2);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.OUTPUT_FULL);
    }

    @Test
    @DisplayName("soil never replaces a block entity or an unbreakable block")
    void soilNeverReplacesProtectedBlocks() {
        for (BlockState floor : List.of(Blocks.CHEST.defaultBlockState(), Blocks.BEDROCK.defaultBlockState())) {
            FarmHarness farm = new FarmHarness(layingPodzol());
            farm.world.set(farm.plot(0).below(), floor);
            farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
            farm.power(10_000);

            farm.tick();

            assertThat(farm.world.blocks).as("%s", floor).containsEntry(farm.plot(0).below(), floor);
            assertThat(farm.world.broken).as("%s", floor).isEmpty();
            assertThat(farm.world.writeFlags).as("%s", floor).isEmpty();
            assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).as("%s", floor).isEqualTo(2);
            assertThat(farm.energy()).as("%s", floor).isEqualTo(10_000);
            assertThat(farm.farm().status()).as("%s", floor).isEqualTo(FarmStatus.BLOCKED);
        }
    }

    @Test
    @DisplayName("soil overwrites water, still or flowing, without breaking it; a waterlogged block is broken first")
    void soilOverwritesWater() {
        for (BlockState water : List.of(
                Blocks.WATER.defaultBlockState(), Fluids.FLOWING_WATER.getFlowing(3, false).createLegacyBlock())) {
            FarmHarness farm = new FarmHarness(layingPodzol());
            farm.world.set(farm.plot(0).below(), water);
            farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
            farm.power(10_000);

            farm.tick();

            assertThat(farm.world.blocks).as("%s", water).containsEntry(farm.plot(0).below(), Blocks.PODZOL.defaultBlockState());
            assertThat(farm.world.broken).as("%s", water).isEmpty();
            assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).as("%s", water).isEqualTo(1);
        }

        FarmHarness farm = new FarmHarness(layingPodzol());
        BlockState seagrass = Blocks.SEAGRASS.defaultBlockState();
        farm.world.set(farm.plot(0).below(), seagrass);
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.broken).containsExactly(farm.plot(0).below());
        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), Blocks.PODZOL.defaultBlockState());
    }

    private static final BlockState WET_FARMLAND = farmland(7);

    private static BlockState farmland(int moisture) {
        return Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, moisture);
    }

    /** Tills dirt into dry farmland. */
    private static ScriptedFarmLogic tillingDirt() {
        return new ScriptedFarmLogic().script((world, plot) ->
                world.state(plot.below()).is(Blocks.DIRT) ? new PlotAction.Till(plot.below(), farmland(0)) : PlotAction.NONE);
    }

    @Test
    @DisplayName("a till costs RF only and tills every plot to dry farmland")
    void tillCostsRf() {
        FarmHarness farm = new FarmHarness(tillingDirt());
        for (int i = 0; i < 8; i++) {
            farm.world.set(farm.plot(i).below(), Blocks.DIRT.defaultBlockState());
        }
        farm.power(10_000);

        farm.ticks(8);

        for (int i = 0; i < 8; i++) {
            assertThat(farm.world.blocks).containsEntry(farm.plot(i).below(), farmland(0));
        }
        assertThat(farm.energy()).isEqualTo(10_000 - 8 * FarmHarness.SETTINGS.cost(FarmCost.TILL));
    }

    @Test
    @DisplayName("a till lays the logic's result as asked")
    void tillLaysResult() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) ->
                world.state(plot.below()).is(Blocks.DIRT) ? new PlotAction.Till(plot.below(), farmland(5)) : PlotAction.NONE));
        farm.world.set(farm.plot(0).below(), Blocks.DIRT.defaultBlockState());
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), farmland(5));
    }

    @Test
    @DisplayName("a completed harvest turns the soil under the plot into the logic's after-harvest soil")
    void harvestAppliesSoilAfterHarvest() {
        ScriptedFarmLogic logic = harvestingWheat();
        logic.afterHarvest = soil -> soil.is(Blocks.FARMLAND) ? Blocks.DIRT.defaultBlockState() : null;
        FarmHarness farm = new FarmHarness(logic);
        farm.world.set(farm.plot(0), WHEAT);
        farm.world.set(farm.plot(0).below(), WET_FARMLAND);
        farm.world.set(farm.plot(1), WHEAT);
        farm.world.set(farm.plot(1).below(), Blocks.PODZOL.defaultBlockState());
        farm.world.set(farm.plot(2), WHEAT);
        farm.world.set(farm.plot(2).below(), WET_FARMLAND);
        farm.world.claimed.add(farm.plot(2).below());
        farm.power(10_000);

        farm.ticks(3);

        assertThat(farm.world.broken).containsExactly(farm.plot(0), farm.plot(1), farm.plot(2));
        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), Blocks.DIRT.defaultBlockState());
        assertThat(farm.world.writeFlags).containsEntry(farm.plot(0).below(), Block.UPDATE_ALL);
        assertThat(farm.world.blocks).as("null leaves the soil").containsEntry(farm.plot(1).below(), Blocks.PODZOL.defaultBlockState());
        assertThat(farm.world.blocks).as("claim protection").containsEntry(farm.plot(2).below(), WET_FARMLAND);
    }

    @Test
    @DisplayName("a felling restores the soil under every ring plot it broke a block on, and nowhere else")
    void fellingRestoresSoilUnderEveryPlot() {
        FarmHarness probe = new FarmHarness(new ScriptedFarmLogic());
        BlockPos base = probe.plot(0);
        BlockPos neighbour = probe.plot(1);
        BlockPos outside = probe.area().min().west();
        List<BlockPos> plan = new ArrayList<>(trunk(base, 3));
        plan.add(neighbour);
        plan.add(outside);
        ScriptedFarmLogic logic = felling(base, plan);
        logic.afterHarvest = soil -> soil.is(Blocks.DIRT) ? Blocks.SAND.defaultBlockState() : null;
        FarmHarness farm = new FarmHarness(logic);
        plan.forEach(pos -> farm.world.set(pos, LOG));
        for (BlockPos plot : List.of(base, neighbour, outside)) {
            farm.world.set(plot.below(), Blocks.DIRT.defaultBlockState());
        }
        farm.power(10_000);

        farm.ticks(10);

        assertThat(farm.world.broken).containsAll(plan);
        assertThat(farm.world.blocks).containsEntry(base.below(), Blocks.SAND.defaultBlockState());
        assertThat(farm.world.blocks).containsEntry(neighbour.below(), Blocks.SAND.defaultBlockState());
        assertThat(farm.world.blocks).as("outside the ring").containsEntry(outside.below(), Blocks.DIRT.defaultBlockState());
    }

    @Test
    @DisplayName("plots a felling broke before a reload still get their soil restored when it completes")
    void fellingRestoresSoilAcrossReload() {
        FarmHarness probe = new FarmHarness(new ScriptedFarmLogic());
        BlockPos base = probe.plot(0);
        BlockPos neighbour = probe.plot(1);
        List<BlockPos> plan = new ArrayList<>(List.of(neighbour));
        plan.addAll(trunk(base, 10));
        ScriptedFarmLogic logic = felling(base, plan);
        logic.afterHarvest = soil -> soil.is(Blocks.DIRT) ? Blocks.SAND.defaultBlockState() : null;
        FarmHarness farm = new FarmHarness(logic);
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.world.set(base.below(), Blocks.DIRT.defaultBlockState());
        farm.world.set(neighbour.below(), Blocks.DIRT.defaultBlockState());
        farm.power(10_000);
        farm.tick();
        assertThat(farm.world.broken).contains(neighbour);

        FarmHarness reloaded = new FarmHarness(logic);
        reloaded.world.blocks.putAll(farm.world.blocks);
        reloaded.load(farm.save());
        reloaded.power(10_000);
        reloaded.ticks(20);

        assertThat(reloaded.farm().isHarvesting()).isFalse();
        assertThat(reloaded.world.blocks).containsEntry(base.below(), Blocks.SAND.defaultBlockState());
        assertThat(reloaded.world.blocks).containsEntry(neighbour.below(), Blocks.SAND.defaultBlockState());
    }

    @Test
    @DisplayName("a harvest that leaves its plot standing keeps the soil")
    void refusedHarvestKeepsSoil() {
        ScriptedFarmLogic logic = harvestingWheat();
        logic.afterHarvest = soil -> Blocks.DIRT.defaultBlockState();
        FarmHarness farm = new FarmHarness(logic);
        farm.world.set(farm.plot(0), WHEAT);
        farm.world.set(farm.plot(0).below(), WET_FARMLAND);
        farm.world.claimed.add(farm.plot(0));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0), WHEAT);
        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), WET_FARMLAND);
    }

    // ==================== Scan and status ====================

    @Test
    @DisplayName("after a pass with nothing to do the farm turns idle and waits before scanning on")
    void idleBackOff() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        farm.power(10_000);

        farm.ticks(8);
        assertThat(farm.logic.decisions).isEqualTo(8);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.IDLE);

        farm.ticks(FarmHarness.SETTINGS.idleBackoffBaseTicks());
        assertThat(farm.logic.decisions).as("backing off").isEqualTo(8);

        farm.tick();
        assertThat(farm.logic.decisions).as("the checks saved while backing off, two ticks' worth").isEqualTo(10);
    }

    @Test
    @DisplayName("Working holds for a pass after the last op, then the farm turns idle")
    void workingThenIdle() {
        BlockPos first = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) ->
                plot.equals(first) && world.state(plot).isAir()
                        ? PlotAction.plant(plot, new ItemStack(Items.OAK_SAPLING))
                        : PlotAction.NONE));
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 1));
        farm.power(10_000);

        farm.tick();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);

        farm.ticks(7); // the rest of the pass
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);
        assertThat(farm.host.lit()).isTrue();

        farm.tick();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.IDLE);
        assertThat(farm.host.lit()).as("idle, with RF in the buffer").isTrue();
    }

    @Test
    @DisplayName("the farm is lit whenever its buffer holds RF, whatever its status, and only updates on a change")
    void litFollowsTheBuffer() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.tick();
        assertThat(farm.host.lit()).as("empty buffer").isFalse();

        farm.power(1);
        farm.tick();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_POWER);
        assertThat(farm.host.lit()).as("one RF, not enough for anything").isTrue();

        int writes = farm.host.stateWrites;
        farm.ticks(3);
        assertThat(farm.host.stateWrites).as("no write while nothing changes").isEqualTo(writes);

        farm.power(0);
        farm.tick();
        assertThat(farm.host.lit()).isFalse();

        FarmHarness surveying = FarmHarness.surveying(plantingEverywhere(), FarmHarness.SETTINGS);
        surveying.power(10);
        surveying.tick();
        assertThat(surveying.farm().status()).isEqualTo(FarmStatus.SURVEYING);
        assertThat(surveying.host.lit()).as("surveying, with RF in the buffer").isTrue();
    }

    @Test
    @DisplayName("the scan position and owner survive a reload")
    void persistsScanAndOwner() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        UUID owner = UUID.randomUUID();
        farm.farm().setOwner(owner);
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 8));
        farm.power(10_000);
        farm.ticks(2);

        FarmHarness reloaded = new FarmHarness(plantingEverywhere());
        reloaded.load(farm.save());
        reloaded.power(10_000);
        reloaded.tick();

        assertThat(reloaded.farm().owner()).isEqualTo(owner);
        assertThat(reloaded.world.blocks).containsOnlyKeys(reloaded.plot(2));
    }

    @Test
    @DisplayName("the area override works a different ring, and restores the tier's")
    void areaOverride() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        assertThat(farm.farm().areaSize()).isEqualTo(3);

        farm.areaOverride(1, 5);
        farm.power(10_000);
        farm.ticks(24 + FarmHarness.SETTINGS.idleBackoffBaseTicks());

        assertThat(farm.farm().areaSize()).isEqualTo(5);
        assertThat(farm.logic.decisions).as("one 24-plot pass, then the back-off").isEqualTo(24);

        farm.areaOverride(0, 0);
        assertThat(farm.farm().areaSize()).isEqualTo(FarmHarness.PROFILE.outerSize());
    }

    @Test
    @DisplayName("the rate override checks plots faster, and restores the tier's")
    void rateOverride() {
        FarmHarness tierRate = new FarmHarness(new ScriptedFarmLogic());
        FarmHarness raised = new FarmHarness(new ScriptedFarmLogic());
        tierRate.areaOverride(1, 5);
        raised.areaOverride(1, 5);
        raised.farm().setRateOverride(80, 80);
        tierRate.power(10_000);
        raised.power(10_000);

        tierRate.ticks(6);
        raised.ticks(6);

        assertThat(tierRate.logic.decisions).as("one check a tick").isEqualTo(6);
        assertThat(raised.logic.decisions).as("four checks a tick: the whole 24-plot pass").isEqualTo(24);

        raised.farm().setRateOverride(0, 0);
        raised.ticks(FarmHarness.SETTINGS.idleBackoffBaseTicks() + 3);
        int afterRestore = raised.logic.decisions;
        raised.ticks(3);
        assertThat(raised.logic.decisions - afterRestore).as("back to one check a tick").isEqualTo(3);
    }

    @Test
    @DisplayName("an override that is not a valid ring is refused")
    void invalidAreaOverride() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> farm.areaOverride(3, 3))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> farm.areaOverride(2, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the logic is handed the ring its plot belongs to, one block above the farm so the soil is level with it")
    void logicSeesRing() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        farm.power(10_000);

        farm.tick();

        assertThat(farm.logic.lastArea).isEqualTo(new RingArea(farm.host.pos().above(), 1, 3));
        assertThat(farm.logic.lastArea.plot(0, 0).below().getY()).isEqualTo(farm.host.pos().getY());
        assertThat(farm.logic.lastArea.plot(0, 0)).isEqualTo(farm.plot(0));
    }

    @Test
    @DisplayName("the soil the logic sees is the one the farm would lay next")
    void inputsReportNextSoil() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(true));
        farm.items().setItem(FarmMachine.SOIL_START + 1, new ItemStack(Items.PODZOL, 3));
        farm.items().setItem(FarmMachine.SOIL_START + 2, new ItemStack(Items.DIRT, 5));
        farm.power(10_000);

        farm.tick();

        ItemStack soil = farm.logic.lastInputs.soil();
        assertThat(soil.is(Items.PODZOL)).isTrue();
        assertThat(soil.getCount()).isEqualTo(1);
        assertThat(farm.logic.lastInputs.hasSoil()).isTrue();

        farm.items().setItem(FarmMachine.SOIL_START + 1, ItemStack.EMPTY);
        farm.items().setItem(FarmMachine.SOIL_START + 2, ItemStack.EMPTY);
        assertThat(farm.logic.lastInputs.soil().isEmpty()).isTrue();
    }
}
