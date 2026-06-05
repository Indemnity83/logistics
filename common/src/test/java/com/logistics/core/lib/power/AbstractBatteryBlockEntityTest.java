package com.logistics.core.lib.power;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the loader-agnostic, {@code Level}-independent behaviour of
 * {@link AbstractBatteryBlockEntity}: charge-level bucketing, energy accessors, NBT persistence,
 * and the dropped-item energy component. (Tick / network-registration paths require a live world
 * and are covered by game tests.)
 */
class AbstractBatteryBlockEntityTest extends MinecraftTestEnvironment {

    /** Concrete battery backed by a vanilla block-entity type so no mod registration is needed. */
    private static final class TestBattery extends AbstractBatteryBlockEntity {
        TestBattery(long capacity) {
            // A vanilla type/state pair (the constructor validates the type matches the block).
            super(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState(),
                    capacity, capacity, capacity);
        }

        void saveTo(CompoundTag tag) {
            saveLogisticsData(tag, null);
        }

        void loadFrom(CompoundTag tag) {
            loadLogisticsData(tag, null);
        }

        void collectInto(DataComponentMap.Builder builder) {
            collectImplicitComponents(builder);
        }
    }

    private static TestBattery battery(long capacity, long amount) {
        TestBattery b = new TestBattery(capacity);
        ((EnergyComponent) b.energyStorage(null)).setAmount(amount);
        return b;
    }

    @Test
    void chargeLevel_bucketsEnergyFromZeroToTen() {
        long cap = 100_000;
        assertEquals(0, battery(cap, 0).getChargeLevel(), "empty -> 0");
        assertEquals(1, battery(cap, 1).getChargeLevel(), "any non-zero charge shows at least level 1");
        assertEquals(5, battery(cap, 50_000).getChargeLevel());
        assertEquals(10, battery(cap, 100_000).getChargeLevel(), "full -> 10");
    }

    @Test
    void infoAccessors_reflectStoredEnergy() {
        TestBattery b = battery(100_000, 25_000);
        assertSame(b.energyStorage(null), b.energyStorage(Direction.UP), "storage is non-sided");
        assertEquals(25_000, b.getEnergyStored());
        assertEquals(100_000, b.getEnergyCapacity());
        assertTrue(b.acceptsLowTierEnergyFrom(Direction.NORTH));
    }

    @Test
    void nbtRoundTrip_preservesStoredEnergy() {
        CompoundTag tag = new CompoundTag();
        battery(100_000, 42_000).saveTo(tag);

        TestBattery restored = battery(100_000, 0);
        restored.loadFrom(tag);
        assertEquals(42_000, restored.getEnergyStored());
    }

    @Test
    void collectImplicitComponents_exposesEnergyForItemDrop() {
        DataComponentMap.Builder builder = DataComponentMap.builder();
        battery(100_000, 30_000).collectInto(builder);

        TypedEntityData<?> data = builder.build().get(DataComponents.BLOCK_ENTITY_DATA);
        assertNotNull(data, "battery should expose block_entity_data so a broken battery keeps its charge");
        CompoundTag logisticsData = NbtCompat.getCompoundOrEmpty(data.getUnsafe(), "LogisticsData");
        assertEquals(30_000, NbtCompat.getLong(logisticsData, "Energy", 0));
    }
}
