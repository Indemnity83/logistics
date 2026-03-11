package com.logistics.pipe.ui;

import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SinkModule;
import com.logistics.pipe.Pipe;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory wrapper for the Sink GUI.
 * Displays current filter configuration as ghost items.
 */
public class SinkInventory implements Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(SinkModule.MAX_FILTER_SLOTS, ItemStack.EMPTY);

    public SinkInventory(PipeBlockEntity pipeEntity) {
        if (pipeEntity != null) {
            loadFilters(pipeEntity);
        }
    }

    private void loadFilters(PipeBlockEntity pipeEntity) {
        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        Pipe pipe = block.getPipe();
        SinkModule module = pipe.getModule(SinkModule.class, pipeEntity);

        if (module == null) {
            return;
        }

        String[] filters = module.getFilters(pipeEntity.createContext());
        for (int i = 0; i < filters.length; i++) {
            if (filters[i].isEmpty()) {
                continue;
            }

            ResourceId itemId = ResourceId.tryParse(filters[i]);
            if (itemId == null) {
                continue;
            }

            // MC 1.21.1: get() returns Item directly, not Optional<Holder<Item>>
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(itemId.toIdentifier());
            if (item == null) {
                continue;
            }

            items.set(i, new ItemStack(item));
        }
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
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
