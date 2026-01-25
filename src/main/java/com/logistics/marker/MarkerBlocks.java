package com.logistics.marker;

import java.util.function.Function;

import com.logistics.LogisticsMod;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class MarkerBlocks {
    private MarkerBlocks() {}

    public static final Block MARKER = register("marker", MarkerBlock::new);

    private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, name));
        Block block = blockFactory.apply(AbstractBlock.Settings.create().registryKey(blockKey));

        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, name));
        BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
        Registry.register(Registries.ITEM, itemKey, blockItem);

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering marker blocks");
    }
}
