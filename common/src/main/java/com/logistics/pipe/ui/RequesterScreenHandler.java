package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.network.packet.SyncRequesterInventoryPacket;
import com.logistics.core.lib.platform.ServerNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
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
    @Nullable private final ServerPlayer viewer;
    @Nullable private final String targetModuleStateKey;
    private BlockPos pipePos;
    private boolean initialSyncSent = false;

    private List<ItemStack> filteredItems = new ArrayList<>();
    private int currentPage = 0;
    private String currentSearch = "";

    public RequesterScreenHandler(int syncId, Container playerInventory) {
        super(LogisticsPipe.SCREEN.REQUESTER, syncId);
        this.pipeEntity = null;
        this.viewer = null;
        this.targetModuleStateKey = null;
        this.pipePos = BlockPos.ZERO;
        this.requestInventory = new RequestInventory(null);
    }

    public RequesterScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null);
    }

    public RequesterScreenHandler(
            int syncId, Container playerInventory, PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        super(LogisticsPipe.SCREEN.REQUESTER, syncId);
        this.pipeEntity = pipeEntity;
        this.viewer = viewerOf(playerInventory);
        this.targetModuleStateKey = targetModuleStateKey;
        this.pipePos = pipeEntity.getBlockPos();
        this.requestInventory = new RequestInventory(pipeEntity, targetModuleStateKey);
    }

    /**
     * The single player a menu opened over {@code playerInventory} syncs to, or {@code null} when no
     * server player owns it (client-side menus, synthetic containers). Requester contents are private
     * to their viewer and must never reach the wider player list.
     */
    @Nullable
    static ServerPlayer viewerOf(Container playerInventory) {
        return playerInventory instanceof Inventory inventory && inventory.player instanceof ServerPlayer serverPlayer
                ? serverPlayer
                : null;
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
     * Apply a sync payload to this menu (client-side). A payload addressed to a different menu is
     * dropped, leaving this menu's pipe position and item list untouched.
     *
     * @return {@code true} when the payload was applied
     */
    public boolean applySync(SyncRequesterInventoryPacket packet) {
        if (packet.containerId() != containerId) {
            return false;
        }
        setPipePos(packet.pipePos());
        setAvailableItems(packet.items(), packet.amounts());
        return true;
    }

    /**
     * Refresh search results and reset to page 1.
     */
    public void refreshSearch(String query) {
        this.currentSearch = query;
        this.filteredItems = getFilteredItems(query);
        this.filteredItems.sort(Comparator.comparing(
                        (ItemStack stack) -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .thenComparing(stack -> stack.getComponents().toString()));
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
        return PipeMenuValidity.stillValid(pipeEntity, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Send initial sync on first broadcast, to the viewer only
        if (!initialSyncSent && pipeEntity != null && viewer != null && !pipeEntity.getLevel().isClientSide()) {
            var itemsAndAmounts = requestInventory.getAllItemsWithAmounts();
            ServerNetworking.send(viewer, new SyncRequesterInventoryPacket(
                    containerId,
                    pipeEntity.getBlockPos(),
                    itemsAndAmounts.items(),
                    itemsAndAmounts.amounts()
            ));

            initialSyncSent = true;
        }
    }
}
