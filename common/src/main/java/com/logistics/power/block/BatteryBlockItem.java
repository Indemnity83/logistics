package com.logistics.power.block;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.power.block.entity.BatteryBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block item for the battery that shows a green charge bar (like a durability bar)
 * based on the energy stored in the carried item's {@code minecraft:block_entity_data} component.
 */
public class BatteryBlockItem extends BlockItem {
    public BatteryBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredEnergy(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long stored = getStoredEnergy(stack);
        return Math.round(13.0f * stored / BatteryBlockEntity.CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00AA00;
    }

    private static long getStoredEnergy(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (beData == null) return 0L;
        CompoundTag tag = beData.getUnsafe();
        CompoundTag logisticsData = NbtCompat.getCompoundOrEmpty(tag, "LogisticsData");
        return NbtCompat.getLong(logisticsData, "Energy", 0L);
    }
}
