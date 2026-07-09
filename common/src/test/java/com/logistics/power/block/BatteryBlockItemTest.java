package com.logistics.power.block;

import com.logistics.power.block.entity.BatteryBlockEntity;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BatteryBlockItem}'s durability-style charge bar, which reads the stored
 * energy from the carried item's {@code minecraft:block_entity_data} component.
 */
class BatteryBlockItemTest extends MinecraftTestEnvironment {

    private final BatteryBlockItem item = new BatteryBlockItem(
            Blocks.STONE,
            new Item.Properties().setId(
                    ResourceKey.create(Registries.ITEM, Identifier.parse("logistics:test_battery"))));

    private static ItemStack stackWithEnergy(long energy) {
        ItemStack stack = new ItemStack(Items.STONE);
        CompoundTag logisticsData = new CompoundTag();
        logisticsData.putLong("Energy", energy);
        CompoundTag tag = new CompoundTag();
        tag.put("LogisticsData", logisticsData);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityTypes.FURNACE, tag));
        return stack;
    }

    @Test
    void bar_hiddenWhenEmptyOrNoData() {
        assertFalse(item.isBarVisible(stackWithEnergy(0)));
        assertFalse(item.isBarVisible(new ItemStack(Items.STONE)), "no block_entity_data -> no bar");
    }

    @Test
    void bar_visibleAndScalesWithCharge() {
        ItemStack full = stackWithEnergy(BatteryBlockEntity.capacity());
        assertTrue(item.isBarVisible(full));
        assertEquals(13, item.getBarWidth(full), "full battery fills the whole bar");
        assertEquals(0x00AA00, item.getBarColor(full));

        ItemStack half = stackWithEnergy(BatteryBlockEntity.capacity() / 2);
        int halfWidth = item.getBarWidth(half);
        assertTrue(halfWidth > 0 && halfWidth < 13, "half charge is a partial bar, got " + halfWidth);
    }
}
