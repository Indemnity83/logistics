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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Screen handler for the Requester GUI.
 * Provides access to network items for the client screen widgets.
 */
public class RequesterScreenHandler extends AbstractContainerMenu {
    private final RequestInventory requestInventory;
    private final PipeBlockEntity pipeEntity;
    private boolean initialSyncSent = false;

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
