package com.logistics.forestry.farm.cocoa;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmUpkeep;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.TendLogic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Cocoa Plantation: picks a cocoa pod once it reaches its maximum age, and replants a bean on
 * the same log. Only a pod hanging on a {@code logistics:forestry/cocoa_plantation_ground} log is
 * tended; the logs are the player's, and the farm never places, breaks or replaces them.
 *
 * <p>A pod hangs beside its log rather than standing on it, so the Plot's {@link #support} is the
 * log the pod faces, or for an Empty Plot the first tended log beside it, north, east, south then
 * west; {@link #plantState} faces the new pod towards that log.
 */
public final class CocoaTendLogic implements TendLogic {

    private final Predicate<BlockState> ground;

    public CocoaTendLogic() {
        this(state -> state.is(LogisticsForestry.TAG.COCOA_PLANTATION_GROUND));
    }

    /** Test seam: {@code ground} stands in for the {@code logistics:forestry/cocoa_plantation_ground} block tag. */
    public CocoaTendLogic(Predicate<BlockState> ground) {
        this.ground = ground;
    }

    @Override
    @Nullable
    public Harvest ripe(FarmWorld world, BlockPos pos) {
        BlockState state = world.state(pos);
        if (!isPod(state) || !isRipe(state) || !hangsOnGround(world, pos, state)) {
            return null;
        }
        return Harvest.single(pos, germling(state));
    }

    /** An unripe pod on a tended log. */
    @Override
    @Nullable
    public ItemStack growing(FarmWorld world, BlockPos pos) {
        BlockState state = world.state(pos);
        if (!isPod(state) || isRipe(state) || !hangsOnGround(world, pos, state)) {
            return null;
        }
        return germling(state);
    }

    /** The log a pod at {@code plot} faces; with no pod there, the first tended log beside it. */
    @Override
    @Nullable
    public BlockPos support(FarmWorld world, BlockPos plot) {
        BlockState state = world.state(plot);
        if (isPod(state)) {
            return plot.relative(state.getValue(CocoaBlock.FACING));
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = plot.relative(side);
            if (world.isLoaded(beside) && ground.test(world.state(beside))) {
                return beside;
            }
        }
        return null;
    }

    /** The pods hanging on a tended log at {@code pos}, so a pod just outside the Reach on a log inside it is tended. */
    @Override
    public Collection<BlockPos> attached(FarmWorld world, BlockPos pos) {
        if (!ground.test(world.state(pos))) {
            return List.of();
        }
        List<BlockPos> pods = new ArrayList<>(4);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = pos.relative(side);
            if (!world.isLoaded(beside)) {
                continue;
            }
            BlockState state = world.state(beside);
            if (isPod(state) && state.getValue(CocoaBlock.FACING) == side.getOpposite()) {
                pods.add(beside);
            }
        }
        return pods;
    }

    /** A pod facing the Plot's {@link #support}; {@code null} when it has none or the germling is not cocoa. */
    @Override
    @Nullable
    public BlockState plantState(ItemStack germling, BlockPos plot, FarmWorld world) {
        if (!(germling.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof CocoaBlock)) {
            return null;
        }
        BlockPos log = support(world, plot);
        if (log == null) {
            return null;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (plot.relative(side).equals(log)) {
                return item.getBlock().defaultBlockState().setValue(CocoaBlock.FACING, side);
            }
        }
        return null;
    }

    @Override
    public FarmUpkeep upkeep() {
        return FarmUpkeep.COCOA_PLANTATION;
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

    /** A tended log as it is; anything else holds no Plot. The farm never repairs a log. */
    @Override
    @Nullable
    public BlockState plotGround(BlockState state) {
        return ground.test(state) ? state : null;
    }

    private boolean hangsOnGround(FarmWorld world, BlockPos pos, BlockState pod) {
        BlockPos log = pos.relative(pod.getValue(CocoaBlock.FACING));
        return world.isLoaded(log) && ground.test(world.state(log));
    }

    private static boolean isPod(BlockState state) {
        return state.getBlock() instanceof CocoaBlock;
    }

    static boolean isRipe(BlockState state) {
        return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
    }

    private static ItemStack germling(BlockState pod) {
        return new ItemStack(pod.getBlock().asItem());
    }
}
