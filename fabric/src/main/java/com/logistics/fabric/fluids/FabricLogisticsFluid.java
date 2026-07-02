package com.logistics.fabric.fluids;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.core.Direction;

/**
 * A no-world, water-like fluid for tanks/pipes/buckets. One flag-based class covers both the source and
 * flowing forms (they differ only by the {@code LEVEL} state and amount). Never placed in the world, so
 * {@link #createLegacyBlock} returns air. The source/flowing pair is supplied lazily so each can reference
 * the other before both are registered.
 */
public class FabricLogisticsFluid extends FlowingFluid {

    private final boolean source;
    private final Supplier<FlowingFluid> sourceFluid;
    private final Supplier<FlowingFluid> flowingFluid;
    private final Supplier<Item> bucket;

    public FabricLogisticsFluid(boolean source, Supplier<FlowingFluid> sourceFluid,
            Supplier<FlowingFluid> flowingFluid, Supplier<Item> bucket) {
        this.source = source;
        this.sourceFluid = sourceFluid;
        this.flowingFluid = flowingFluid;
        this.bucket = bucket;
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
    protected boolean canConvertToSource(Level level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {}

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 4;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 1;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return direction == Direction.DOWN && other != getSource() && other != getFlowing();
    }
}
