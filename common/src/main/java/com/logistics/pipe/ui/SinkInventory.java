package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.filter.FilterSlots;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SinkModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/**
 * Inventory wrapper for the Sink GUI.
 * Displays current filter configuration as ghost items.
 */
public class SinkInventory implements Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(SinkModule.MAX_FILTER_SLOTS, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;
    @Nullable private final String targetModuleStateKey;

    public SinkInventory(PipeBlockEntity pipeEntity) {
        this(pipeEntity, null);
    }

    public SinkInventory(PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        this.pipeEntity = pipeEntity;
        this.targetModuleStateKey = targetModuleStateKey;

        if (pipeEntity != null) {
            loadFilters(pipeEntity);
        }
    }

    private void loadFilters(PipeBlockEntity pipeEntity) {
        SinkModule module = PipeModuleHelper.getModule(pipeEntity, SinkModule.class, targetModuleStateKey);

        if (module == null) {
            return;
        }

        PipeContext ctx = pipeEntity.createContext();
        if (targetModuleStateKey != null) {
            ctx = ctx.withModuleStateKey(module, targetModuleStateKey);
        }
        FilterSlots filters = module.getFilters(ctx);
        for (int i = 0; i < filters.size(); i++) {
            String id = filters.get(i);
            if (id.isEmpty()) continue;
            ResourceId itemId = ResourceId.tryParse(id);
            if (itemId == null) continue;
            var itemHolder = BuiltInRegistries.ITEM.get(itemId.toIdentifier());
            if (itemHolder.isEmpty()) continue;
            items.set(i, new ItemStack(itemHolder.get()));
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

    public void loadFromItem(ItemStack stack) {
        for (int i = 0; i < SinkModule.MAX_FILTER_SLOTS; i++) items.set(i, ItemStack.EMPTY);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        FilterSlots filters = FilterSlots.load(NbtCompat.getCompoundOrEmpty(tag, SinkModule.FILTERS), SinkModule.MAX_FILTER_SLOTS);
        for (int i = 0; i < filters.size(); i++) {
            String itemId = filters.get(i);
            if (itemId.isEmpty()) continue;
            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) continue;
            var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder.isEmpty()) continue;
            items.set(i, new ItemStack(holder.get()));
        }
    }
}
