package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.CraftingModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Inventory for the Crafting Logistics Pipe GUI.
 * Holds 9 ghost ingredient slots (indices 0-8) and 1 ghost result slot (index 9).
 */
public class CraftingRecipeInventory implements Container {
    private static final int SLOT_COUNT = 10; // 9 ingredients + 1 result
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;

    public CraftingRecipeInventory(PipeBlockEntity pipeEntity) {
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
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    private void loadFromModule() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }

        if (pipeEntity.getLevel() == null || pipeEntity.getLevel().isClientSide()) return;

        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        Pipe pipe = block.getPipe();
        CraftingModule module = pipe.getModule(CraftingModule.class, pipeEntity);
        if (module == null) return;

        PipeContext ctx = pipeEntity.createContext();

        // Load ingredient slots 0-8
        for (int slot = 0; slot < 9; slot++) {
            String itemId = module.getIngredientItem(ctx, slot);
            if (itemId.isEmpty()) continue;
            int count = module.getIngredientCount(ctx, slot);
            ItemStack stack = resolveItem(itemId, count);
            if (!stack.isEmpty()) {
                stacks.set(slot, stack);
            }
        }

        // Load result slot 9
        String resultId = module.getResultItem(ctx);
        if (!resultId.isEmpty()) {
            int count = module.getResultCount(ctx);
            ItemStack stack = resolveItem(resultId, count);
            if (!stack.isEmpty()) {
                stacks.set(9, stack);
            }
        }
    }

    public void loadFromItem(ItemStack stack) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();

        for (int slot = 0; slot < 9; slot++) {
            String itemId = CraftingModule.getIngredientItemFromTag(tag, slot);
            if (itemId.isEmpty()) continue;
            int count = CraftingModule.getIngredientCountFromTag(tag, slot);
            ItemStack itemStack = resolveItem(itemId, count);
            if (!itemStack.isEmpty()) {
                stacks.set(slot, itemStack);
            }
        }

        String resultId = CraftingModule.getResultItemFromTag(tag);
        if (!resultId.isEmpty()) {
            int count = CraftingModule.getResultCountFromTag(tag);
            ItemStack itemStack = resolveItem(resultId, count);
            if (!itemStack.isEmpty()) {
                stacks.set(9, itemStack);
            }
        }
    }

    private static ItemStack resolveItem(String itemId, int count) {
        try {
            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) return ItemStack.EMPTY;
            var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder == null) return ItemStack.EMPTY;
            var item = holder.asItem();
            if (item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(item, Math.max(1, count));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
