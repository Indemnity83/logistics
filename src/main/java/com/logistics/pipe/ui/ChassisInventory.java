package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.ChassisPipe;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Inventory for the Chassis Logistics Pipe GUI.
 * Holds up to {@link ChassisPipe#MAX_SLOTS} ghost module slots, backed by item ID strings
 * stored in the pipe block entity's module state.
 */
public class ChassisInventory implements Container {
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(ChassisPipe.MAX_SLOTS, ItemStack.EMPTY);

    public ChassisInventory(PipeBlockEntity pipeEntity) {
        if (pipeEntity != null) {
            loadFromEntity(pipeEntity);
        }
    }

    private void loadFromEntity(PipeBlockEntity pipeEntity) {
        if (!(pipeEntity.getBlockState().getBlock() instanceof PipeBlock pb)
                || !(pb.getPipe() instanceof ChassisPipe)) {
            return;
        }

        var ctx = pipeEntity.createContext();
        for (int slot = 0; slot < ChassisPipe.MAX_SLOTS; slot++) {
            String itemId = ChassisPipe.getSlotItem(ctx, slot);
            if (itemId.isEmpty()) continue;

            ResourceId rid = ResourceId.tryParse(itemId);
            if (rid == null) continue;

            var holder = BuiltInRegistries.ITEM.get(rid.toIdentifier());
            if (holder.isEmpty()) continue;

            items.set(slot, new ItemStack(holder.get().value()));
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
        if (slot >= 0 && slot < items.size()) {
            items.set(slot, stack);
        }
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }
}
