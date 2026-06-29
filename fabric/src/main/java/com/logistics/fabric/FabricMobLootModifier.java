package com.logistics.fabric;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class FabricMobLootModifier {
    private FabricMobLootModifier() {}

    private static final ResourceKey<LootTable> BREEZE = ResourceKey.create(Registries.LOOT_TABLE,
        ResourceId.parse("minecraft:entities/breeze").toIdentifier());

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            // The Breeze (modern air-elemental analogue of TE's Blitz) drops niter,
            // at the same rate a creeper drops gunpowder: 0-2 + a Looting bonus.
            if (BREEZE.equals(key)) {
                tableBuilder.pool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(LogisticsCore.ITEM.NITER)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0, 2)))
                        .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0, 1))))
                    .build());
            }
        });
    }
}
