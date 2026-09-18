package com.logistics.automation.laserquarry.entity;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Stateless helper for the laser quarry's block breaking: computes a target block's drops, removes
 * the block, and hands the drops (and any container-spawned items) to a {@link QuarryOutput} sink.
 * Output routing lives in {@link QuarryOutput}.
 */
public final class QuarryBlockBreaker {

    private QuarryBlockBreaker() {}

    public static void mineBlock(ServerLevel world, BlockPos target, BlockState targetState, QuarryOutput output) {
        BlockEntity blockEntity = world.getBlockEntity(target);
        List<ItemStack> drops = Block.getDrops(targetState, world, target, blockEntity, null, ItemStack.EMPTY);

        // A container spills its contents as loose items instead of returning them from getDrops, so
        // those have to be swept off the ground afterwards. Only what this break spawns belongs to
        // the quarry, so record what was lying there first: plenty of block entities spill nothing
        // (signs, beds, spawners), and a container can stand next to items already on the ground.
        Set<Integer> alreadyOnGround = blockEntity == null ? Set.of() : output.itemsNear(world, target);

        // Break the block without natural drops so we can route them ourselves.
        removeBlock(world, target, targetState, blockEntity);

        for (ItemStack drop : drops) {
            output.accept(world, drop);
        }

        if (blockEntity != null) {
            output.sweepNearby(world, target, alreadyOnGround);
        }
    }

    /**
     * Clears the block, running its removal side effects -- a container spilling its contents, a
     * pipe dropping what it was carrying -- exactly once, driven from the block entity already in
     * hand.
     *
     * <p>This is {@code Level.destroyBlock(target, false)} with the block entity side effects
     * suppressed and run here instead. Vanilla runs them from inside its own removal path off a
     * <em>second</em> block entity lookup, and skips them silently when that lookup comes back
     * null; the block is cleared either way, so a broken container's contents cease to exist.
     * Driving the hook from a reference we already hold removes that dependency.
     *
     * <p>Vanilla's call is suppressed rather than left to run as well, because the hook is not
     * idempotent: a lectern re-issues its book, a furnace re-issues its experience, and a pipe
     * re-drops everything it was carrying. Going through the hook rather than emptying containers
     * directly is what keeps a shulker box intact -- it overrides the hook to do nothing, because
     * the item it drops already carries its contents.
     */
    private static void removeBlock(
            ServerLevel world, BlockPos target, BlockState targetState, BlockEntity blockEntity) {
        if (targetState.isAir()) {
            return;
        }

        BlockState leftBehind = world.getFluidState(target).createLegacyBlock();

        if (blockEntity != null) {
            blockEntity.preRemoveSideEffects(target, targetState);
        }

        if (!(targetState.getBlock() instanceof BaseFireBlock)) {
            world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, target, Block.getId(targetState));
        }

        if (world.setBlock(target, leftBehind, Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS)) {
            world.gameEvent(GameEvent.BLOCK_DESTROY, target, GameEvent.Context.of(null, targetState));
        }
    }
}
