package com.logistics.core.lib.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The cross-version seam for vanilla plant APIs: the only place the mod calls
 * {@link BonemealableBlock} or reproduces a berry harvest, so callers stay identical across branches.
 *
 * <p>mc/26.x passes a {@code BonemealSource} to every {@link BonemealableBlock} method; mc/1.21.11
 * has none. Berries come from the block-interact loot tables on both, but mc/1.21.11's
 * {@code Block.dropFromBlockInteractLootTable} is protected, so the roll is reproduced here with the
 * same loot parameters. Per branch, only this file carries the matching calls.
 *
 * <p>Nothing here plays sounds or spawns particles; callers decide on feedback.
 */
public final class PlantCompat {

    private PlantCompat() {}

    /** Whether bone meal would do anything to {@code state} at {@code pos}. */
    public static boolean canFertilize(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof BonemealableBlock block
                && block.isValidBonemealTarget(level, pos, state);
    }

    /**
     * Applies one bone-meal step as a dispenser would, minus the bone meal and the feedback.
     *
     * @return {@code true} if a growth step was performed; {@code false} if the target cannot be
     *     fertilized or the block's success roll failed
     */
    public static boolean fertilize(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!canFertilize(level, pos, state)) {
            return false;
        }
        BonemealableBlock block = (BonemealableBlock) state.getBlock();
        if (!block.isBonemealSuccess(level, random, pos, state)) {
            return false;
        }
        block.performBonemeal(level, random, pos, state);
        return true;
    }

    /**
     * Picks a sweet berry bush (age 2 or more) or cave vines bearing glow berries the way a player
     * does: rolls the same drops, resets the plant (bush to age 1, vines to no berries) and returns
     * the drops instead of spawning them.
     *
     * @return the picked stacks; empty, with the plant unchanged, if it has nothing to pick
     */
    public static List<ItemStack> harvestBerries(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            if (state.getValue(SweetBerryBushBlock.AGE) <= 1) {
                return List.of();
            }
            return pick(level, pos, state, BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH,
                    state.setValue(SweetBerryBushBlock.AGE, 1));
        }
        if (state.getBlock() instanceof CaveVines && CaveVines.hasGlowBerries(state)) {
            return pick(level, pos, state, BuiltInLootTables.HARVEST_CAVE_VINE,
                    state.setValue(CaveVines.BERRIES, false));
        }
        return List.of();
    }

    private static List<ItemStack> pick(
            ServerLevel level, BlockPos pos, BlockState state, ResourceKey<LootTable> lootTable, BlockState picked) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(pos))
                .create(LootContextParamSets.BLOCK_INTERACT);
        List<ItemStack> drops = new ArrayList<>(
                level.getServer().reloadableRegistries().getLootTable(lootTable).getRandomItems(params));
        level.setBlock(pos, picked, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(picked));
        return drops;
    }
}
