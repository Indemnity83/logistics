package com.logistics.forestry.farm.crop;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.FarmInputs;
import com.logistics.forestry.farm.FarmLogic;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.PlotAction;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The Crop Farm: keeps each plot's soil tilled to farmland and wet, plants field crops on it and
 * harvests them once mature, replanting from the harvest first.
 *
 * <p>Crop blocks ({@link CropBlock}) are the farm's own. Overgrowth such as grass, ferns or snow
 * (replaceable blocks without fluid) is cleared so the plot can be farmed; any other block standing
 * on a plot, a torch or a fence, leaves that plot alone, and water is never filled in. Farmland is
 * {@link Blocks#FARMLAND} with {@link BlockStateProperties#MOISTURE}, never the farmland block
 * class, whose name differs between Minecraft versions.
 */
public final class CropFarmLogic implements FarmLogic {

    /** Crops need this much light to be planted and to grow. */
    public static final int MIN_LIGHT = 8;

    /**
     * Farmland this dry or drier is irrigated. Crops grow at the wet rate at any moisture above 0,
     * and vanilla dries farmland one step per random tick, so this leaves a margin without spending
     * water and plot visits on farmland that is still wet.
     */
    public static final int IRRIGATE_AT_MOISTURE = 2;

    private final Predicate<BlockState> tillable;

    public CropFarmLogic() {
        this(state -> state.is(LogisticsForestry.TAG.TILLABLE));
    }

    /** Test seam: {@code tillable} stands in for the {@code logistics:forestry/tillable} tag. */
    public CropFarmLogic(Predicate<BlockState> tillable) {
        this.tillable = tillable;
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
            return isOvergrowth(plant) ? PlotAction.harvest(plot) : PlotAction.NONE;
        }

        BlockPos soilPos = plot.below();
        if (!world.isLoaded(soilPos)) {
            return PlotAction.NONE;
        }
        BlockState soil = world.state(soilPos);
        if (empty && soil.isAir()) {
            return new PlotAction.PlaceSoil(soilPos);
        }
        if (empty && tillable.test(soil)) {
            return new PlotAction.Till(soilPos, Blocks.FARMLAND.defaultBlockState());
        }
        if (!soil.is(Blocks.FARMLAND)) {
            return PlotAction.NONE;
        }

        if (empty) {
            if (world.light(plot) < MIN_LIGHT) {
                return PlotAction.blocked(PlotAction.Reason.TOO_DARK);
            }
            List<ItemStack> germlings = inputs.germlings();
            return PlotAction.plant(plot, germlings.isEmpty() ? ItemStack.EMPTY : germlings.get(0));
        }
        if (inputs.hasWater()
                && soil.hasProperty(BlockStateProperties.MOISTURE)
                && soil.getValue(BlockStateProperties.MOISTURE) <= IRRIGATE_AT_MOISTURE) {
            return new PlotAction.Irrigate(soilPos);
        }
        return PlotAction.NONE;
    }

    /** A replaceable block on a plot that holds no fluid: grass, a fern, a snow layer and the like. */
    private static boolean isOvergrowth(BlockState state) {
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }
}
