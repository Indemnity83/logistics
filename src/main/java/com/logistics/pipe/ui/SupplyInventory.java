package com.logistics.pipe.ui;

import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SupplierModule;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Inventory for the Supplier GUI.
 * Shows configured supply items and their target amounts.
 */
public class SupplyInventory implements Container {
    private final NonNullList<ItemStack> stacks =
            NonNullList.withSize(SupplierModule.MAX_SUPPLY_SLOTS, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;

    public SupplyInventory(PipeBlockEntity pipeEntity) {
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

        // Get the supplier module
        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        Pipe pipe = block.getPipe();
        SupplierModule module = pipe.getModule(SupplierModule.class);

        if (module == null) {
            return;
        }

        // Load configured supplies
        PipeContext ctx = pipeEntity.createContext();
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

            // MC 1.21.1: get() returns Item directly, not Optional<Holder<Item>>
            Item item = BuiltInRegistries.ITEM.get(itemId.toIdentifier());
            if (item == null) {
                continue;
            }
            ItemStack displayStack = new ItemStack(item);
            // Show target amount as stack count (capped at 64 for display)
            displayStack.setCount((int) Math.min(config.amount(), 64));
            stacks.set(slotIndex++, displayStack);
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
