package com.logistics.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal {@link MachineContext} for unit tests: no live {@link Level}, registries from the
 * bootstrapped built-in registry, and no-op world mutations. Records whether {@code markChanged}
 * was called so tests can assert dirty-marking. Hosted at the origin as air unless a position and
 * state are given.
 */
public final class FakeMachineContext implements MachineContext {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final RandomSource random = RandomSource.create(42L);
    private final BlockPos pos;
    private final BlockState state;
    public int changedCalls;
    public boolean workEnabled = true;

    public FakeMachineContext() {
        this(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
    }

    public FakeMachineContext(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
    }

    @Override
    @Nullable
    public Level level() {
        return null;
    }

    @Override
    public BlockPos pos() {
        return pos;
    }

    @Override
    public BlockState blockState() {
        return state;
    }

    @Override
    public HolderLookup.Provider registries() {
        return registries;
    }

    @Override
    public void markChanged() {
        changedCalls++;
    }

    @Override
    public void sync() {}

    @Override
    public void setBlockState(BlockState state, int flags) {}

    @Override
    @Nullable
    public RecipeManager recipeManager() {
        return null;
    }

    @Override
    public RandomSource random() {
        return random;
    }

    @Override
    public boolean workEnabled() {
        return workEnabled;
    }
}
