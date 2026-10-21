package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.core.machine.MachineHudModel;
import com.logistics.forestry.farm.crop.CropTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Crop Farm Plots and Empty Plots")
class TendPlotTest extends MinecraftTestEnvironment {

    /** Long enough for one full pass over the Reach at one check a tick. */
    private static final int PASS = 160;

    private static final Set<Block> TENDED = Set.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);

    /** Stands in for {@code logistics:forestry/crop_farm_ground}. */
    private static final Set<Block> GROUND = Set.of(Blocks.FARMLAND, Blocks.DIRT, Blocks.GRASS_BLOCK);

    private static final BlockPos PLOT = new BlockPos(2, 0, 1);

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    /** A Crop Farm whose wheat and beetroot harvests roll no seed; carrots and potatoes replant themselves. */
    private static FarmHarness seedlessFarm() {
        FarmHarness farm = FarmHarness.tending(new CropTendLogic(
                state -> TENDED.contains(state.getBlock()), state -> GROUND.contains(state.getBlock())));
        farm.world.drops = state -> {
            if (state.is(Blocks.WHEAT)) {
                return List.of(new ItemStack(Items.WHEAT));
            }
            if (state.is(Blocks.BEETROOTS)) {
                return List.of(new ItemStack(Items.BEETROOT));
            }
            return List.of(new ItemStack(state.getBlock().asItem(), 2));
        };
        farm.power(10_000);
        return farm;
    }

    private static BlockState ripe(Block crop) {
        CropBlock block = (CropBlock) crop;
        return block.getStateForAge(block.getMaxAge());
    }

    private static void field(FarmHarness farm, BlockPos plot, BlockState crop) {
        farm.world.set(plot.below(), Blocks.FARMLAND.defaultBlockState());
        farm.world.set(plot, crop);
    }

    private static int count(Container items, Item item) {
        int total = 0;
        for (int slot = 0; slot < items.getContainerSize(); slot++) {
            ItemStack stack = items.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Harvests the ripe wheat at {@link #PLOT}, which rolls no seed, so the plot is left empty. */
    private static FarmHarness emptyWheatPlot() {
        FarmHarness farm = seedlessFarm();
        field(farm, PLOT, ripe(Blocks.WHEAT));
        farm.ticks(PASS);
        assertThat(farm.world.blocks.get(PLOT)).as("left empty").isNull();
        return farm;
    }

    @Test
    void replantsFromTheInventoryWhenTheDropsHoldNoGermling() {
        FarmHarness farm = seedlessFarm();
        farm.items().setItem(3, new ItemStack(Items.WHEAT_SEEDS, 5));
        field(farm, PLOT, ripe(Blocks.WHEAT));

        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(PLOT)).isEqualTo(Blocks.WHEAT.defaultBlockState());
        assertThat(count(farm.items(), Items.WHEAT_SEEDS)).isEqualTo(4);
        assertThat(count(farm.items(), Items.WHEAT)).isEqualTo(1);
    }

    @Test
    void replantsAnEmptyPlotOnceItsGermlingArrives() {
        FarmHarness farm = emptyWheatPlot();

        farm.items().setItem(7, new ItemStack(Items.WHEAT_SEEDS, 2));
        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(PLOT)).isEqualTo(Blocks.WHEAT.defaultBlockState());
        assertThat(count(farm.items(), Items.WHEAT_SEEDS)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"dirt", "grass_block"})
    void reTillsAnEmptyPlotsDirtOrGrassAndChargesTheTill(String ground) {
        FarmHarness farm = emptyWheatPlot();
        farm.world.set(PLOT.below(), groundBlock(ground).defaultBlockState());
        long before = farm.energy();

        farm.items().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 1));
        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(PLOT.below())).isEqualTo(Blocks.FARMLAND.defaultBlockState());
        assertThat(farm.world.blocks.get(PLOT)).isEqualTo(Blocks.WHEAT.defaultBlockState());
        assertThat(farm.energy()).isEqualTo(before - farm.cost(FarmCost.TILL) - farm.cost(FarmCost.PLANT));
    }

    @Test
    void replantsFarmlandWithoutTillingIt() {
        FarmHarness farm = emptyWheatPlot();
        long before = farm.energy();

        farm.items().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 1));
        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(PLOT)).isEqualTo(Blocks.WHEAT.defaultBlockState());
        assertThat(farm.energy()).isEqualTo(before - farm.cost(FarmCost.PLANT));
    }

    /** Seeds in, a pass, and the plot is still bare: the farm no longer remembers it. */
    private static void assertForgotten(FarmHarness farm) {
        farm.items().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 4));
        farm.ticks(2 * PASS);
        assertThat(farm.world.blocks.get(PLOT)).as("not replanted").isNull();
        assertThat(count(farm.items(), Items.WHEAT_SEEDS)).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"dirt_path", "slab", "air"})
    void forgetsAPlotWhoseGroundIsReplaced(String ground) {
        FarmHarness farm = emptyWheatPlot();
        farm.world.set(PLOT.below(), groundBlock(ground).defaultBlockState());
        farm.ticks(PASS);

        farm.world.set(PLOT.below(), Blocks.FARMLAND.defaultBlockState());
        assertForgotten(farm);
    }

    @Test
    void forgetsAPlotAnotherCropIsPlantedOn() {
        FarmHarness farm = emptyWheatPlot();
        farm.world.set(PLOT, Blocks.CARROTS.defaultBlockState());
        farm.ticks(PASS);

        farm.world.blocks.remove(PLOT);
        assertForgotten(farm);
    }

    @Test
    void forgetsAPlotATorchIsPlacedOn() {
        FarmHarness farm = emptyWheatPlot();
        farm.world.set(PLOT, Blocks.TORCH.defaultBlockState());
        farm.ticks(PASS);

        farm.world.blocks.remove(PLOT);
        assertForgotten(farm);
    }

    @Test
    void keepsAPlotWhileItsOwnCropGrows() {
        FarmHarness farm = emptyWheatPlot();
        farm.world.set(PLOT, Blocks.WHEAT.defaultBlockState());
        farm.ticks(PASS);

        farm.world.blocks.remove(PLOT);
        farm.items().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 4));
        farm.ticks(PASS);
        assertThat(farm.world.blocks.get(PLOT)).isEqualTo(Blocks.WHEAT.defaultBlockState());
    }

    private static List<MachineHudModel.Entry> hud(FarmHarness farm) {
        MachineHudModel hud = new MachineHudModel();
        farm.engine().contributeHud(hud);
        CompoundTag data = new CompoundTag();
        hud.save(data);
        return MachineHudModel.entries(data);
    }

    @Test
    void reportsWaitingForSeedsWithTheEmptyPlotCount() {
        FarmHarness farm = seedlessFarm();
        field(farm, PLOT, ripe(Blocks.WHEAT));
        field(farm, new BlockPos(-1, 0, -2), ripe(Blocks.BEETROOTS));
        field(farm, new BlockPos(1, 1, 3), ripe(Blocks.CARROTS));
        farm.ticks(2 * PASS);

        assertThat(farm.engine().status()).isEqualTo(FarmStatus.WAITING_FOR_SEEDS);
        assertThat(farm.engine().emptyPlots()).isEqualTo(2);
        assertThat(hud(farm)).first().isEqualTo(new MachineHudModel.LineEntry(
                TendFarmComponent.HUD_STATUS, FarmStatus.WAITING_FOR_SEEDS.translationKey(), List.of("2")));

        farm.items().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 4));
        farm.items().setItem(1, new ItemStack(Items.BEETROOT_SEEDS, 4));
        farm.ticks(2 * PASS);

        assertThat(farm.engine().status()).isEqualTo(FarmStatus.IDLE);
        assertThat(farm.engine().emptyPlots()).isZero();
        assertThat(hud(farm)).first().isEqualTo(
                new MachineHudModel.LineEntry(TendFarmComponent.HUD_STATUS, FarmStatus.IDLE.translationKey(), List.of()));
    }

    @Test
    void remembersItsPlotsAcrossASave() {
        FarmHarness farm = emptyWheatPlot();

        FarmHarness loaded = seedlessFarm();
        loaded.world.blocks.putAll(farm.world.blocks);
        loaded.load(farm.save());
        loaded.power(10_000);
        assertThat(loaded.engine().emptyPlots()).isEqualTo(1);
        loaded.items().setItem(0, new ItemStack(Items.WHEAT_SEEDS, 1));
        loaded.ticks(PASS);

        assertThat(loaded.world.blocks.get(PLOT)).isEqualTo(Blocks.WHEAT.defaultBlockState());
    }

    private static Block groundBlock(String name) {
        return switch (name) {
            case "dirt" -> Blocks.DIRT;
            case "grass_block" -> Blocks.GRASS_BLOCK;
            case "dirt_path" -> Blocks.DIRT_PATH;
            case "slab" -> Blocks.OAK_SLAB;
            case "air" -> Blocks.AIR;
            default -> throw new IllegalArgumentException(name);
        };
    }

    @Test
    void neverFillsAWheatPlotWithAnotherCropsGermling() {
        FarmHarness farm = emptyWheatPlot();

        farm.items().setItem(0, new ItemStack(Items.BEETROOT_SEEDS, 4));
        farm.items().setItem(1, new ItemStack(Items.CARROT, 4));
        farm.ticks(2 * PASS);

        assertThat(farm.world.blocks.get(PLOT)).isNull();
        assertThat(count(farm.items(), Items.BEETROOT_SEEDS)).isEqualTo(4);
        assertThat(count(farm.items(), Items.CARROT)).isEqualTo(4);
    }
}
