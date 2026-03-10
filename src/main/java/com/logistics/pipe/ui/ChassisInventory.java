package com.logistics.pipe.ui;

import com.logistics.pipe.ChassisPipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.item.ModuleItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

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

    /** When a module item is inserted, copy its stored CustomData → block entity module state. */
    private void syncStateFromItem(ItemStack stack) {
        if (!(stack.getItem() instanceof ModuleItem moduleItem)) return;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        PipeContext ctx = pipeEntity.createContext();
        String stateKey = moduleItem.createModule().getStateKey();
        CompoundTag state = ctx.moduleState(stateKey);
        CompoundTag itemState = customData.copyTag();
        for (String key : itemState.keySet()) {
            Tag value = itemState.get(key);
            if (value != null) state.put(key, value);
        }
    }

    /** Call the module's onDetach lifecycle hook so it can clean up network registrations. */
    private void detachModule(ItemStack stack) {
        if (pipeEntity == null) return;
        Level level = pipeEntity.getLevel();
        if (level == null || level.isClientSide()) return;
        if (!(stack.getItem() instanceof ModuleItem moduleItem)) return;
        moduleItem.createModule().onDetach(pipeEntity.createContext());
    }

    /** Before returning a module item, copy the block entity module state → item's CustomData. */
    private void syncStateToItem(ItemStack stack) {
        if (!(stack.getItem() instanceof ModuleItem moduleItem)) return;
        PipeContext ctx = pipeEntity.createContext();
        String stateKey = moduleItem.createModule().getStateKey();
        CompoundTag state = ctx.moduleState(stateKey);
        if (!state.isEmpty()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(state.copy()));
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

        // Sync current module state back into each item before persisting
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                syncStateToItem(stack);
            }
        }

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
        if (pipeEntity != null) {
            syncStateToItem(result);
        }
        existing.shrink(removed);
        if (existing.isEmpty()) {
            detachModule(result);
            items.set(slot, ItemStack.EMPTY);
        }
        saveToEntity();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        ItemStack existing = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) return;
        ItemStack previous = items.get(slot);
        if (!previous.isEmpty() && (stack.isEmpty() || !ItemStack.isSameItem(previous, stack))) {
            detachModule(previous);
        }
        items.set(slot, stack);
        if (!stack.isEmpty() && pipeEntity != null) {
            syncStateFromItem(stack);
        }
        saveToEntity();

        // When inserting a module, force onConnectionsChanged to re-fire on the next tick.
        // The module was just saved, but the connection cache is already clean (no physical
        // neighbor change occurred), so without this the new module's sinkDirection stays null.
        if (!stack.isEmpty() && pipeEntity != null) {
            Level level = pipeEntity.getLevel();
            if (level != null && !level.isClientSide()) {
                pipeEntity.setLastConnectionsMask(-1);
                pipeEntity.invalidateConnectionCache();
            }
        }
    }

    @Override
    public void setChanged() {
        saveToEntity();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (ItemStack stack : items) {
            detachModule(stack);
        }
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        saveToEntity();
    }
}
