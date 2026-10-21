package com.logistics.forestry.farm;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What sets one tend farm type apart: what it counts as ripe, how that is harvested, and what
 * replants it. The {@link TendFarmComponent} asks about one position of its Reach at a time and
 * owns everything else: power, the inventory, spills, redstone and claim protection.
 *
 * <p>One stateless instance per farm block ({@link FarmMachineBlock#tendLogic()}), shared by every
 * farm of that type. A logic only reads the world, through {@link FarmWorld}.
 */
public interface TendLogic {

    /**
     * The ripe produce this farm tends at {@code pos}, as the harvest that takes it; {@code null}
     * when there is none. Only called while {@code pos} is loaded; reads beyond it should check
     * {@link FarmWorld#isLoaded}.
     */
    @Nullable
    Harvest ripe(FarmWorld world, BlockPos pos);

    /**
     * What is left to take of {@code harvest}, part broken, its blocks having changed since it was
     * planned or the farm having been reloaded; {@code null} when nothing of it is left. The result
     * keeps {@code harvest}'s plot and germling. Defaults to {@link #ripe} at the plot.
     */
    @Nullable
    default Harvest resume(FarmWorld world, Harvest harvest) {
        Harvest again = ripe(world, harvest.plot());
        return again != null ? new Harvest(harvest.plot(), again.blocks(), harvest.germling()) : null;
    }

    /** What breaking {@code state} as part of a harvest costs. */
    FarmCost harvestCost(BlockState state);

    /**
     * The block state planting {@code germling} at {@code plot} places, or {@code null} when it
     * cannot be planted there. Defaults to the germling's block in its default state.
     */
    @Nullable
    default BlockState plantState(ItemStack germling, BlockPos plot, FarmWorld world) {
        return germling.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : null;
    }

    /**
     * The ground a Plot's germling grows on, made ready from {@code ground}, the block under the Plot:
     * {@code ground} itself when it is ready, the block the farm repairs it into (charged as a
     * {@link FarmCost#TILL}) when it can be repaired, or {@code null} when it cannot hold the Plot,
     * which the farm then forgets.
     */
    @Nullable
    BlockState plotGround(BlockState ground);

    /**
     * Whether {@code occupant} is the plant {@code germling} grows into, so that a Plot it stands on
     * still holds its own plant. Defaults to the germling's block, at any age.
     */
    default boolean isPlanted(BlockState occupant, ItemStack germling) {
        return germling.getItem() instanceof BlockItem blockItem && occupant.is(blockItem.getBlock());
    }

    /**
     * One harvest: break {@code blocks} in order, then replant {@code plot} with one {@code germling}.
     * Leftover drops spill at {@code plot}. A multi-block list (a tree) is broken across ticks and
     * must be ordered so that breaking it in order never leaves a block unsupported.
     */
    record Harvest(BlockPos plot, List<BlockPos> blocks, ItemStack germling) {
        public Harvest {
            plot = plot.immutable();
            blocks = blocks.stream().map(BlockPos::immutable).toList();
            Objects.requireNonNull(germling);
            germling = germling.isEmpty() ? ItemStack.EMPTY : germling.copyWithCount(1);
        }

        /** Harvests the single block at {@code plot}. */
        public static Harvest single(BlockPos plot, ItemStack germling) {
            return new Harvest(plot, List.of(plot), germling);
        }

        /** Whether {@code stack} is the germling that replants this harvest's plot. */
        public boolean replants(ItemStack stack) {
            return !germling.isEmpty() && ItemStack.isSameItemSameComponents(stack, germling);
        }
    }
}
