package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.network.SyncRequesterInventoryPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Screen handler for the Requester GUI.
 * Provides access to network items for the client screen widgets.
 */
public class RequesterScreenHandler extends AbstractContainerMenu {
    public static final int ITEMS_PER_PAGE = 32; // 8×4 grid

    private final RequestInventory requestInventory;
    private final PipeBlockEntity pipeEntity;
    private BlockPos pipePos;
    private boolean initialSyncSent = false;

    private List<ItemStack> filteredItems = new ArrayList<>();
    private int currentPage = 0;
    private String currentSearch = "";

    public RequesterScreenHandler(int syncId, Container playerInventory) {
        super(LogisticsPipe.SCREEN.REQUESTER, syncId);
        this.pipeEntity = null;
        this.pipePos = BlockPos.ZERO;
        this.requestInventory = new RequestInventory(null);
    }

    public RequesterScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        super(LogisticsPipe.SCREEN.REQUESTER, syncId);
        this.pipeEntity = pipeEntity;
        this.pipePos = pipeEntity.getBlockPos();
        this.requestInventory = new RequestInventory(pipeEntity);
    }

    /**
     * Get all available items from the network.
     */
    public List<ItemStack> getAllItems() {
        return requestInventory.getAllItems();
    }

    /**
     * Set available items (client-side only, called from packet receiver).
     */
    public void setAvailableItems(List<ItemStack> items, List<Long> amounts) {
        requestInventory.setAllItems(items, amounts);
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
        return pipePos;
    }

    /**
     * Set the pipe's block position (client-side, from sync packet).
     */
    public void setPipePos(BlockPos pos) {
        this.pipePos = pos;
    }

    /**
     * Refresh search results and reset to page 1.
     */
    public void refreshSearch(String query) {
        this.currentSearch = query;
        this.filteredItems = getFilteredItems(query);
        this.currentPage = 0;
    }

    /**
     * Get items for the current page (max 32 items).
     */
    public List<ItemStack> getCurrentPageItems() {
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());
        if (startIndex >= filteredItems.size()) {
            return new ArrayList<>();
        }
        return filteredItems.subList(startIndex, endIndex);
    }

    /**
     * Get total number of pages based on filtered items.
     */
    public int getTotalPages() {
        if (filteredItems.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(filteredItems.size() / (double) ITEMS_PER_PAGE);
    }

    /**
     * Navigate to next page.
     */
    public void nextPage() {
        if (currentPage < getTotalPages() - 1) {
            currentPage++;
        }
    }

    /**
     * Navigate to previous page.
     */
    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    /**
     * Get current page number (0-based).
     */
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Send initial sync on first broadcast
        if (!initialSyncSent && pipeEntity != null && !pipeEntity.getLevel().isClientSide()) {
            var itemsAndAmounts = requestInventory.getAllItemsWithAmounts();
            SyncRequesterInventoryPacket packet = new SyncRequesterInventoryPacket(
                    pipeEntity.getBlockPos(),
                    itemsAndAmounts.items(),
                    itemsAndAmounts.amounts()
            );

            // Send to all players on the server (they'll filter based on open screen)
            for (ServerPlayer player : pipeEntity.getLevel().getServer().getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, packet);
            }

            initialSyncSent = true;
        }
    }
}
