package com.logistics.pipe.ui;

import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.modules.SmartSplitterModule;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class FilterInventory implements Inventory {
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SmartSplitterModule.FILTER_ORDER.length * SmartSplitterModule.FILTER_SLOTS_PER_SIDE, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;
    private final SmartSplitterModule module;

    public FilterInventory(PipeBlockEntity pipeEntity) {
        this.pipeEntity = pipeEntity;
        this.module = getModuleFromPipe(pipeEntity);

        if(pipeEntity != null) {
            loadFromBlockEntity();
        }
    }

    private SmartSplitterModule getModuleFromPipe(PipeBlockEntity entity) {
        if (entity == null) {
            return new SmartSplitterModule();
        }

        com.logistics.block.PipeBlock block = (com.logistics.block.PipeBlock) entity.getCachedState().getBlock();
        com.logistics.pipe.Pipe pipe = block.getPipe();
        SmartSplitterModule pipeModule = pipe.getModule(SmartSplitterModule.class);

        return pipeModule != null ? pipeModule : new SmartSplitterModule();
    }

    @Override
    public int size() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setStack(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack existing = stacks.get(slot);
        setStack(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= stacks.size()) {
            return;
        }

        ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        stacks.set(slot, copy);

        if (pipeEntity != null) {
            syncToBlockEntity();
        }
    }

    @Override
    public void markDirty() {
        if (pipeEntity != null) {
            syncToBlockEntity();
        }
    }

    @Override
    public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        if (pipeEntity != null) {
            syncToBlockEntity();
        }
    }

    private void loadFromBlockEntity() {
        int slotIndex = 0;
        PipeContext ctx = pipeEntity.createContext();
        for (Direction direction : SmartSplitterModule.FILTER_ORDER) {
            List<String> slots = module.getFilterSlots(ctx, direction);
            for (int i = 0; i < SmartSplitterModule.FILTER_SLOTS_PER_SIDE; i++) {
                String id = slots.get(i);
                ItemStack stack = ItemStack.EMPTY;
                if (!id.isEmpty()) {
                    Identifier identifier = Identifier.tryParse(id);
                    if (identifier != null) {
                        Item item = Registries.ITEM.get(identifier);
                        if (item != Items.AIR) {
                            stack = new ItemStack(item);
                        }
                    }
                }
                stacks.set(slotIndex++, stack);
            }
        }
    }

    private void syncToBlockEntity() {
        World world = pipeEntity.getWorld();
        if (world == null || world.isClient()) {
            return;
        }

        int slotIndex = 0;
        PipeContext ctx = pipeEntity.createContext();
        for (Direction direction : SmartSplitterModule.FILTER_ORDER) {
            List<String> slots = new ArrayList<>(SmartSplitterModule.FILTER_SLOTS_PER_SIDE);
            for (int i = 0; i < SmartSplitterModule.FILTER_SLOTS_PER_SIDE; i++) {
                ItemStack stack = stacks.get(slotIndex++);
                if (stack.isEmpty()) {
                    slots.add("");
                } else {
                    Identifier id = Registries.ITEM.getId(stack.getItem());
                    slots.add(id.toString());
                }
            }
            module.setFilterSlots(ctx, direction, slots);
        }

        pipeEntity.markDirty();
        world.updateListeners(pipeEntity.getPos(), pipeEntity.getCachedState(), pipeEntity.getCachedState(), 3);
    }
}
