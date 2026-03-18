package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.FilterSlots;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.Pipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.AdvancedExtractorModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/**
 * Inventory for the Advanced Extractor filter GUI.
 * Shows configured filter items as ghost items (whitelist/blacklist display).
 */
public class AdvancedExtractorFilterInventory implements Container {
    private static final int FILTER_SLOT_COUNT = AdvancedExtractorModule.MAX_FILTER_SLOTS;

    private final NonNullList<ItemStack> stacks =
            NonNullList.withSize(FILTER_SLOT_COUNT, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;

    public AdvancedExtractorFilterInventory(PipeBlockEntity pipeEntity) {
        this.pipeEntity = pipeEntity;
        if (pipeEntity != null) {
            loadFromModule();
        }
    }

    @Override
    public int getContainerSize() { return stacks.size(); }

    @Override
    public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getItem(int slot) { return stacks.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
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
    public void setChanged() {}

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }

    @Override
    public void clearContent() {}

    private void loadFromModule() {
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }

        if (pipeEntity.getLevel() == null || pipeEntity.getLevel().isClientSide()) return;

        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        Pipe pipe = block.getPipe();
        AdvancedExtractorModule module = pipe.getModule(AdvancedExtractorModule.class, pipeEntity);
        if (module == null) return;

        PipeContext ctx = pipeEntity.createContext();
        FilterSlots filterItems = module.getFilterItems(ctx);

        for (int i = 0; i < filterItems.size(); i++) {
            String itemId = filterItems.get(i);
            if (itemId.isEmpty()) continue;
            ResourceId resourceId = ResourceId.tryParse(itemId);
            if (resourceId == null) continue;
            Item itemResolved = BuiltInRegistries.ITEM.get(resourceId.toIdentifier());
            if (itemResolved == null || itemResolved == Items.AIR) continue;
            stacks.set(i, new ItemStack(itemResolved));
        }
    }

    public void loadFromItem(ItemStack stack) {
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) stacks.set(i, ItemStack.EMPTY);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        FilterSlots filterItems = FilterSlots.load(NbtCompat.getCompoundOrEmpty(tag, AdvancedExtractorModule.FILTER_ITEMS), FILTER_SLOT_COUNT);
        for (int i = 0; i < filterItems.size(); i++) {
            String itemId = filterItems.get(i);
            if (itemId.isEmpty()) continue;
            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) continue;
            Item holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder == null || holder == Items.AIR) continue;
            stacks.set(i, new ItemStack(holder));
        }
    }

    public void refresh() {
        if (pipeEntity == null) return;
        loadFromModule();
    }
}
