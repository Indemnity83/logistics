package com.logistics.core.loot;

import com.logistics.LogisticsCore;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class ChestLootModifier {
    // Vanilla chest loot table keys
    private static final ResourceKey<LootTable> ABANDONED_MINESHAFT = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/abandoned_mineshaft"));
    private static final ResourceKey<LootTable> SIMPLE_DUNGEON = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/simple_dungeon"));
    private static final ResourceKey<LootTable> VILLAGE_TOOLSMITH = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/village/village_toolsmith"));
    private static final ResourceKey<LootTable> VILLAGE_ARMORER = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/village/village_armorer"));
    private static final ResourceKey<LootTable> STRONGHOLD_CORRIDOR = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/stronghold_corridor"));
    private static final ResourceKey<LootTable> BURIED_TREASURE = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/buried_treasure"));
    private static final ResourceKey<LootTable> SHIPWRECK_TREASURE = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/shipwreck_treasure"));
    private static final ResourceKey<LootTable> SHIPWRECK_SUPPLY = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/shipwreck_supply"));
    private static final ResourceKey<LootTable> RUINED_PORTAL = ResourceKey.create(Registries.LOOT_TABLE,
        net.minecraft.resources.Identifier.parse("minecraft:chests/ruined_portal"));

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            // Raw tin in mineshafts and dungeons (ore discovery)
            if (ABANDONED_MINESHAFT.equals(key) || SIMPLE_DUNGEON.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.RAW_TIN)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8))))
                    .build());
            }

            // Tin ingots and nuggets in villages and dungeons
            if (VILLAGE_TOOLSMITH.equals(key) || SIMPLE_DUNGEON.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_INGOT)
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_NUGGET)
                        .setWeight(6)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 9))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_GEAR).setWeight(5))
                    .build());
            }

            // Bronze ingots and gear in toolsmith and strongholds (rarer, better loot)
            if (VILLAGE_TOOLSMITH.equals(key) || VILLAGE_ARMORER.equals(key) || STRONGHOLD_CORRIDOR.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(6)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_NUGGET)
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 7))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(4))
                    .build());
            }

            // Sturdy casing in strongholds (advanced component, rare)
            if (STRONGHOLD_CORRIDOR.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.STURDY_CASING)
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .build());
            }

            // Buried treasure - good amounts of ingots and gears
            if (BURIED_TREASURE.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_INGOT)
                        .setWeight(7)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 8))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR)
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                    .build());
            }

            // Shipwreck treasure - bronze items (nautical theme)
            if (SHIPWRECK_TREASURE.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 5))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_NUGGET)
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 12))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(6))
                    .build());
            }

            // Shipwreck supply - raw materials and tin
            if (SHIPWRECK_SUPPLY.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.RAW_TIN)
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_INGOT)
                        .setWeight(6)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                    .build());
            }

            // Ruined portals - mixed materials
            if (RUINED_PORTAL.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_INGOT)
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_GEAR).setWeight(3))
                    .build());
            }
        });
    }
}
