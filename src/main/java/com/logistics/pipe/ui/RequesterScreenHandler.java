package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Requester GUI.
 * Provides access to network items for the client screen widgets.
 */
public class RequesterScreenHandler extends AbstractContainerMenu {
    private final RequestInventory requestInventory;
    private final PipeBlockEntity pipeEntity;

    public RequesterScreenHandler(int syncId, Container playerInventory) {
        super(LogisticsPipe.SCREEN.REQUESTER, syncId);
        this.pipeEntity = null;
        this.requestInventory = new RequestInventory(null);
    }

    public RequesterScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        super(LogisticsPipe.SCREEN.REQUESTER, syncId);
        this.pipeEntity = pipeEntity;
        this.requestInventory = new RequestInventory(pipeEntity);
    }

    /**
     * Get all available items from the network.
     */
    public List<ItemStack> getAllItems() {
        return requestInventory.getAllItems();
    }

    /**
     * Get filtered items based on search text.
     * Matches if all space-separated keywords are found in the item name (case-insensitive).
     */
    public List<ItemStack> getFilteredItems(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return getAllItems();
        }

        String[] keywords = searchText.toLowerCase().trim().split("\\s+");

        return getAllItems().stream()
                .filter(stack -> matchesSearch(stack, keywords))
                .collect(Collectors.toList());
    }

    private boolean matchesSearch(ItemStack stack, String[] keywords) {
        String itemName = stack.getHoverName().getString().toLowerCase();

        for (String keyword : keywords) {
            if (!itemName.contains(keyword)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the available amount of a specific item in the network.
     */
    public long getAvailableAmount(ItemStack stack) {
        return requestInventory.getAvailableAmount(stack);
    }

    /**
     * Get the pipe's block position for network packet.
     */
    public BlockPos getPipePos() {
        return pipeEntity != null ? pipeEntity.getBlockPos() : BlockPos.ZERO;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
