package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsForestry;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.forestry.fertilizer.FertilizerCharges;
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
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) -> plot.getX() == 0
                ? PlotAction.blocked(PlotAction.Reason.TOO_DARK)
                : PlotAction.NONE));
        farm.power(1_000);

        farm.tick(); // plot 0 is at x = 1
        farm.tick(); // plot 1 is at x = 0

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
        for (int i = 0; i < 9; i++) {
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
    @DisplayName("loose items lying in the area are swept into the output after a harvest")
    void sweepsLooseItemsAfterHarvest() {
        FarmHarness farm = new FarmHarness(harvestingWheat());
        farm.world.set(farm.plot(0), WHEAT);
        farm.world.loose.add(new ItemStack(Items.APPLE, 2));
        farm.power(1_000);

        farm.tick();

        List<ItemStack> output = new ArrayList<>();
        for (int slot = FarmMachine.OUTPUT_START; slot < FarmMachine.OUTPUT_END; slot++) {
            output.add(farm.items().getItem(slot));
        }
        assertThat(output).anySatisfy(stack -> assertThat(stack.is(Items.APPLE) && stack.getCount() == 2).isTrue());
        assertThat(farm.world.loose).isEmpty();
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

    // ==================== Fertilizer ====================

    @Test
    @DisplayName("fertilizer items are consumed one at a time, each banking its charges")
    void fertilizerChargesOneItemAtATime() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) -> new PlotAction.Fertilize(plot)));
        for (int i = 0; i < 9; i++) {
            farm.world.fertilizable.add(farm.plot(i));
        }
        farm.items().setItem(FarmMachine.FERTILIZER_SLOT, new ItemStack(LogisticsForestry.ITEM.FERTILIZER, 2));
        farm.power(10_000);
        int perItem = FertilizerCharges.charges(new ItemStack(LogisticsForestry.ITEM.FERTILIZER));

        farm.tick();
        assertThat(farm.items().getItem(FarmMachine.FERTILIZER_SLOT).getCount()).isEqualTo(1);
        assertThat(farm.farm().charges()).isEqualTo(perItem - 1);

        farm.ticks(perItem - 1);
        assertThat(farm.items().getItem(FarmMachine.FERTILIZER_SLOT).getCount()).isEqualTo(1);
        assertThat(farm.farm().charges()).isZero();

        farm.tick();
        assertThat(farm.items().getItem(FarmMachine.FERTILIZER_SLOT).isEmpty()).isTrue();
        assertThat(farm.farm().charges()).isEqualTo(perItem - 1);
        assertThat(farm.world.fertilized).hasSize(perItem + 1);
        assertThat(farm.energy()).isEqualTo(10_000 - (perItem + 1) * FarmHarness.SETTINGS.rfFertilize());
    }

    @Test
    @DisplayName("without fertilizer a plant is left to grow on its own, with no error status")
    void noFertilizerIsNotAProblem() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic().script((world, plot) -> new PlotAction.Fertilize(plot)));
        farm.world.fertilizable.add(farm.plot(0));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.fertilized).isEmpty();
        assertThat(farm.energy()).isEqualTo(10_000);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.IDLE);
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
    @DisplayName("replaceable ground such as dirt is dug up for soil, and its drops are kept")
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
    @DisplayName("soil never replaces anything else, such as a stone floor")
    void soilNeverReplacesStone() {
        FarmHarness farm = new FarmHarness(layingPodzol());
        farm.world.set(farm.plot(0).below(), Blocks.STONE.defaultBlockState());
        farm.items().setItem(FarmMachine.SOIL_START, new ItemStack(Items.PODZOL, 2));
        farm.power(10_000);

        farm.tick();

        assertThat(farm.world.blocks).containsEntry(farm.plot(0).below(), Blocks.STONE.defaultBlockState());
        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.writeFlags).isEmpty();
        assertThat(farm.items().getItem(FarmMachine.SOIL_START).getCount()).isEqualTo(2);
        assertThat(farm.energy()).isEqualTo(10_000);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.BLOCKED);
    }

    @Test
    @DisplayName("irrigating wets farmland to full moisture without a neighbour update, spending water")
    void irrigates() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic(true, false).script((world, plot) -> {
            BlockState soil = world.state(plot.below());
            return soil.is(Blocks.FARMLAND) && soil.getValue(BlockStateProperties.MOISTURE) < 7
                    ? new PlotAction.Irrigate(plot.below())
                    : PlotAction.NONE;
        }));
        for (int i = 0; i < 9; i++) {
            farm.world.set(farm.plot(i).below(), Blocks.FARMLAND.defaultBlockState());
        }
        farm.power(10_000);

        farm.tick();
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.NO_WATER);
        assertThat(farm.world.writeFlags).isEmpty();

        farm.machine.water().tank().insert(SimpleFluidKey.of(Fluids.WATER), FluidUnits.mb(1_000), false);
        farm.tick();

        BlockPos soil = farm.plot(1).below();
        assertThat(farm.world.blocks.get(soil).getValue(BlockStateProperties.MOISTURE)).isEqualTo(7);
        assertThat(farm.world.writeFlags).containsEntry(soil, Block.UPDATE_CLIENTS);
        assertThat(FluidUnits.toMillibuckets(farm.machine.water().tank().getAmount()))
                .isEqualTo(1_000 - FarmHarness.SETTINGS.waterPerIrrigateMb());
        assertThat(farm.energy()).isEqualTo(10_000 - FarmHarness.SETTINGS.rfIrrigate());
    }

    @Test
    @DisplayName("only farms that irrigate get a water tank")
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

        farm.ticks(9);
        assertThat(farm.logic.decisions).isEqualTo(9);
        assertThat(farm.farm().status()).isEqualTo(FarmStatus.IDLE);

        farm.ticks(FarmHarness.SETTINGS.idleBackoffBaseTicks());
        assertThat(farm.logic.decisions).as("backing off").isEqualTo(9);

        farm.tick();
        assertThat(farm.logic.decisions).isEqualTo(10);
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

        farm.ticks(8); // the rest of the pass
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
        farm.items().setItem(0, new ItemStack(Items.OAK_SAPLING, 9));
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
    @DisplayName("the area size override works a smaller plot")
    void areaSizeOverride() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        farm.settings = new FarmSettings(9, 1, 10_000, 1_000, 40, 100, 60, 60, 20, 120, 8_000, 50, 4, 20, 200);
        assertThat(farm.farm().areaSize()).isEqualTo(9);

        farm.farm().setAreaSizeOverride(3);
        farm.power(10_000);
        farm.ticks(9 + FarmHarness.SETTINGS.idleBackoffBaseTicks() + 1);

        assertThat(farm.farm().areaSize()).isEqualTo(3);
        assertThat(farm.logic.decisions).as("one 3x3 pass, the back-off, then the next pass").isEqualTo(10);
    }

    @Test
    @DisplayName("the logic is handed the work area its plot belongs to")
    void logicSeesWorkArea() {
        FarmHarness farm = new FarmHarness(new ScriptedFarmLogic());
        farm.power(10_000);

        farm.tick();

        assertThat(farm.logic.lastArea).isNotNull();
        assertThat(farm.logic.lastArea.size()).isEqualTo(3);
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
