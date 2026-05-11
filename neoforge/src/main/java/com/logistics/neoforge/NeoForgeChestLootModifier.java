package com.logistics.neoforge;

import com.logistics.LogisticsCore;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.LootTableLoadEvent;

public final class NeoForgeChestLootModifier {
    private NeoForgeChestLootModifier() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeChestLootModifier::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        var key = event.getKey();

        if (BuiltInLootTables.ABANDONED_MINESHAFT.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.RAW_TIN, 10, 2, 8))
                    .add(counted(LogisticsCore.ITEM.APATITE, 15, 4, 12))
                    .build());
        }
        if (BuiltInLootTables.SIMPLE_DUNGEON.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.RAW_TIN, 8, 2, 8))
                    .add(counted(LogisticsCore.ITEM.TIN_NUGGET, 12, 5, 15))
                    .add(counted(LogisticsCore.ITEM.TIN_INGOT, 5, 1, 3))
                    .add(counted(LogisticsCore.ITEM.APATITE, 15, 3, 10))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_GEAR).setWeight(4))
                    .build());
        }
        if (BuiltInLootTables.VILLAGE_TOOLSMITH.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.TIN_NUGGET, 10, 5, 15))
                    .add(counted(LogisticsCore.ITEM.TIN_INGOT, 4, 1, 3))
                    .add(counted(LogisticsCore.ITEM.BRONZE_NUGGET, 8, 4, 12))
                    .add(counted(LogisticsCore.ITEM.BRONZE_INGOT, 3, 1, 2))
                    .add(counted(LogisticsCore.ITEM.APATITE, 10, 3, 10))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_GEAR).setWeight(4))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(3))
                    .build());
        }
        if (BuiltInLootTables.VILLAGE_ARMORER.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.BRONZE_NUGGET, 10, 4, 12))
                    .add(counted(LogisticsCore.ITEM.BRONZE_INGOT, 4, 1, 2))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(4))
                    .build());
        }
        if (BuiltInLootTables.STRONGHOLD_CORRIDOR.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.BRONZE_NUGGET, 10, 4, 12))
                    .add(counted(LogisticsCore.ITEM.BRONZE_INGOT, 4, 1, 2))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(4))
                    .add(counted(LogisticsCore.ITEM.STURDY_CASING, 3, 1, 2))
                    .build());
        }
        if (BuiltInLootTables.BURIED_TREASURE.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.TIN_NUGGET, 8, 8, 20))
                    .add(counted(LogisticsCore.ITEM.TIN_INGOT, 5, 2, 5))
                    .add(counted(LogisticsCore.ITEM.BRONZE_NUGGET, 9, 6, 18))
                    .add(counted(LogisticsCore.ITEM.BRONZE_INGOT, 6, 1, 4))
                    .add(counted(LogisticsCore.ITEM.APATITE, 10, 6, 16))
                    .add(counted(LogisticsCore.ITEM.BRONZE_GEAR, 5, 1, 3))
                    .build());
        }
        if (BuiltInLootTables.SHIPWRECK_TREASURE.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.BRONZE_NUGGET, 12, 6, 18))
                    .add(counted(LogisticsCore.ITEM.BRONZE_INGOT, 6, 1, 3))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(6))
                    .build());
        }
        if (BuiltInLootTables.SHIPWRECK_SUPPLY.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.RAW_TIN, 8, 2, 6))
                    .add(counted(LogisticsCore.ITEM.TIN_INGOT, 6, 1, 4))
                    .add(counted(LogisticsCore.ITEM.APATITE, 10, 3, 8))
                    .build());
        }
        if (BuiltInLootTables.RUINED_PORTAL.equals(key)) {
            event.getTable().addPool(pool()
                    .add(counted(LogisticsCore.ITEM.TIN_INGOT, 5, 1, 3))
                    .add(counted(LogisticsCore.ITEM.BRONZE_INGOT, 4, 1, 2))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_GEAR).setWeight(3))
                    .build());
        }
    }

    private static LootPool.Builder pool() {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1));
    }

    private static LootItem.Builder<?> counted(net.minecraft.world.level.ItemLike item, int weight, int min, int max) {
        return LootItem.lootTableItem(item)
                .setWeight(weight)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }
}
