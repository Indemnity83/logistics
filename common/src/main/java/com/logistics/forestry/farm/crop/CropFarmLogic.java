package com.logistics.forestry.farm.crop;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.FarmInputs;
import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.PlotAction;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * The Crop Farm: tills each plot's dirt into wet farmland, paying a till's worth of water, plants
 * field crops on it, keeps it wet while the crop grows, and harvests the crop once mature, replanting
 * from the harvest first. A harvest turns the farmland back into dirt, so every crop costs one till.
 *
 * <p>Crop blocks ({@link CropBlock}) are the farm's own growth; anything else above a plot is
 * cleared by the farm before this logic is asked. Soil counts as prepared when it is dirt or
 * farmland; anything else under an empty plot, grass included, is replaced with the farm's soil.
 * Farmland is {@link Blocks#FARMLAND} with {@link BlockStateProperties#MOISTURE}, never the farmland
 * block class, whose name differs between Minecraft versions.
 */
public final class CropFarmLogic implements FarmLogic {

    /** Crops need this much light to be planted and to grow. */
    public static final int MIN_LIGHT = 8;

    /**
     * Farmland under a crop this dry or drier is moistened. Crops grow at the wet rate at any
     * moisture above 0, and vanilla dries farmland one step per random tick, so this leaves a margin
     * without spending plot visits on farmland that is still wet.
     */
    public static final int MOISTEN_AT_MOISTURE = 2;

    private static final BlockState WET_FARMLAND = Blocks.FARMLAND.defaultBlockState()
            .setValue(BlockStateProperties.MOISTURE, Collections.max(BlockStateProperties.MOISTURE.getPossibleValues()));

    private final Predicate<BlockState> tillable;

    public CropFarmLogic() {
        this(state -> state.is(LogisticsForestry.TAG.TILLABLE));
    }

    /** Test seam: {@code tillable} stands in for the {@code logistics:forestry/tillable} tag. */
    public CropFarmLogic(Predicate<BlockState> tillable) {
        this.tillable = tillable;
    }

    @Override
    public int ringOuter() {
        return 15;
    }

    @Override
    public int clearanceHeight() {
        return 5;
    }

    @Override
    public boolean isOwnGrowth(BlockState state) {
        return state.getBlock() instanceof CropBlock;
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
        return stack.is(LogisticsForestry.TAG.CROP_GERMLINGS);
    }

    @Override
    public boolean isSoil(ItemStack stack) {
        return stack.is(LogisticsForestry.TAG.CROP_SOILS);
    }

    @Override
    public boolean replantsFromOutput() {
        return true;
    }

    @Override
    public PlotAction decide(FarmWorld world, BlockPos plot, FarmInputs inputs) {
        BlockState plant = world.state(plot);
        CropBlock crop = plant.getBlock() instanceof CropBlock cropBlock ? cropBlock : null;
        if (crop != null && crop.isMaxAge(plant)) {
            return PlotAction.harvest(plot);
        }
        boolean empty = plant.isAir();
        if (!empty && crop == null) {
            return PlotAction.NONE; // the farm clears it first
        }

        BlockPos soilPos = plot.below();
        if (!world.isLoaded(soilPos)) {
            return PlotAction.NONE;
        }
        BlockState soil = world.state(soilPos);
        if (!soil.is(Blocks.FARMLAND)) {
            if (!empty) {
                return PlotAction.NONE;
            }
            if (!soil.is(Blocks.DIRT)) {
                return new PlotAction.PlaceSoil(soilPos);
            }
            return tillable.test(soil)
                    ? new PlotAction.Till(soilPos, WET_FARMLAND)
                    : PlotAction.NONE;
        }

        if (empty) {
            if (world.light(plot) < MIN_LIGHT) {
                return PlotAction.blocked(PlotAction.Reason.TOO_DARK);
            }
            List<ItemStack> germlings = inputs.germlings();
            return PlotAction.plant(plot, germlings.isEmpty() ? ItemStack.EMPTY : germlings.get(0));
        }
        if (soil.hasProperty(BlockStateProperties.MOISTURE)
                && soil.getValue(BlockStateProperties.MOISTURE) <= MOISTEN_AT_MOISTURE) {
            return new PlotAction.Moisten(soilPos);
        }
        return PlotAction.NONE;
    }

    @Override
    @Nullable
    public BlockState soilAfterHarvest(BlockState soil) {
        return soil.is(Blocks.FARMLAND) ? Blocks.DIRT.defaultBlockState() : null;
    }
}
