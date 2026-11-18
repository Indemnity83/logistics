package com.logistics.forestry.farm.plantation;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmUpkeep;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.TendLogic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Plantation: harvests a stalk plant ({@code logistics:forestry/plantation_stalks}: sugar cane,
 * bamboo, cactus) down to its base, which stays and grows again. Nothing is replanted after a
 * harvest: the base is the planted plant, and it keeps the Plot.
 *
 * <p>A base is a stalk block standing on Plantation ground
 * ({@code logistics:forestry/plantation_ground}). Its harvest is every stalk block stacked on it,
 * however far above the Reach the stack reaches, plus one topping
 * ({@code logistics:forestry/plantation_toppings}, the cactus flower) crowning the stack, broken from
 * the top down. A base with nothing on it is growing. A stalk block above a base is never a base.
 *
 * <p>The germling is the base's own item; a bamboo shoot's is bamboo, which is planted back as a
 * shoot. The Plantation never builds water channels, lays sand or repairs its ground.
 */
public final class PlantationTendLogic implements TendLogic {

    /** The most blocks a harvest takes above its base; vanilla bamboo stops at 15. */
    static final int MAX_STALK = 31;

    private static final Set<FarmCost> ACTIONS = EnumSet.of(FarmCost.HARVEST_CROP, FarmCost.PLANT);

    private final Predicate<BlockState> stalks;
    private final Predicate<BlockState> toppings;
    private final Predicate<BlockState> ground;

    public PlantationTendLogic() {
        this(
                state -> state.is(LogisticsForestry.TAG.PLANTATION_STALKS),
                state -> state.is(LogisticsForestry.TAG.PLANTATION_TOPPINGS),
                state -> state.is(LogisticsForestry.TAG.PLANTATION_GROUND));
    }

    /**
     * Test seam: {@code stalks}, {@code toppings} and {@code ground} stand in for the
     * {@code logistics:forestry/plantation_stalks}, {@code plantation_toppings} and
     * {@code plantation_ground} block tags.
     */
    public PlantationTendLogic(
            Predicate<BlockState> stalks, Predicate<BlockState> toppings, Predicate<BlockState> ground) {
        this.stalks = stalks;
        this.toppings = toppings;
        this.ground = ground;
    }

    @Override
    @Nullable
    public Harvest ripe(FarmWorld world, BlockPos pos) {
        BlockState base = world.state(pos);
        if (!isBase(world, pos, base)) {
            return null;
        }
        List<BlockPos> above = above(world, pos);
        return above.isEmpty() ? null : new Harvest(pos, above, germlingFor(base));
    }

    /** A base with nothing on it yet. */
    @Override
    @Nullable
    public ItemStack growing(FarmWorld world, BlockPos pos) {
        BlockState base = world.state(pos);
        if (!isBase(world, pos, base) || !above(world, pos).isEmpty()) {
            return null;
        }
        return germlingFor(base);
    }

    @Override
    public FarmUpkeep upkeep() {
        return FarmUpkeep.PLANTATION;
    }

    @Override
    public Set<FarmCost> actions() {
        return ACTIONS;
    }

    @Override
    public FarmCost harvestCost(BlockState state) {
        return FarmCost.HARVEST_CROP;
    }

    /** Bamboo is planted as a shoot, as placing it by hand does; anything else as its own block. */
    @Override
    @Nullable
    public BlockState plantState(ItemStack germling, BlockPos plot, FarmWorld world) {
        if (germling.is(Items.BAMBOO)) {
            return Blocks.BAMBOO_SAPLING.defaultBlockState();
        }
        return TendLogic.super.plantState(germling, plot, world);
    }

    /** Plantation ground as it is; anything else holds no Plot. */
    @Override
    @Nullable
    public BlockState plotGround(BlockState state) {
        return ground.test(state) ? state : null;
    }

    /** A stalk grown from {@code germling}: a bamboo shoot counts as bamboo. */
    @Override
    public boolean isPlanted(BlockState occupant, ItemStack germling) {
        return stalks.test(occupant) && ItemStack.isSameItem(germlingFor(occupant), germling);
    }

    /** A stalk block on Plantation ground; a stalk block under it never counts as ground. */
    private boolean isBase(FarmWorld world, BlockPos pos, BlockState state) {
        if (!stalks.test(state) || germlingFor(state).isEmpty()) {
            return false;
        }
        BlockPos foot = pos.below();
        if (!world.isLoaded(foot)) {
            return false;
        }
        BlockState below = world.state(foot);
        return !stalks.test(below) && ground.test(below);
    }

    /** The stalk blocks stacked on {@code base} and the topping crowning them, top first. */
    private List<BlockPos> above(FarmWorld world, BlockPos base) {
        List<BlockPos> blocks = new ArrayList<>();
        BlockPos pos = base.above();
        while (blocks.size() < MAX_STALK && world.isLoaded(pos) && stalks.test(world.state(pos))) {
            blocks.add(pos);
            pos = pos.above();
        }
        if (blocks.size() < MAX_STALK && world.isLoaded(pos) && toppings.test(world.state(pos))) {
            blocks.add(pos);
        }
        Collections.reverse(blocks);
        return blocks;
    }

    private static ItemStack germlingFor(BlockState stalk) {
        if (stalk.is(Blocks.BAMBOO_SAPLING)) {
            return new ItemStack(Items.BAMBOO);
        }
        return new ItemStack(stalk.getBlock().asItem());
    }
}
