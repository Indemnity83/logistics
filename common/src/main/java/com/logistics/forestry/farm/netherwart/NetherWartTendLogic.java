package com.logistics.forestry.farm.netherwart;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmUpkeep;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.TendLogic;
import com.logistics.forestry.logic.GrowthAge;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * The Nether Wart Farm: harvests a plant in {@code logistics:forestry/nether_wart_farm_tends} once
 * its {@code age} reaches its highest value, and replants it from the plant block's own item. Only a
 * plant standing on {@code logistics:forestry/nether_wart_farm_ground} (soul sand) is tended; the
 * farm never lays or repairs that ground.
 */
public final class NetherWartTendLogic implements TendLogic {

    private final Predicate<BlockState> tended;
    private final Predicate<BlockState> ground;

    public NetherWartTendLogic() {
        this(
                state -> state.is(LogisticsForestry.TAG.NETHER_WART_FARM_TENDS),
                state -> state.is(LogisticsForestry.TAG.NETHER_WART_FARM_GROUND));
    }

    /**
     * Test seam: {@code tended} stands in for the {@code logistics:forestry/nether_wart_farm_tends}
     * block tag and {@code ground} for {@code logistics:forestry/nether_wart_farm_ground}.
     */
    public NetherWartTendLogic(Predicate<BlockState> tended, Predicate<BlockState> ground) {
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

    /** An unripe tended plant on its ground, replanted from the plant block's own item. */
    @Override
    @Nullable
    public ItemStack growing(FarmWorld world, BlockPos pos) {
        BlockState state = world.state(pos);
        if (!tended.test(state) || isRipe(state) || !standsOnGround(world, pos)) {
            return null;
        }
        return new ItemStack(state.getBlock().asItem());
    }

    @Override
    public FarmUpkeep upkeep() {
        return FarmUpkeep.NETHER_WART_FARM;
    }

    private static final Set<FarmCost> ACTIONS = EnumSet.of(FarmCost.HARVEST_CROP, FarmCost.PLANT);

    @Override
    public Set<FarmCost> actions() {
        return ACTIONS;
    }

    @Override
    public FarmCost harvestCost(BlockState state) {
        return FarmCost.HARVEST_CROP;
    }

    /** The ground as it is; anything else holds no Plot, since the farm never repairs it. */
    @Override
    @Nullable
    public BlockState plotGround(BlockState state) {
        return ground.test(state) ? state : null;
    }

    private boolean standsOnGround(FarmWorld world, BlockPos pos) {
        BlockPos foot = pos.below();
        return world.isLoaded(foot) && ground.test(world.state(foot));
    }

    static boolean isRipe(BlockState state) {
        IntegerProperty age = GrowthAge.of(state);
        return age != null && state.getValue(age) >= GrowthAge.max(age);
    }
}
