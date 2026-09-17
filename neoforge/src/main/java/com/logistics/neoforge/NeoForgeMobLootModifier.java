package com.logistics.neoforge;

import com.logistics.LogisticsCore;
import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.LootTableLoadEvent;

public final class NeoForgeMobLootModifier {
    private NeoForgeMobLootModifier() {}

    private static final ResourceKey<LootTable> BREEZE = ResourceKey.create(Registries.LOOT_TABLE,
        ResourceId.parse("minecraft:entities/breeze").toIdentifier());

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeMobLootModifier::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        // The Breeze (modern air-elemental analogue of TE's Blitz) drops niter,
        // at the same rate a creeper drops gunpowder: 0-2 + a Looting bonus.
        if (BREEZE.equals(event.getKey()) && event.getTable().getPool("logistics:breeze_niter") == null) {
            event.getTable().addPool(LootPool.lootPool().name("logistics:breeze_niter")
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(LogisticsCore.ITEM.NITER)
                    .apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
                    .apply(EnchantedCountIncreaseFunction.lootingMultiplier(
                        event.getRegistries().lookupOrThrow(Registries.ENCHANTMENT),
                        ContextFloatProviders.between(0.0f, 1.0f))))
                .build());
        }
    }
}
