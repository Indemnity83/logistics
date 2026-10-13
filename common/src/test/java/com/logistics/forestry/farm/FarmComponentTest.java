package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
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
        assertThat(farm.energy()).isEqualTo(1_000 - FarmHarness.SETTINGS.rfPlant());
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.WORKING);
        assertThat(farm.host.lit()).isTrue();
    }

    @Test
    @DisplayName("power that covers only part of an op leaves the op undone")
    void noPartialOps() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic()
                .script((world, plot) -> new PlotAction.Plant(List.of(plot, plot.east()), new ItemStack(Items.OAK_SAPLING))));
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(FarmHarness.SETTINGS.rfPlant()); // pays for one sapling, the group needs two

        farm.tick();

        assertThat(farm.world.blocks).isEmpty();
        assertThat(farm.items().getItem(0).getCount()).isEqualTo(5);
        assertThat(farm.energy()).isEqualTo(FarmHarness.SETTINGS.rfPlant());
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
    @DisplayName("a redstone signal pauses the farm until it is removed")
    void redstonePauses() {
        FarmHarness farm = new FarmHarness(plantingEverywhere());
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 5));
        farm.power(1_000);
        farm.world.signal = true;

        farm.ticks(5);

        assertThat(farm.world.blocks).isEmpty();
        assertThat(farm.logic.decisions).isZero();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.PAUSED);
        assertThat(farm.host.lit()).isFalse();

        farm.world.signal = false;
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
        assertThat(farm.energy()).isEqualTo(10_000 - FarmHarness.SETTINGS.rfClearPerBlock());
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
            assertThat(farm.energy()).as("%s", fluid).isEqualTo(10_000 - FarmHarness.SETTINGS.rfClearPerBlock());
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
        unpowered.power(FarmHarness.SETTINGS.rfClearPerBlock() - 1);
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
        farm.farm().setAreaOverride(3, 5);
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
        assertThat(farm.world.writeFlags.keySet()).allMatch(new RingArea(farm.host.pos(), 3, 5)::contains);
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
    @DisplayName("a tree is felled in plan order, at most harvest_blocks_per_tick blocks a tick")
    void fellsAFewBlocksPerTick() {
        BlockPos base = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        List<BlockPos> plan = trunk(base, 10);
        FarmHarness farm = new FarmHarness(felling(base, plan));
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.power(10_000);
        int perTick = FarmHarness.SETTINGS.harvestBlocksPerTick();

        farm.tick();
        assertThat(farm.world.broken).containsExactlyElementsOf(plan.subList(0, perTick));
        assertThat(farm.farm().isHarvesting()).isTrue();

        farm.tick();
        assertThat(farm.world.broken).containsExactlyElementsOf(plan.subList(0, 2 * perTick));

        farm.tick();
        assertThat(farm.world.broken).containsExactlyElementsOf(plan);
        assertThat(farm.farm().isHarvesting()).isFalse();
        assertThat(farm.energy()).isEqualTo(10_000 - plan.size() * FarmHarness.SETTINGS.rfHarvestPerBlock());
        assertThat(farm.logic.decisions).as("felling holds the scan").isEqualTo(1);
    }

    @Test
    @DisplayName("a block changed since the plan was made is left standing")
    void skipsBlocksChangedSincePlanned() {
        BlockPos base = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        List<BlockPos> plan = trunk(base, 6);
        FarmHarness farm = new FarmHarness(felling(base, plan));
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.power(10_000);

        farm.tick();
        farm.world.set(plan.get(5), Blocks.STONE.defaultBlockState());
        farm.tick();

        assertThat(farm.world.blocks).containsEntry(plan.get(5), Blocks.STONE.defaultBlockState());
        assertThat(farm.world.broken).containsExactlyElementsOf(plan.subList(0, 5));
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

        int perTick = FarmHarness.SETTINGS.harvestBlocksPerTick();
        assertThat(reloaded.world.broken).containsExactlyElementsOf(plan.subList(perTick, 2 * perTick));
        assertThat(reloaded.farm().isHarvesting()).isTrue();
    }

    @Test
    @DisplayName("a felling that runs out of power waits, and finishes once power returns")
    void harvestStallsWithoutPower() {
        BlockPos base = new FarmHarness(new ScriptedFarmLogic()).plot(0);
        List<BlockPos> plan = trunk(base, 6);
        FarmHarness farm = new FarmHarness(felling(base, plan));
        plan.forEach(pos -> farm.world.set(pos, LOG));
        farm.power(2 * FarmHarness.SETTINGS.rfHarvestPerBlock());

        farm.ticks(3);

        assertThat(farm.world.broken).hasSize(2);
        assertThat(farm.farm().isHarvesting()).isTrue();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_POWER);

        farm.power(10_000);
        farm.tick();

        assertThat(farm.world.broken).containsExactlyElementsOf(plan);
    }

    // ==================== Soil and water ====================

    @Test
    @DisplayName("laying soil consumes a soil item; with none the farm reports No soil")
    void laysSoil() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(false, true).script((world, plot) ->
                world.state(plot.below()).is(Blocks.DIRT) ? PlotAction.NONE : new PlotAction.PlaceSoil(plot.below())));
        farm.power(10_000);

        farm.tick();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_SOIL);
        assertThat(farm.world.blocks).isEmpty();

        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.DIRT, 2));
        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(1).below(), Blocks.DIRT.defaultBlockState());
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).isEqualTo(1);
        assertThat(farm.energy()).isEqualTo(10_000 - FarmHarness.SETTINGS.rfSoil());
    }

    /** Lays podzol under every plot that has none yet. */
    private static ScriptedFarmLogic layingPodzol() {
        return new ScriptedFarmLogic(false, true).script((world, plot) ->
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

    private static final BlockState WET_FARMLAND = Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7);

    private static ScriptedFarmLogic tillingDirt(boolean usesWater) {
        return new ScriptedFarmLogic(usesWater, false).script((world, plot) ->
                world.state(plot.below()).is(Blocks.DIRT) ? new PlotAction.Till(plot.below(), WET_FARMLAND) : PlotAction.NONE);
    }

    private static void fillWater(FarmHarness farm, long mb) {
        farm.machine.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(mb), false);
    }

    private static long waterMb(FarmHarness farm) {
        return FluidUnits.toMillibuckets(farm.machine.water().tank().getAmount());
    }

    @Test
    @DisplayName("each till spends exactly water_per_till; without it the till is skipped and the farm reports No water")
    void tillSpendsWaterPerTill() {
        FarmHarness farm = new FarmHarness(tillingDirt(true));
        for (int i = 0; i < 8; i++) {
            farm.world.set(farm.plot(i).below(), Blocks.DIRT.defaultBlockState());
        }
        int perTill = FarmHarness.SETTINGS.waterPerTillMb();
        fillWater(farm, 2L * perTill + perTill / 2);
        farm.power(10_000);

        farm.ticks(8);

        assertThat(tilled(farm)).isEqualTo(2);
        assertThat(waterMb(farm)).isEqualTo(perTill / 2);
        assertThat(farm.energy()).isEqualTo(10_000 - 2 * FarmHarness.SETTINGS.rfTill());
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_WATER);

        fillWater(farm, perTill);
        farm.ticks(100);
        assertThat(tilled(farm)).isEqualTo(3);
        assertThat(waterMb(farm)).isEqualTo(perTill / 2);
    }

    private static int tilled(FarmHarness farm) {
        int tilled = 0;
        for (int i = 0; i < 8; i++) {
            if (farm.world.blocks.get(farm.plot(i).below()) == WET_FARMLAND) {
                tilled++;
            }
        }
        return tilled;
    }

    @Test
    @DisplayName("a farm without a tank tills for RF alone")
    void tillWithoutTankIsFree() {
        FarmHarness farm = new FarmHarness(tillingDirt(false));
        farm.world.set(farm.plot(0).below(), Blocks.DIRT.defaultBlockState());
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), WET_FARMLAND);
        assertThat(farm.energy()).isEqualTo(10_000 - FarmHarness.SETTINGS.rfTill());
    }

    @Test
    @DisplayName("an idle farm with an empty tank does not report No water")
    void emptyTankAloneIsNotNoWater() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(true, false));
        farm.power(10_000);

        farm.ticks(3);

        assertThat(farm.farm().status()).isNotEqualTo(FarmStatus.NO_WATER);
    }

    @Test
    @DisplayName("a farm without a tank never reports No water")
    void noTankNoWaterStatus() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(false, false));
        farm.power(10_000);

        farm.ticks(3);

        assertThat(farm.farm().status()).isNotEqualTo(FarmStatus.NO_WATER);
    }

    @Test
    @DisplayName("moistening wets farmland to full moisture without a neighbour update, for RF and no water")
    void moistensWithoutWater() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(true, false).script((world, plot) -> {
            BlockState soil = world.state(plot.below());
            return soil.is(Blocks.FARMLAND) && soil.getValue(BlockStateProperties.MOISTURE) < 7
                    ? new PlotAction.Moisten(plot.below())
                    : PlotAction.NONE;
        }));
        for (int i = 0; i < 8; i++) {
            farm.world.set(farm.plot(i).below(), Blocks.FARMLAND.defaultBlockState());
        }
        fillWater(farm, 1_000);
        farm.power(10_000);

        farm.tick();

        BlockPos soil = farm.plot(0).below();
        assertThat(farm.world.blocks.get(soil).getValue(BlockStateProperties.MOISTURE)).isEqualTo(7);
        assertThat(farm.world.writeFlags).containsEntry(soil, Block.UPDATE_CLIENTS);
        assertThat(waterMb(farm)).isEqualTo(1_000);
        assertThat(farm.energy()).isEqualTo(10_000 - FarmHarness.SETTINGS.rfMoisten());

        FarmHarness dry = new FarmHarness(farm.logic);
        dry.world.set(dry.plot(0).below(), Blocks.FARMLAND.defaultBlockState());
        dry.power(10_000);
        dry.tick();
        assertThat(dry.world.blocks.get(dry.plot(0).below()).getValue(BlockStateProperties.MOISTURE))
                .as("an empty tank does not stop moistening")
                .isEqualTo(7);
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

    @Test
    @DisplayName("only farms that use water get a water tank")
    void waterTankOnlyWhenUsed() {
        assertThat(new FarmHarness(new ScriptedFarmLogic(false, false)).machine.water()).isNull();
        assertThat(new FarmHarness(new ScriptedFarmLogic(true, false)).machine.water()).isNotNull();
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
        assertThat(farm.logic.decisions).isEqualTo(9);
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
        assertThat(farm.host.lit()).isFalse();
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
    @DisplayName("the area override works a different ring, and restores the logic's")
    void areaOverride() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        assertThat(farm.farm().areaSize()).isEqualTo(3);

        farm.farm().setAreaOverride(1, 5);
        farm.power(10_000);
        farm.ticks(24 + FarmHarness.SETTINGS.idleBackoffBaseTicks() + 1);

        assertThat(farm.farm().areaSize()).isEqualTo(5);
        assertThat(farm.logic.decisions).as("one 24-plot pass, the back-off, then the next pass").isEqualTo(25);

        farm.farm().setAreaOverride(0, 0);
        assertThat(farm.farm().areaSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("an override that is not a valid ring is refused")
    void invalidAreaOverride() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> farm.farm().setAreaOverride(3, 3))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> farm.farm().setAreaOverride(2, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the logic is handed the ring its plot belongs to")
    void logicSeesRing() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        farm.power(10_000);

        farm.tick();

        assertThat(farm.logic.lastArea).isEqualTo(new RingArea(farm.host.pos(), 1, 3));
        assertThat(farm.logic.lastArea.plot(0)).isEqualTo(farm.plot(0));
    }

    @Test
    @DisplayName("the soil the logic sees is the one the farm would lay next")
    void inputsReportNextSoil() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(false, true));
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
