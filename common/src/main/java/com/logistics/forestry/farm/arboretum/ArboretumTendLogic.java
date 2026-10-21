package com.logistics.forestry.farm.arboretum;

import com.logistics.LogisticsForestry;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.machine.area.Columns;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.TendLogic;
import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.FellPlanner;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.forestry.logic.TreeView;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Arboretum: fells a tree whose trunk stands directly on Arboretum ground
 * ({@code logistics:forestry/arboretum_ground}, Humus) and carries natural leaves, and replants a
 * sapling of the same kind there. Trees on any other ground, wood with no natural leaves (a log
 * pillar, a hand-built tree) and the ground itself are never touched; the Arboretum never lays or
 * repairs its ground.
 *
 * <p>A tree's wood is taken only from its trunk's own column, so wood beside it, a neighbouring
 * trunk or a log build, is never pulled in. Its canopy is taken within
 * {@link TreeFeller.Limits#CANOPY_MARGIN} columns of the trunk, by the feller's nearest-wood rule,
 * and the whole tree is felled however far above the Reach it reaches, within the feller's limits.
 *
 * <p>The germling is the {@code #saplings} item named after the tree's leaves:
 * {@code <kind>_sapling}, {@code <kind>_propagule} or {@code <kind>} for {@code <kind>_leaves}.
 */
public final class ArboretumTendLogic implements TendLogic {

    private static final String LEAVES_SUFFIX = "_leaves";
    private static final List<String> GERMLING_SUFFIXES = List.of("_sapling", "_propagule", "");

    private final Predicate<BlockState> ground;
    private final Predicate<ItemStack> germlings;

    public ArboretumTendLogic() {
        this(state -> state.is(LogisticsForestry.TAG.ARBORETUM_GROUND), stack -> stack.is(ItemTags.SAPLINGS));
    }

    /**
     * Test seam: {@code ground} stands in for the {@code logistics:forestry/arboretum_ground} block
     * tag and {@code germlings} for the {@code #saplings} item tag.
     */
    public ArboretumTendLogic(Predicate<BlockState> ground, Predicate<ItemStack> germlings) {
        this.ground = ground;
        this.germlings = germlings;
    }

    @Override
    @Nullable
    public Harvest ripe(FarmWorld world, BlockPos pos) {
        if (!standsOnGround(world, pos)) {
            return null;
        }
        FellPlan tree = world.fellPlan(pos, limits(pos));
        return tree.isFound() ? new Harvest(pos, tree.blocks(), germling(world, tree.blocks())) : null;
    }

    /** What is left of the tree on {@code harvest}'s trunk, its canopy perhaps already gone. */
    @Override
    @Nullable
    public Harvest resume(FarmWorld world, Harvest harvest) {
        BlockPos plot = harvest.plot();
        if (!standsOnGround(world, plot)) {
            return null;
        }
        FellPlan rest = new FellPlanner(world.treeView(), plot, limits(plot), false).step(Integer.MAX_VALUE);
        return rest != null && rest.isFound() ? new Harvest(plot, rest.blocks(), harvest.germling()) : null;
    }

    @Override
    public FarmCost harvestCost(BlockState state) {
        return isCanopy(state) ? FarmCost.HARVEST_LEAVES : FarmCost.HARVEST_LOG;
    }

    /** Arboretum ground as it is; anything else holds no Plot. */
    @Override
    @Nullable
    public BlockState plotGround(BlockState state) {
        return ground.test(state) ? state : null;
    }

    private boolean standsOnGround(FarmWorld world, BlockPos pos) {
        if (world.treeKind(world.state(pos)) != TreeView.Kind.WOOD) {
            return false;
        }
        BlockPos foot = pos.below();
        return world.isLoaded(foot) && ground.test(world.state(foot));
    }

    /** Wood in the trunk's column only; canopy around it. */
    private static TreeFeller.Limits limits(BlockPos trunk) {
        Columns column = Columns.square(trunk, 1);
        return new TreeFeller.Limits(
                column,
                column.grow(TreeFeller.Limits.CANOPY_MARGIN),
                0,
                TreeFeller.Limits.ABOVE,
                TreeFeller.Limits.CAP);
    }

    private static boolean isCanopy(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || state.is(Blocks.MANGROVE_PROPAGULE);
    }

    /** The germling named after the first leaves of {@code tree}; empty when there is none. */
    private ItemStack germling(FarmWorld world, List<BlockPos> tree) {
        for (BlockPos pos : tree) {
            BlockState state = world.state(pos);
            if (state.getBlock() instanceof LeavesBlock) {
                return germlingFor(state.getBlock());
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack germlingFor(Block leaves) {
        var key = BuiltInRegistries.BLOCK.getKey(leaves);
        String path = key.getPath();
        if (!path.endsWith(LEAVES_SUFFIX)) {
            return ItemStack.EMPTY;
        }
        String kind = path.substring(0, path.length() - LEAVES_SUFFIX.length());
        for (String suffix : GERMLING_SUFFIXES) {
            Item item = BuiltInRegistries.ITEM
                    .getOptional(ResourceId.in(key.getNamespace(), kind + suffix).toIdentifier())
                    .orElse(null);
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                if (germlings.test(stack)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
