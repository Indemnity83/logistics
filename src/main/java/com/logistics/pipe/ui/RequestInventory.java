package com.logistics.pipe.ui;

import com.logistics.pipe.network.NetworkRegistry;
import com.logistics.pipe.network.PipeNetwork;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.RequesterModule;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Inventory for the Requester GUI.
 * Shows available items from the network (first 9 types for proof-of-concept).
 * Clicking an item requests it from the network.
 */
public class RequestInventory implements Container {
    private final NonNullList<ItemStack> stacks =
            NonNullList.withSize(RequesterModule.MAX_REQUEST_SLOTS, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;

    public RequestInventory(PipeBlockEntity pipeEntity) {
        this.pipeEntity = pipeEntity;

        if (pipeEntity != null) {
            loadFromNetwork();
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
        // No-op for read-only network view
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        // No-op for read-only network view
    }

    /**
     * Get all available items from the network (no limit).
     * Returns a list of ItemStacks with counts set to display amount (capped at 64).
     */
    public List<ItemStack> getAllItems() {
        Map<ItemStack, Long> available = getAvailableItemsFromNetwork();
        List<ItemStack> result = new ArrayList<>();

        for (var entry : available.entrySet()) {
            ItemStack display = entry.getKey().copy();
            display.setCount((int) Math.min(entry.getValue(), 64));
            result.add(display);
        }

        return result;
    }

    /**
     * Get the available amount of a specific item in the network.
     */
    public long getAvailableAmount(ItemStack stack) {
        if (pipeEntity == null || pipeEntity.getLevel() == null) {
            return 0;
        }

        PipeNetwork network = NetworkRegistry.getOrCreateNetwork(pipeEntity.getLevel(), pipeEntity.getBlockPos());
        if (network == null) {
            return 0;
        }

        Map<ItemStack, Long> available = network.getAllAvailableItems();
        for (var entry : available.entrySet()) {
            if (ItemStack.isSameItemSameComponents(entry.getKey(), stack)) {
                return entry.getValue();
            }
        }

        return 0;
    }

    /**
     * Get available items from the network.
     */
    private Map<ItemStack, Long> getAvailableItemsFromNetwork() {
        if (pipeEntity == null || pipeEntity.getLevel() == null) {
            return Map.of();
        }

        PipeNetwork network = NetworkRegistry.getOrCreateNetwork(pipeEntity.getLevel(), pipeEntity.getBlockPos());
        if (network == null) {
            return Map.of();
        }

        return network.getAllAvailableItems();
    }

    /**
     * Load available items from the network.
     * Shows first 9 item types available from providers (for backwards compatibility).
     */
    private void loadFromNetwork() {
        // Clear slots
        for (int i = 0; i < RequesterModule.MAX_REQUEST_SLOTS; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }

        if (pipeEntity == null || pipeEntity.getLevel() == null || pipeEntity.getLevel().isClientSide()) {
            return;
        }

        // Get all available items from network
        Map<ItemStack, Long> availableItems = getAvailableItemsFromNetwork();

        // Show first 9 item types
        List<Map.Entry<ItemStack, Long>> entries = new ArrayList<>(availableItems.entrySet());
        int slotIndex = 0;
        for (var entry : entries) {
            if (slotIndex >= RequesterModule.MAX_REQUEST_SLOTS) {
                break;
            }

            ItemStack displayStack = entry.getKey().copy();
            // Show available amount as stack count (capped at 64 for display)
            long available = entry.getValue();
            displayStack.setCount((int) Math.min(available, 64));
            stacks.set(slotIndex++, displayStack);
        }
    }

    /**
     * Refresh the inventory from the network.
     * Call this to update available items.
     */
    public void refresh() {
        loadFromNetwork();
    }
}
