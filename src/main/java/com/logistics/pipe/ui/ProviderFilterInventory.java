package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ProviderModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Inventory for the Provider filter GUI.
 * Shows configured filter items (ghost items for whitelist/blacklist).
 */
public class ProviderFilterInventory implements Container {
    private static final int FILTER_SLOT_COUNT = 9;
    private final NonNullList<ItemStack> stacks =
            NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;

    public ProviderFilterInventory(PipeBlockEntity pipeEntity) {
        this.pipeEntity = pipeEntity;

        if (pipeEntity != null) {
            loadFromModule();
        }
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        return stacks.stream().allMatch(ItemStack::isEmpty);
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
        if (slot >= 0 && slot < stacks.size()) {
            stacks.set(slot, stack);
        }
    }

    @Override
    public void setChanged() {
        // No-op for read-only filter view
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        // No-op for read-only filter view
    }

    /**
     * Load configured filter items from the ProviderModule.
     */
    private void loadFromModule() {
        // Clear slots
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }

        if (pipeEntity.getLevel() == null || pipeEntity.getLevel().isClientSide()) {
            return;
        }

        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        Pipe pipe = block.getPipe();
        ProviderModule module = pipe.getModule(ProviderModule.class, pipeEntity);

        if (module == null) {
            return;
        }

        PipeContext ctx = pipeEntity.createContext();
        List<String> filterItems = module.getFilterItems(ctx);

        int slotIndex = 0;
        for (String itemId : filterItems) {
            if (slotIndex >= FILTER_SLOT_COUNT) {
                break;
            }

            com.logistics.core.lib.resource.ResourceId resourceId =
                    com.logistics.core.lib.resource.ResourceId.tryParse(itemId);
            if (resourceId == null) {
                continue;
            }

            var itemHolder = BuiltInRegistries.ITEM.get(resourceId.toIdentifier());
            if (itemHolder.isEmpty()) {
                continue;
            }

            Item item = itemHolder.get().value();
            ItemStack displayStack = new ItemStack(item);
            displayStack.setCount(1);
            stacks.set(slotIndex++, displayStack);
        }
    }

    public void loadFromItem(ItemStack stack) {
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) stacks.set(i, ItemStack.EMPTY);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        CompoundTag filterItems = NbtCompat.getCompoundOrEmpty(tag, ProviderModule.FILTER_ITEMS);
        int displaySlot = 0;
        for (int i = 0; i < FILTER_SLOT_COUNT && displaySlot < FILTER_SLOT_COUNT; i++) {
            String itemId = NbtCompat.getString(filterItems, String.valueOf(i), "");
            if (itemId.isEmpty()) continue;
            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) continue;
            var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder.isEmpty()) continue;
            Item item = holder.get().value();
            stacks.set(displaySlot++, new ItemStack(item));
        }
    }

    /**
     * Refresh the inventory from the module.
     */
    public void refresh() {
        if (pipeEntity == null) return;
        loadFromModule();
    }
}
