package com.logistics.forestry.farm;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A test farm: oak saplings and wheat seeds are germlings, dirt is soil, and each plot's action
 * comes from a script. Counts its decisions.
 */
final class ScriptedFarmLogic implements FarmLogic {

    private final boolean usesWater;
    private final boolean laysSoil;
    private BiFunction<FarmWorld, BlockPos, PlotAction> script = (world, plot) -> PlotAction.NONE;
    int decisions;

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
        return stack.is(Items.DIRT);
    }

    @Override
    public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
        decisions++;
        return script.apply(world, plot);
    }
}
