package com.logistics.forestry.farm;

import com.logistics.core.machine.area.PlotLayout;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A test farm: oak saplings and wheat seeds are germlings, dirt and podzol are soil, and each plot's action
 * comes from a script. It keeps one block clear above each plot
 * unless told otherwise, and counts wheat, oak saplings and oak logs as its own growth. Counts its decisions
 * and remembers the area and inputs of the last one.
 */
final class ScriptedFarmLogic implements FarmLogic {

    private final boolean laysSoil;
    private BiFunction<FarmWorld, BlockPos, PlotAction> script = (world, plot) -> PlotAction.NONE;
    int clearanceHeight = 1;
    Predicate<BlockState> ownGrowth =
            state -> state.is(Blocks.WHEAT) || state.is(Blocks.OAK_SAPLING) || state.is(Blocks.OAK_LOG);
    Function<BlockState, BlockState> afterHarvest = soil -> null;
    Predicate<ItemStack> germlings = stack -> stack.is(Items.OAK_SAPLING) || stack.is(Items.WHEAT_SEEDS);
    int decisions;
    PlotLayout lastArea;
    FarmInputs lastInputs;

    ScriptedFarmLogic() {
        this(false);
    }

    ScriptedFarmLogic(boolean laysSoil) {
        this.laysSoil = laysSoil;
    }

    ScriptedFarmLogic script(BiFunction<FarmWorld, BlockPos, PlotAction> script) {
        this.script = script;
        return this;
    }

    @Override
    public int clearanceHeight() {
        return clearanceHeight;
    }

    @Override
    public boolean isOwnGrowth(BlockState state) {
        return ownGrowth.test(state);
    }

    @Override
    public boolean laysSoil() {
        return laysSoil;
    }

    @Override
    public boolean isGermling(ItemStack stack) {
        return germlings.test(stack);
    }

    @Override
    public boolean isSoil(ItemStack stack) {
        return stack.is(Items.DIRT) || stack.is(Items.PODZOL);
    }

    @Override
    @Nullable
    public BlockState soilAfterHarvest(BlockState soil) {
        return afterHarvest.apply(soil);
    }

    /** Oak logs are logs and oak leaves are leaves, as the tags would have them; anything else a crop. */
    @Override
    public FarmCost harvestCost(BlockState state) {
        if (state.is(Blocks.OAK_LOG)) {
            return FarmCost.HARVEST_LOG;
        }
        if (state.is(Blocks.OAK_LEAVES)) {
            return FarmCost.HARVEST_LEAVES;
        }
        return FarmCost.HARVEST_CROP;
    }

    @Override
    public PlotAction decide(FarmWorld world, PlotLayout region, BlockPos plot, FarmInputs inputs) {
        lastArea = region;
        lastInputs = inputs;
        return FarmLogic.super.decide(world, region, plot, inputs);
    }

    @Override
    public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
        decisions++;
        return script.apply(world, plot);
    }
}
