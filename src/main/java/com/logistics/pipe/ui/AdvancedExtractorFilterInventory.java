package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.AdvancedExtractorModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.List;

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
        List<String> filterItems = module.getFilterItems(ctx);

        for (int i = 0; i < filterItems.size() && i < FILTER_SLOT_COUNT; i++) {
            String itemId = filterItems.get(i);
            if (itemId.isEmpty()) continue;
            ResourceId resourceId = ResourceId.tryParse(itemId);
            if (resourceId == null) continue;
            var itemHolder = BuiltInRegistries.ITEM.get(resourceId.toIdentifier());
            if (itemHolder.isEmpty()) continue;
            stacks.set(i, new ItemStack(itemHolder.get().value()));
        }
    }

    public void refresh() {
        loadFromModule();
    }
}
