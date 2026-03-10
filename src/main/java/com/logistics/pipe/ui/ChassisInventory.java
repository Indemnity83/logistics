package com.logistics.pipe.ui;

import com.logistics.pipe.ChassisPipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;

/**
 * Real inventory for the Chassis Logistics Pipe GUI.
 * Holds up to {@link ChassisPipe#MAX_SLOTS} module slots backed by full ItemStacks
 * persisted in the pipe block entity's module state.
 */
public class ChassisInventory implements Container {
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(ChassisPipe.MAX_SLOTS, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;

    public ChassisInventory(PipeBlockEntity pipeEntity) {
        this.pipeEntity = pipeEntity;
        if (pipeEntity != null) {
            loadFromEntity();
        }
    }

    private void loadFromEntity() {
        PipeContext ctx = pipeEntity.createContext();
        var state = ctx.moduleState(ChassisPipe.STATE_KEY);
        for (int slot = 0; slot < ChassisPipe.MAX_SLOTS; slot++) {
            final int s = slot;
            Tag tag = state.get(String.valueOf(slot));
            if (tag != null) {
                ItemStack.CODEC.parse(NbtOps.INSTANCE, tag).result()
                        .ifPresent(stack -> items.set(s, stack));
            }
        }
    }

    private void saveToEntity() {
        if (pipeEntity == null) return;
        Level level = pipeEntity.getLevel();
        if (level == null || level.isClientSide()) return;

        PipeContext ctx = pipeEntity.createContext();
        var state = ctx.moduleState(ChassisPipe.STATE_KEY);
        for (int slot = 0; slot < ChassisPipe.MAX_SLOTS; slot++) {
            String key = String.valueOf(slot);
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                state.remove(key);
            } else {
                final String k = key;
                ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result()
                        .ifPresent(tag -> state.put(k, tag));
            }
        }
        ctx.markDirtyAndSync();
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int removed = Math.min(amount, existing.getCount());
        ItemStack result = existing.copyWithCount(removed);
        existing.shrink(removed);
        if (existing.isEmpty()) items.set(slot, ItemStack.EMPTY);
        saveToEntity();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        ItemStack existing = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        saveToEntity();
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) return;
        items.set(slot, stack);
        saveToEntity();
    }

    @Override
    public void setChanged() {
        saveToEntity();
    }

    @Override
    public boolean stillValid(Player player) {
        if (pipeEntity == null || pipeEntity.isRemoved()) return false;
        BlockPos pos = pipeEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        saveToEntity();
    }
}
