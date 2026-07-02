package com.logistics.fabric.fluids;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * A water-like custom fluid. One flag-based class covers both the source and flowing forms (they differ
 * only by the {@code LEVEL} state and amount). Tank-only fluids supply a {@code null} block, so
 * {@link #createLegacyBlock} returns air and they never appear in the world; crude oil supplies a
 * {@link LiquidBlock} and viscous flow tuning. The source/flowing pair and block are supplied lazily so
 * each can reference the others before all are registered.
 */
public class FabricLogisticsFluid extends FlowingFluid {

    private final boolean source;
    private final Supplier<FlowingFluid> sourceFluid;
    private final Supplier<FlowingFluid> flowingFluid;
    private final Supplier<Item> bucket;
    private final Supplier<LiquidBlock> block;
    private final int slopeFindDistance;
    private final int dropOff;
    private final int tickDelay;

    public FabricLogisticsFluid(boolean source, Supplier<FlowingFluid> sourceFluid,
            Supplier<FlowingFluid> flowingFluid, Supplier<Item> bucket, Supplier<LiquidBlock> block,
            int slopeFindDistance, int dropOff, int tickDelay) {
        this.source = source;
        this.sourceFluid = sourceFluid;
        this.flowingFluid = flowingFluid;
        this.bucket = bucket;
        this.block = block;
        this.slopeFindDistance = slopeFindDistance;
        this.dropOff = dropOff;
        this.tickDelay = tickDelay;
    }

    @Override
    public Fluid getFlowing() {
        return flowingFluid.get();
    }

    @Override
    public Fluid getSource() {
        return sourceFluid.get();
    }

    @Override
    public Item getBucket() {
        Item b = bucket.get();
        return b != null ? b : Items.BUCKET;
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder);
        if (!source) {
            builder.add(LEVEL);
        }
    }

    @Override
    public int getAmount(FluidState state) {
        return source ? 8 : state.getValue(LEVEL);
    }

    @Override
    public boolean isSource(FluidState state) {
        return source;
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {}

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return slopeFindDistance;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return dropOff;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return tickDelay;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        LiquidBlock b = block.get();
        return b != null
                ? b.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state))
                : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return direction == Direction.DOWN && other != getSource() && other != getFlowing();
    }
}
