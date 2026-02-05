package com.logistics.pipe.ui;

import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ItemFilterModule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FilterInventory implements Container {
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(
            ItemFilterModule.FILTER_ORDER.length * ItemFilterModule.FILTER_SLOTS_PER_SIDE, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;
    private final ItemFilterModule module;

    public FilterInventory(PipeBlockEntity pipeEntity) {
        this.pipeEntity = pipeEntity;
        this.module = getModuleFromPipe(pipeEntity);

        if (pipeEntity != null) {
            loadFromBlockEntity();
        }
    }

    private ItemFilterModule getModuleFromPipe(PipeBlockEntity entity) {
        if (entity == null) {
            return new ItemFilterModule();
        }

        com.logistics.pipe.block.PipeBlock block =
                (com.logistics.pipe.block.PipeBlock) entity.getBlockState().getBlock();
        com.logistics.pipe.Pipe pipe = block.getPipe();
        ItemFilterModule pipeModule = pipe.getModule(ItemFilterModule.class);

        return pipeModule != null ? pipeModule : new ItemFilterModule();
    }

    @Override
    public int getContainerSize() {
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
    public ItemStack getItem(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setItem(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack existing = stacks.get(slot);
        setItem(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
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
    public void setChanged() {
        if (pipeEntity != null) {
            syncToBlockEntity();
        }
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
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
        for (Direction direction : ItemFilterModule.FILTER_ORDER) {
            List<String> slots = module.getFilterSlots(ctx, direction);
            for (int i = 0; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
                String id = slots.get(i);
                ItemStack stack = ItemStack.EMPTY;
                if (!id.isEmpty()) {
                    Identifier identifier = Identifier.tryParse(id);
                    if (identifier != null) {
                        var itemOpt = BuiltInRegistries.ITEM.get(identifier);
                        if (itemOpt.isPresent()) {
                            Item item = itemOpt.get().value();
                            if (item != Items.AIR) {
                                stack = new ItemStack(item);
                            }
                        }
                    }
                }
                stacks.set(slotIndex++, stack);
            }
        }
    }

    private void syncToBlockEntity() {
        Level world = pipeEntity.getLevel();
        if (world == null || world.isClientSide()) {
            return;
        }

        int slotIndex = 0;
        PipeContext ctx = pipeEntity.createContext();
        for (Direction direction : ItemFilterModule.FILTER_ORDER) {
            List<String> slots = new ArrayList<>(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
            for (int i = 0; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
                ItemStack stack = stacks.get(slotIndex++);
                if (stack.isEmpty()) {
                    slots.add("");
                } else {
                    Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    slots.add(id.toString());
                }
            }
            module.setFilterSlots(ctx, direction, slots);
        }

        pipeEntity.setChanged();
        world.sendBlockUpdated(pipeEntity.getBlockPos(), pipeEntity.getBlockState(), pipeEntity.getBlockState(), 3);
    }
}
