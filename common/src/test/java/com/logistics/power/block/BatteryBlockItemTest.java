package com.logistics.power.block;

import com.logistics.DomainRegistrations;
import com.logistics.LogisticsPower;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BatteryBlockItem}'s durability-style charge bar, which reads the stored
 * energy from the carried item's {@code minecraft:block_entity_data} component and scales it
 * against the capacity of the tier it places.
 *
 * <p>The items come from the registry rather than being constructed here: an {@code Item.Properties}
 * given an id that is never registered leaves a dangling data-component initializer, and the next
 * {@code bindDataComponents()} in the same JVM fails with "Missing element
 * ResourceKey[minecraft:item / ...]" inside whatever other class shares the worker.
 */
class BatteryBlockItemTest extends MinecraftTestEnvironment {

    @BeforeAll
    static void registerDomains() {
        DomainRegistrations.ensureRegistered();
    }

    private static Block blockFor(BatteryTier tier) {
        return switch (tier) {
            case COPPER -> LogisticsPower.BLOCK.COPPER_BATTERY;
            case BRONZE -> LogisticsPower.BLOCK.BRONZE_BATTERY;
            case GOLD -> LogisticsPower.BLOCK.GOLD_BATTERY;
            case AMETHYST -> LogisticsPower.BLOCK.AMETHYST_BATTERY;
            case ECHO -> LogisticsPower.BLOCK.ECHO_BATTERY;
        };
    }

    private static BatteryBlockItem itemFor(BatteryTier tier) {
        return (BatteryBlockItem) blockFor(tier).asItem();
    }

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
        BatteryBlockItem item = itemFor(BatteryTier.COPPER);
        assertFalse(item.isBarVisible(stackWithEnergy(0)));
        assertFalse(item.isBarVisible(new ItemStack(Items.STONE)), "no block_entity_data -> no bar");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(BatteryTier.class)
    void bar_visibleAndScalesWithCharge(BatteryTier tier) {
        BatteryBlockItem item = itemFor(tier);

        ItemStack full = stackWithEnergy(tier.capacity());
        assertTrue(item.isBarVisible(full));
        assertEquals(13, item.getBarWidth(full), "full battery fills the whole bar");
        assertEquals(0x00AA00, item.getBarColor(full));

        ItemStack half = stackWithEnergy(tier.capacity() / 2);
        int halfWidth = item.getBarWidth(half);
        assertTrue(halfWidth > 0 && halfWidth < 13, "half charge is a partial bar, got " + halfWidth);
    }

    /**
     * The bar scales against the item's own tier, so a charge that fills a Copper battery must
     * read as nearly empty on an Echo one — the regression a single shared capacity would cause.
     */
    @Test
    void bar_scalesAgainstTheItemsOwnTierNotAFixedCapacity() {
        ItemStack copperFull = stackWithEnergy(BatteryTier.COPPER.capacity());

        assertEquals(13, itemFor(BatteryTier.COPPER).getBarWidth(copperFull));
        assertEquals(1, itemFor(BatteryTier.ECHO).getBarWidth(copperFull),
                "a Copper-sized charge is a sliver of an Echo battery, not a full bar");
    }
}
