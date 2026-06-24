package com.logistics.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal {@link MachineContext} for unit tests: no live {@link Level}, registries from the
 * bootstrapped built-in registry, and no-op world mutations. Records whether {@code setChanged}
 * was called so tests can assert dirty-marking.
 */
public final class FakeMachineContext implements MachineContext {

    private final HolderLookup.Provider registries =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    public int changedCalls;

    @Override
    @Nullable
    public Level level() {
        return null;
    }

    @Override
    public BlockPos pos() {
        return BlockPos.ZERO;
    }

    @Override
    public BlockState blockState() {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public HolderLookup.Provider registries() {
        return registries;
    }

    @Override
    public void setChanged() {
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
}
