package com.logistics.forestry.farm;

import com.logistics.core.machine.area.PlotArea;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A test farm: oak saplings and wheat seeds are germlings, dirt and podzol are soil, and each plot's action
 * comes from a script. It works the 8-plot ring around a 1×1 centre, keeps one block clear above each plot
 * unless told otherwise, and counts wheat, oak saplings and oak logs as its own growth. Counts its decisions
 * and remembers the area and inputs of the last one.
 */
final class ScriptedFarmLogic implements FarmLogic {

    private final boolean usesWater;
    private final boolean laysSoil;
    private BiFunction<FarmWorld, BlockPos, PlotAction> script = (world, plot) -> PlotAction.NONE;
    int clearanceHeight = 1;
    Predicate<BlockState> ownGrowth =
            state -> state.is(Blocks.WHEAT) || state.is(Blocks.OAK_SAPLING) || state.is(Blocks.OAK_LOG);
    int decisions;
    PlotArea lastArea;
    FarmInputs lastInputs;

    ScriptedFarmLogic() {
        this(false, false);
    }

    ScriptedFarmLogic(boolean usesWater, boolean laysSoil) {
        this.usesWater = usesWater;
        this.laysSoil = laysSoil;
    }

    ScriptedFarmLogic script(BiFunction<FarmWorld, BlockPos, PlotAction> script) {
        this.script = script;
        return this;
    }

    @Override
    public int ringInner() {
        return 1;
    }

    @Override
    public int ringOuter() {
        return 3;
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
    public boolean usesWater() {
        return usesWater;
    }

    @Override
    public boolean laysSoil() {
        return laysSoil;
    }

    @Override
    public boolean isGermling(ItemStack stack) {
        return stack.is(Items.OAK_SAPLING) || stack.is(Items.WHEAT_SEEDS);
    }

    @Override
    public boolean isSoil(ItemStack stack) {
        return stack.is(Items.DIRT) || stack.is(Items.PODZOL);
    }

    @Override
    public PlotAction decide(FarmWorld world, PlotArea area, BlockPos plot, FarmInputs inputs) {
        lastArea = area;
        lastInputs = inputs;
        return FarmLogic.super.decide(world, area, plot, inputs);
    }

    @Override
    public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
        decisions++;
        return script.apply(world, plot);
    }
}
