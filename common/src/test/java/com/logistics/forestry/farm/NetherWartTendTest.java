package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.DomainRegistrations;
import com.logistics.forestry.farm.netherwart.NetherWartTendLogic;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Nether Wart Farm tending soul sand in its Reach")
class NetherWartTendTest extends MinecraftTestEnvironment {

    /** Long enough for one full pass over the Reach at one check a tick. */
    private static final int PASS = 160;

    private static final BlockPos PLOT = new BlockPos(2, 0, 1);

    private static final BlockState SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState();
    private static final BlockState YOUNG = Blocks.NETHER_WART.defaultBlockState();
    private static final BlockState RIPE = wart(NetherWartBlock.MAX_AGE);

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static BlockState wart(int age) {
        return Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, age);
    }

    /** Stands in for the two tags, which are not bound in unit tests; ripe wart drops three. */
    private static FarmHarness farm() {
        return farm(FarmHarness.SETTINGS);
    }

    private static FarmHarness farm(FarmSettings settings) {
        FarmHarness farm = FarmHarness.tending(
                new NetherWartTendLogic(state -> state.is(Blocks.NETHER_WART), state -> state.is(Blocks.SOUL_SAND)),
                settings);
        farm.world.drops = state -> List.of(new ItemStack(Items.NETHER_WART, 3));
        farm.power(10_000);
        return farm;
    }

    /** Wart dropping nothing, so a harvested Plot is left empty. */
    private static FarmHarness wartlessFarm() {
        FarmHarness farm = farm();
        farm.world.drops = state -> List.of();
        return farm;
    }

    private static void field(FarmHarness farm, BlockPos plot, BlockState ground, BlockState wart) {
        farm.world.set(plot.below(), ground);
        farm.world.set(plot, wart);
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

    @Test
    void harvestsRipeWartAndReplantsFromItsDrops() {
        FarmHarness farm = farm();
        field(farm, PLOT, SOUL_SAND, RIPE);

        farm.ticks(PASS);

        assertThat(farm.world.broken).containsExactly(PLOT);
        assertThat(farm.world.blocks.get(PLOT)).isEqualTo(YOUNG);
        assertThat(farm.world.blocks.get(PLOT.below())).isEqualTo(SOUL_SAND);
        assertThat(count(farm.items(), Items.NETHER_WART)).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void leavesUnripeWartAlone(int age) {
        FarmHarness farm = farm();
        field(farm, PLOT, SOUL_SAND, wart(age));

        farm.ticks(PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.mutations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"soul_soil", "netherrack", "farmland"})
    void leavesWartOnOtherGroundAloneAndNeverRemembersIt(String ground) {
        FarmHarness farm = farm();
        BlockState other = switch (ground) {
            case "soul_soil" -> Blocks.SOUL_SOIL.defaultBlockState();
            case "netherrack" -> Blocks.NETHERRACK.defaultBlockState();
            default -> Blocks.FARMLAND.defaultBlockState();
        };
        field(farm, PLOT, other, RIPE);

        farm.ticks(PASS);

        assertThat(farm.world.broken).isEmpty();
        assertThat(farm.world.mutations).isEmpty();
        assertThat(farm.engine().plotsTotal()).isZero();
    }

    @Test
    void takesOnGrowingWartAndRefillsItsEmptyPlotFromTheInventory() {
        FarmHarness farm = farm();
        field(farm, PLOT, SOUL_SAND, YOUNG);
        farm.ticks(PASS);
        assertThat(farm.engine().plotsTotal()).isEqualTo(1);

        farm.world.blocks.remove(PLOT);
        farm.ticks(PASS);
        assertThat(farm.world.blocks.get(PLOT)).as("left empty").isNull();
        assertThat(farm.engine().status()).isEqualTo(FarmStatus.WAITING_FOR_SEEDS);

        farm.items().setItem(5, new ItemStack(Items.NETHER_WART, 2));
        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(PLOT)).isEqualTo(YOUNG);
        assertThat(count(farm.items(), Items.NETHER_WART)).isEqualTo(1);
    }

    @Test
    void neverLaysSoulSandAndForgetsAPlotWhoseSoulSandIsReplaced() {
        FarmHarness farm = wartlessFarm();
        field(farm, PLOT, SOUL_SAND, RIPE);
        farm.ticks(PASS);
        assertThat(farm.world.blocks.get(PLOT)).as("left empty").isNull();

        farm.world.set(PLOT.below(), Blocks.SOUL_SOIL.defaultBlockState());
        farm.items().setItem(0, new ItemStack(Items.NETHER_WART, 4));
        farm.ticks(PASS);
        assertThat(farm.world.blocks.get(PLOT.below())).as("not repaired").isEqualTo(Blocks.SOUL_SOIL.defaultBlockState());

        farm.world.set(PLOT.below(), SOUL_SAND);
        farm.ticks(2 * PASS);

        assertThat(farm.world.blocks.get(PLOT)).as("forgotten, so not replanted").isNull();
        assertThat(count(farm.items(), Items.NETHER_WART)).isEqualTo(4);
    }

    @Test
    void chargesTheHarvestAndThePlantAndNeverATill() {
        FarmHarness farm = farm();
        field(farm, PLOT, SOUL_SAND, RIPE);

        farm.ticks(PASS);

        assertThat(farm.energy()).isEqualTo(10_000 - farm.cost(FarmCost.HARVEST_CROP) - farm.cost(FarmCost.PLANT));
    }

    @Test
    void paysItsOwnUpkeepRateForEachPlot() {
        FarmHarness farm = farm(FarmHarness.SETTINGS
                .withUpkeep(FarmUpkeep.NETHER_WART_FARM, 0.5)
                .withUpkeep(FarmUpkeep.CROP_FARM, 5.0));
        field(farm, PLOT, SOUL_SAND, YOUNG);
        field(farm, PLOT.west(3), SOUL_SAND, YOUNG);
        farm.ticks(PASS);
        assertThat(farm.engine().plotsTotal()).isEqualTo(2);
        long before = farm.energy();

        farm.ticks(10);

        assertThat(before - farm.energy()).isEqualTo(10);
    }

    @Test
    void aFullInventorySpillsAtThePlotAndStillReplants() {
        FarmHarness farm = farm();
        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            farm.items().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        field(farm, PLOT, SOUL_SAND, RIPE);

        farm.ticks(PASS);

        assertThat(farm.world.blocks.get(PLOT)).isEqualTo(YOUNG);
        assertThat(farm.world.spilled).isNotEmpty().allMatch(loose -> loose.pos().equals(PLOT));
        assertThat(farm.world.spilled.stream()
                        .filter(loose -> loose.stack().is(Items.NETHER_WART))
                        .mapToInt(loose -> loose.stack().getCount())
                        .sum())
                .isEqualTo(2);
    }
}
