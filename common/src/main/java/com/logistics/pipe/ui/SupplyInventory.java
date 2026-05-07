package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SupplierModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Inventory for the Supplier GUI.
 * Shows configured supply items and their target amounts.
 */
public class SupplyInventory implements Container {
    private final NonNullList<ItemStack> stacks =
            NonNullList.withSize(SupplierModule.MAX_SUPPLY_SLOTS, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;
    @Nullable private final String targetModuleStateKey;

    public SupplyInventory(PipeBlockEntity pipeEntity) {
        this(pipeEntity, null);
    }

    public SupplyInventory(PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        this.pipeEntity = pipeEntity;
        this.targetModuleStateKey = targetModuleStateKey;

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
        // Allow syncing from server to client for GUI display
        if (slot >= 0 && slot < stacks.size()) {
            stacks.set(slot, stack);
        }
    }

    @Override
    public void setChanged() {
        // No-op for read-only supply view
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        // No-op for read-only supply view
    }

    /**
     * Load configured supplies from the SupplierModule.
     * Shows items and their target amounts.
     */
    private void loadFromModule() {
        // Clear slots
        for (int i = 0; i < SupplierModule.MAX_SUPPLY_SLOTS; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }

        if (pipeEntity.getLevel() == null || pipeEntity.getLevel().isClientSide()) {
            return;
        }

        SupplierModule module = PipeModuleHelper.getModule(pipeEntity, SupplierModule.class, targetModuleStateKey);

        if (module == null) {
            return;
        }

        // Load configured supplies
        PipeContext ctx = pipeEntity.createContext();
        if (targetModuleStateKey != null) {
            ctx = ctx.withModuleStateKey(module, targetModuleStateKey);
        }
        List<SupplierModule.SupplyConfig> configs = module.getSupplyConfigs(ctx);

        int slotIndex = 0;
        for (SupplierModule.SupplyConfig config : configs) {
            if (slotIndex >= SupplierModule.MAX_SUPPLY_SLOTS) {
                break;
            }

            // Parse item from ID
            com.logistics.core.lib.resource.ResourceId itemId =
                    com.logistics.core.lib.resource.ResourceId.tryParse(config.itemId());
            if (itemId == null) {
                continue;
            }

            var itemHolder = BuiltInRegistries.ITEM.get(itemId.toIdentifier());
            if (itemHolder.isEmpty()) {
                continue;
            }

            Item item = itemHolder.get().value();
            ItemStack displayStack = new ItemStack(item);
            // Show target amount as stack count (capped at 64 for display)
            displayStack.setCount((int) Math.min(config.amount(), 64));
            stacks.set(slotIndex++, displayStack);
        }
    }

    public void loadFromItem(ItemStack stack) {
        for (int i = 0; i < SupplierModule.MAX_SUPPLY_SLOTS; i++) stacks.set(i, ItemStack.EMPTY);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        CompoundTag supplies = NbtCompat.getCompoundOrEmpty(tag, SupplierModule.SUPPLIES);
        for (int i = 0; i < SupplierModule.MAX_SUPPLY_SLOTS; i++) {
            CompoundTag slotTag = NbtCompat.getCompoundOrEmpty(supplies, String.valueOf(i));
            String itemId = NbtCompat.getString(slotTag, "item", "");
            int amount = NbtCompat.getInt(slotTag, "amount", 0);
            if (itemId.isEmpty() || amount <= 0) continue;
            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) continue;
            var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder.isEmpty()) continue;
            Item item = holder.get().value();
            ItemStack display = new ItemStack(item);
            display.setCount((int) Math.min(amount, 64));
            stacks.set(i, display);
        }
    }

    /**
     * Refresh the inventory from the module.
     * Call this to update configured supplies.
     */
    public void refresh() {
        loadFromModule();
    }
}
