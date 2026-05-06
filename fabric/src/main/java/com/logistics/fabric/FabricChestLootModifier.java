package com.logistics.fabric;

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

public final class FabricChestLootModifier {
    private FabricChestLootModifier() {}

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
            // Abandoned mineshaft - raw tin and apatite (ore discovery)
            if (ABANDONED_MINESHAFT.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.RAW_TIN)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.APATITE)
                        .setWeight(15)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 12))))
                    .build());
            }

            // Simple dungeon - raw tin, tin materials, and apatite
            if (SIMPLE_DUNGEON.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.RAW_TIN)
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_NUGGET)
                        .setWeight(12)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(5, 15))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_INGOT)
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.APATITE)
                        .setWeight(15)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 10))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_GEAR).setWeight(4))
                    .build());
            }

            // Village toolsmith - tin and bronze materials, apatite, and gears
            if (VILLAGE_TOOLSMITH.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_NUGGET)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(5, 15))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_INGOT)
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_NUGGET)
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 12))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.APATITE)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 10))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_GEAR).setWeight(4))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(3))
                    .build());
            }

            // Village armorer - bronze materials
            if (VILLAGE_ARMORER.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_NUGGET)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 12))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(4))
                    .build());
            }

            // Stronghold corridor - bronze materials and sturdy casing
            if (STRONGHOLD_CORRIDOR.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_NUGGET)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 12))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(4)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(4))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.STURDY_CASING)
                        .setWeight(3)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                    .build());
            }

            // Buried treasure - mix of nuggets/ingots, gears, and apatite
            if (BURIED_TREASURE.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_NUGGET)
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(8, 20))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_INGOT)
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_NUGGET)
                        .setWeight(9)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 18))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(6)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.APATITE)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 16))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR)
                        .setWeight(5)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                    .build());
            }

            // Shipwreck treasure - bronze nuggets/ingots (nautical theme, favor nuggets)
            if (SHIPWRECK_TREASURE.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_NUGGET)
                        .setWeight(12)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(6, 18))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_INGOT)
                        .setWeight(6)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.BRONZE_GEAR).setWeight(6))
                    .build());
            }

            // Shipwreck supply - raw materials, tin, and apatite
            if (SHIPWRECK_SUPPLY.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.RAW_TIN)
                        .setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.TIN_INGOT)
                        .setWeight(6)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.APATITE)
                        .setWeight(10)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 8))))
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
