package com.logistics.forestry.farm.gourd;

import com.logistics.LogisticsForestry;
import com.logistics.forestry.farm.FarmCost;
import com.logistics.forestry.farm.FarmUpkeep;
import com.logistics.forestry.farm.FarmWorld;
import com.logistics.forestry.farm.TendLogic;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Gourd Farm: tends pumpkin and melon stems. Its Plots are the stems, replanted from their seeds.
 * A stem attached to its fruit is ripe, and harvesting it breaks the fruit only: the stem stays
 * standing, so the Plot stays planted and grows the next fruit itself. Only a stem standing on
 * {@code logistics:forestry/gourd_farm_ground} is tended.
 *
 * <p>The fruit is the stem's produce wherever the stem points, so a fruit one block outside the
 * Reach is harvested when its stem is inside it, and a fruit inside the Reach whose stem is outside
 * is left alone.
 */
public final class GourdTendLogic implements TendLogic {

    /** One gourd: its growing stem (planted from the seeds), the stem attached to a fruit, and the fruit. */
    record Gourd(Block stem, Block attached, Block fruit) {
        ItemStack seeds() {
            return new ItemStack(stem.asItem());
        }
    }

    static final List<Gourd> GOURDS = List.of(
            new Gourd(Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM, Blocks.PUMPKIN),
            new Gourd(Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM, Blocks.MELON));

    /** A till's result: dry farmland. */
    private static final BlockState FARMLAND = Blocks.FARMLAND.defaultBlockState();

    private static final Set<FarmCost> ACTIONS = EnumSet.of(FarmCost.HARVEST_CROP, FarmCost.TILL, FarmCost.PLANT);

    private final Predicate<BlockState> ground;

    public GourdTendLogic() {
        this(state -> state.is(LogisticsForestry.TAG.GOURD_FARM_GROUND));
    }

    /** Test seam: {@code ground} stands in for the {@code logistics:forestry/gourd_farm_ground} block tag. */
    public GourdTendLogic(Predicate<BlockState> ground) {
        this.ground = ground;
    }

    /** A stem attached to its own fruit: the fruit is the harvest, the stem the Plot. */
    @Override
    @Nullable
    public Harvest ripe(FarmWorld world, BlockPos pos) {
        BlockState state = world.state(pos);
        Gourd gourd = gourdOf(state);
        if (gourd == null || !state.is(gourd.attached()) || !standsOnGround(world, pos)) {
            return null;
        }
        BlockPos fruit = pos.relative(state.getValue(AttachedStemBlock.FACING));
        if (!world.isLoaded(fruit) || !world.state(fruit).is(gourd.fruit())) {
            return null;
        }
        return new Harvest(pos, List.of(fruit), gourd.seeds());
    }

    /** A stem on Gourd Farm ground that has no fruit to harvest yet. */
    @Override
    @Nullable
    public ItemStack growing(FarmWorld world, BlockPos pos) {
        Gourd gourd = gourdOf(world.state(pos));
        if (gourd == null || !standsOnGround(world, pos)) {
            return null;
        }
        return gourd.seeds();
    }

    /** The stem grown from {@code germling}, attached to a fruit or not. */
    @Override
    public boolean isPlanted(BlockState occupant, ItemStack germling) {
        Gourd gourd = gourdOf(occupant);
        return gourd != null && germling.is(gourd.stem().asItem());
    }

    @Override
    public FarmUpkeep upkeep() {
        return FarmUpkeep.GOURD_FARM;
    }

    @Override
    public Set<FarmCost> actions() {
        return ACTIONS;
    }

    @Override
    public FarmCost harvestCost(BlockState state) {
        return FarmCost.HARVEST_CROP;
    }

    /** Farmland as it is; other Gourd Farm ground tilled into farmland; anything else holds no Plot. */
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

    /** The gourd whose stem, attached or not, {@code state} is; {@code null} for anything else. */
    @Nullable
    static Gourd gourdOf(BlockState state) {
        for (Gourd gourd : GOURDS) {
            if (state.is(gourd.stem()) || state.is(gourd.attached())) {
                return gourd;
            }
        }
        return null;
    }
}
