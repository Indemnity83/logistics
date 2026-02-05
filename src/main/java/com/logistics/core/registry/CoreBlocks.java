package com.logistics.core.registry;

import com.logistics.LogisticsMod;
import com.logistics.core.marker.MarkerBlock;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

public final class CoreBlocks {
    private CoreBlocks() {}

    private static final String DOMAIN = "core/";

    public static final Block MARKER = register("marker", MarkerBlock::new);

    private static Block register(String name, Function<Block.Properties, Block> blockFactory) {
        // Create a registry key for the block
        ResourceKey<Block> blockKey = keyOfBlock(name);

        // Create the block instance with the registry key
        Block block = blockFactory.apply(Block.Properties.of().setId(blockKey));

        // Items need to be registered with a different type of registry key, but the ID can be the same.
        ResourceKey<Item> itemKey = keyOfItem(name);
        BlockItem blockItem =
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name));
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
        BuiltInRegistries.BLOCK.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, name),
                BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void addItemAlias(String name, Item item) {
        BuiltInRegistries.ITEM.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, name),
                BuiltInRegistries.ITEM.getKey(item));
    }
}
