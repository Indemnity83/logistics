package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import com.logistics.core.marker.MarkerBlock;
import java.util.function.Function;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class CoreBlocks {
    private CoreBlocks() {}

    private static final String DOMAIN = "core/";

    public static final Block MARKER = register("marker", MarkerBlock::new);

    private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory) {
        // Create a registry key for the block
        RegistryKey<Block> blockKey = keyOfBlock(name);

        // Create the block instance (1.21.2+ requires the key to be present in the settings at construction time)
        Block block = blockFactory.apply(AbstractBlock.Settings.create().registryKey(blockKey));

        // Items need to be registered with a different type of registry key, but the ID can be the same.
        RegistryKey<Item> itemKey = keyOfItem(name);
        BlockItem blockItem =
                new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
        Registry.register(Registries.ITEM, itemKey, blockItem);

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static RegistryKey<Block> keyOfBlock(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    private static RegistryKey<Item> keyOfItem(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering core blocks");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        addBlockAlias("marker", MARKER);
        addItemAlias("marker", MARKER.asItem());
    }

    private static void addBlockAlias(String name, Block block) {
        Registries.BLOCK.addAlias(Identifier.of(LogisticsMod.MOD_ID, name), Registries.BLOCK.getId(block));
    }

    private static void addItemAlias(String name, Item item) {
        Registries.ITEM.addAlias(Identifier.of(LogisticsMod.MOD_ID, name), Registries.ITEM.getId(item));
    }
}
