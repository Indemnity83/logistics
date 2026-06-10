package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ItemFilterModule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class FilterInventory implements Container {
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(
            ItemFilterModule.FILTER_ORDER.length * ItemFilterModule.FILTER_SLOTS_PER_SIDE, ItemStack.EMPTY);
    private final PipeBlockEntity pipeEntity;
    @Nullable private final String targetModuleStateKey;
    private final ItemFilterModule module;

    public FilterInventory(PipeBlockEntity pipeEntity) {
        this(pipeEntity, null);
    }

    public FilterInventory(PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        this.pipeEntity = pipeEntity;
        this.targetModuleStateKey = targetModuleStateKey;
        this.module = getModuleFromPipe(pipeEntity);

        if (pipeEntity != null) {
            loadFromBlockEntity();
        }
    }

    private ItemFilterModule getModuleFromPipe(PipeBlockEntity entity) {
        if (entity == null) {
            return new ItemFilterModule();
        }

        ItemFilterModule pipeModule =
                PipeModuleHelper.getModule(entity, ItemFilterModule.class, targetModuleStateKey);

        return pipeModule != null ? pipeModule : new ItemFilterModule();
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
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
        if (slot < 0 || slot >= stacks.size()) {
            return;
        }

        ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        stacks.set(slot, copy);

        if (pipeEntity != null) {
            syncToBlockEntity();
        }
    }

    @Override
    public void setChanged() {
        if (pipeEntity != null) {
            syncToBlockEntity();
        }
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        if (pipeEntity != null) {
            syncToBlockEntity();
        }
    }

    private void loadFromBlockEntity() {
        int slotIndex = 0;
        PipeContext ctx = pipeEntity.createContext();
        if (targetModuleStateKey != null) {
            ctx = ctx.withModuleStateKey(module, targetModuleStateKey);
        }
        for (Direction direction : ItemFilterModule.FILTER_ORDER) {
            List<ItemStack> filterStacks = module.getFilterStacks(ctx, direction);
            for (int i = 0; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
                stacks.set(slotIndex++, filterStacks.get(i));
            }
        }
    }

    public void loadFromItem(ItemStack itemStack, HolderLookup.Provider registries) {
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        CompoundTag filters = NbtCompat.getCompoundOrEmpty(tag, ItemFilterModule.FILTERS);
        RegistryOps<Tag> ops = registries != null
                ? registries.createSerializationContext(NbtOps.INSTANCE)
                : null;
        int slotIndex = 0;
        for (Direction direction : ItemFilterModule.FILTER_ORDER) {
            ListTag list = NbtCompat.getListOrEmpty(filters, direction.getName());
            for (int i = 0; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
                ItemStack resolved = ItemStack.EMPTY;
                if (i < list.size()) {
                    var compoundOpt = list.getCompound(i);
                    if (compoundOpt.isPresent()) {
                        // New format: full ItemStack with components
                        CompoundTag slotTag = compoundOpt.get();
                        if (!slotTag.isEmpty() && ops != null) {
                            resolved = ItemStack.CODEC.parse(ops, slotTag).result()
                                    .orElse(ItemStack.EMPTY);
                        }
                    } else {
                        // Old format: item registry ID string
                        String itemId = NbtCompat.getStringAt(list, i, "");
                        if (!itemId.isEmpty()) {
                            ResourceId rid = ResourceId.tryParse(itemId);
                            if (rid != null) {
                                var itemOpt = BuiltInRegistries.ITEM.get(rid.toIdentifier());
                                if (itemOpt.isPresent() && itemOpt.get().value() != Items.AIR) {
                                    resolved = new ItemStack(itemOpt.get().value());
                                }
                            }
                        }
                    }
                }
                stacks.set(slotIndex++, resolved);
            }
        }
    }

    private void syncToBlockEntity() {
        Level world = pipeEntity.getLevel();
        if (world == null || world.isClientSide()) {
            return;
        }

        int slotIndex = 0;
        PipeContext ctx = pipeEntity.createContext();
        if (targetModuleStateKey != null) {
            ctx = ctx.withModuleStateKey(module, targetModuleStateKey);
        }
        for (Direction direction : ItemFilterModule.FILTER_ORDER) {
            List<ItemStack> slots = new ArrayList<>(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
            for (int i = 0; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
                slots.add(stacks.get(slotIndex++));
            }
            module.setFilterStacks(ctx, direction, slots);
        }

        pipeEntity.setChanged();
        world.sendBlockUpdated(pipeEntity.getBlockPos(), pipeEntity.getBlockState(), pipeEntity.getBlockState(), 3);
    }
}
