package com.logistics.forestry.farm.crop;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.TendLogic;
import com.logistics.forestry.logic.GrowthAge;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The Crop Farm: harvests a crop in {@code logistics:forestry/crop_farm_tends} once it reaches its
 * maximum age, and replants the same crop from the crop block's own item (wheat seeds for wheat).
 * Only a crop standing on {@code logistics:forestry/crop_farm_ground} is tended; one on any other
 * ground is left alone.
 *
 * <p>A {@link CropBlock} knows its maximum age; any other tagged block counts as ripe at the
 * highest value of its {@code age} property.
 */
public final class CropTendLogic implements TendLogic {

    /** A till's result: dry farmland. */
    private static final BlockState FARMLAND = Blocks.FARMLAND.defaultBlockState();

    private final Predicate<BlockState> tended;
    private final Predicate<BlockState> ground;

    public CropTendLogic() {
        this(
                state -> state.is(LogisticsForestry.TAG.CROP_FARM_TENDS),
                state -> state.is(LogisticsForestry.TAG.CROP_FARM_GROUND));
    }

    /**
     * Test seam: {@code tended} stands in for the {@code logistics:forestry/crop_farm_tends} block tag
     * and {@code ground} for {@code logistics:forestry/crop_farm_ground}.
     */
    public CropTendLogic(Predicate<BlockState> tended, Predicate<BlockState> ground) {
        this.tended = tended;
        this.ground = ground;
    }

    @Override
    @Nullable
    public Harvest ripe(FarmWorld world, BlockPos pos) {
        BlockState state = world.state(pos);
        if (!tended.test(state) || !isRipe(state) || !standsOnGround(world, pos)) {
            return null;
        }
        return Harvest.single(pos, new ItemStack(state.getBlock().asItem()));
    }

    @Override
    public FarmCost harvestCost(BlockState state) {
        return FarmCost.HARVEST_CROP;
    }

    /** Farmland as it is; other Crop Farm ground tilled into farmland; anything else holds no Plot. */
    @Override
    @Nullable
    public BlockState plotGround(BlockState state) {
        if (!ground.test(state)) {
            return null;
        }
        return state.is(Blocks.FARMLAND) ? state : FARMLAND;
    }

    private boolean standsOnGround(FarmWorld world, BlockPos pos) {
        BlockPos foot = pos.below();
        return world.isLoaded(foot) && ground.test(world.state(foot));
    }

    static boolean isRipe(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        IntegerProperty age = GrowthAge.of(state);
        return age != null && state.getValue(age) >= GrowthAge.max(age);
    }
}
