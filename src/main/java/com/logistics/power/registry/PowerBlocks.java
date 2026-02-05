package com.logistics.power.registry;

import com.logistics.LogisticsMod;
import com.logistics.power.block.CreativeSinkBlock;
import com.logistics.power.engine.block.CreativeEngineBlock;
import com.logistics.power.engine.block.RedstoneEngineBlock;
import com.logistics.power.engine.block.StirlingEngineBlock;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class PowerBlocks {
    private PowerBlocks() {}

    private static final String DOMAIN = "power/";

    public static final Block REDSTONE_ENGINE = register("redstone_engine", RedstoneEngineBlock::new);
    public static final Block STIRLING_ENGINE = register("stirling_engine", StirlingEngineBlock::new);
    public static final Block CREATIVE_ENGINE = register("creative_engine", CreativeEngineBlock::new);
    public static final Block CREATIVE_SINK = register("creative_sink", CreativeSinkBlock::new);

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        return register(name, blockFactory, BlockItem::new);
    }

    private static Block register(
            String name,
            Function<BlockBehaviour.Properties, Block> blockFactory,
            java.util.function.BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        // Create a registry key for the block
        ResourceKey<Block> blockKey = keyOfBlock(name);

        // Create the block instance (1.21.2+ requires the key to be present in the settings at construction time)
        Block block = blockFactory.apply(BlockBehaviour.Properties.of().setId(blockKey));

        // Items need to be registered with a different type of registry key, but the ID can be the same.
        ResourceKey<Item> itemKey = keyOfItem(name);
        BlockItem blockItem = itemFactory.apply(
                block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering power blocks");
    }
}
