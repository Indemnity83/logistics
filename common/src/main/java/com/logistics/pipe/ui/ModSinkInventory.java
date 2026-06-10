package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ModSinkModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/**
 * Ghost inventory backing the mod filter display slots in the Mod-Based Item Sink GUI.
 *
 * <p>Holds {@link ModSinkModule#MAX_FILTERS} slots (indices 0 to MAX_FILTERS-1), each storing the
 * representative item used when a mod filter was added. Items cannot be picked up or placed
 * normally — they are managed programmatically via the screen handler.
 */
public class ModSinkInventory implements Container {
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(ModSinkModule.MAX_FILTERS, ItemStack.EMPTY);
    @Nullable private final String targetModuleStateKey;

    public ModSinkInventory(@SuppressWarnings("unused") PipeBlockEntity pipeEntity) {
        this(pipeEntity, null);
    }

    public ModSinkInventory(@SuppressWarnings("unused") PipeBlockEntity pipeEntity,
            @Nullable String targetModuleStateKey) {
        this.targetModuleStateKey = targetModuleStateKey;
        if (pipeEntity != null) {
            loadFilters(pipeEntity);
        }
    }

    private void loadFilters(PipeBlockEntity pipeEntity) {
        ModSinkModule module =
                PipeModuleHelper.getModule(pipeEntity, ModSinkModule.class, targetModuleStateKey);
        if (module == null) return;

        PipeContext ctx = pipeEntity.createContext();
        if (targetModuleStateKey != null) {
            ctx = ctx.withModuleStateKey(module, targetModuleStateKey);
        }
        String[] filterIds = module.getFilterItemIds(ctx);
        for (int i = 0; i < filterIds.length; i++) {
            if (filterIds[i].isEmpty()) continue;
            ResourceId rid = ResourceId.tryParse(filterIds[i]);
            if (rid == null) continue;
            Item holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder == null || holder == Items.AIR) continue;
            items.set(i, new ItemStack(holder));
        }
    }

    public void loadFromItem(ItemStack stack) {
        for (int i = 0; i < ModSinkModule.MAX_FILTERS; i++) items.set(i, ItemStack.EMPTY);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        for (int i = 0; i < ModSinkModule.MAX_FILTERS; i++) {
            String itemId = NbtCompat.getString(tag, ModSinkModule.MOD_IDS + "_" + i, "");
            if (itemId.isEmpty()) continue;
            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) continue;
            Item holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder == null || holder == Items.AIR) continue;
            items.set(i, new ItemStack(holder));
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
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
