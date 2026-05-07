package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ItemFilterModule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
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
        ItemFilterModule pipeModule = pipe.getModule(ItemFilterModule.class, entity);

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
                    ResourceId resource = ResourceId.tryParse(id);
                    if (resource != null) {
                        var itemOpt = BuiltInRegistries.ITEM.get(resource.toIdentifier());
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

    public void loadFromItem(ItemStack stack) {
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        CompoundTag filters = NbtCompat.getCompoundOrEmpty(tag, ItemFilterModule.FILTERS);
        int slotIndex = 0;
        for (Direction direction : ItemFilterModule.FILTER_ORDER) {
            ListTag list = NbtCompat.getListOrEmpty(filters, direction.getName());
            for (int i = 0; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
                String itemId = NbtCompat.getStringAt(list, i, "");
                ItemStack resolved = ItemStack.EMPTY;
                if (!itemId.isEmpty()) {
                    ResourceId rid = ResourceId.tryParse(itemId);
                    if (rid != null) {
                        var itemOpt = BuiltInRegistries.ITEM.get(rid.toIdentifier());
                        if (itemOpt.isPresent() && itemOpt.get().value() != Items.AIR) {
                            resolved = new ItemStack(itemOpt.get().value());
                        }
                    }
                }
                stacks.set(slotIndex++, resolved);
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
                    slots.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                }
            }
            module.setFilterSlots(ctx, direction, slots);
        }

        pipeEntity.setChanged();
        world.sendBlockUpdated(pipeEntity.getBlockPos(), pipeEntity.getBlockState(), pipeEntity.getBlockState(), 3);
    }
}
