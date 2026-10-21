package com.logistics.forestry.farm.crop;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.TendLogic;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

/**
 * The Crop Farm: harvests a crop in {@code logistics:forestry/crop_farm_tends} once it reaches its
 * maximum age, and replants the same crop from the crop block's own item (wheat seeds for wheat).
 *
 * <p>A {@link CropBlock} knows its maximum age; any other tagged block counts as ripe at the
 * highest value of its {@code age} property.
 */
public final class CropTendLogic implements TendLogic {

    private static final String AGE = "age";

    /** A till's result: dry farmland. */
    private static final BlockState FARMLAND = Blocks.FARMLAND.defaultBlockState();

    private final Predicate<BlockState> tended;

    public CropTendLogic() {
        this(state -> state.is(LogisticsForestry.TAG.CROP_FARM_TENDS));
    }

    /** Test seam: {@code tended} stands in for the {@code logistics:forestry/crop_farm_tends} tag. */
    public CropTendLogic(Predicate<BlockState> tended) {
        this.tended = tended;
    }

    @Override
    @Nullable
    public Harvest ripe(FarmWorld world, BlockPos pos) {
        BlockState state = world.state(pos);
        if (!tended.test(state) || !isRipe(state)) {
            return null;
        }
        return Harvest.single(pos, new ItemStack(state.getBlock().asItem()));
    }

    @Override
    public FarmCost harvestCost(BlockState state) {
        return FarmCost.HARVEST_CROP;
    }

    /** Farmland as it is; dirt and grass tilled into farmland; anything else holds no Plot. */
    @Override
    @Nullable
    public BlockState plotGround(BlockState ground) {
        if (ground.is(Blocks.FARMLAND)) {
            return ground;
        }
        return ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK) ? FARMLAND : null;
    }

    static boolean isRipe(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals(AGE)) {
                return state.getValue(age) >= age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
            }
        }
        return false;
    }
}
