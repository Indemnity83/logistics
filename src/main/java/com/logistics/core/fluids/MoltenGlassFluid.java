package com.logistics.core.fluids;

import com.logistics.LogisticsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Molten glass fluid for Kiln.
 * A hot, glowing orange liquid created by melting glass/sand.
 *
 * <p>This is a tank-only fluid that doesn't flow in the world.
 * Used only in machine tanks for crafting valves.
 */
public abstract class MoltenGlassFluid extends FlowingFluid {

    @Override
    public Fluid getFlowing() {
        return LogisticsCore.FLUID.MOLTEN_GLASS_FLOWING;
    }

    @Override
    public Fluid getSource() {
        return LogisticsCore.FLUID.MOLTEN_GLASS_STILL;
    }

    @Override
    protected boolean canConvertToSource(Level level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        // No special behavior
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 0; // Doesn't flow
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 8; // Immediate drop-off (doesn't flow)
    }

    @Override
    public Item getBucket() {
        return Items.AIR; // No bucket
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return false;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 100; // Very slow (doesn't matter since it doesn't flow)
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState(); // Tank-only fluid, no world block
    }

    @Override
    public boolean isSource(FluidState state) {
        return false; // Overridden in subclasses
    }

    @Override
    public int getAmount(FluidState state) {
        return 0; // Overridden in subclasses
    }

    public static class Flowing extends MoltenGlassFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Still extends MoltenGlassFluid {
        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
