package com.logistics.pipe.registry;

import com.logistics.LogisticsMod;
import com.logistics.pipe.PipeTypes;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.item.ModularPipeBlockItem;
import java.util.function.BiFunction;
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

public final class PipeBlocks {
    private PipeBlocks() {}

    private static final String DOMAIN = "pipe/";

    // Transport Pipes
    public static final Block STONE_TRANSPORT_PIPE =
            register("stone_transport_pipe", settings -> new PipeBlock(settings, PipeTypes.STONE_TRANSPORT_PIPE));

    public static final Block ITEM_PASSTHROUGH_PIPE =
            register("item_passthrough_pipe", settings -> new PipeBlock(settings, PipeTypes.ITEM_PASSTHROUGH_PIPE));

    public static final Block COPPER_TRANSPORT_PIPE = register(
            "copper_transport_pipe",
            settings -> new PipeBlock(settings, PipeTypes.COPPER_TRANSPORT_PIPE),
            ModularPipeBlockItem::new);

    public static final Block ITEM_EXTRACTOR_PIPE =
            register("item_extractor_pipe", settings -> new PipeBlock(settings, PipeTypes.ITEM_EXTRACTOR));

    public static final Block ITEM_MERGER_PIPE =
            register("item_merger_pipe", settings -> new PipeBlock(settings, PipeTypes.ITEM_MERGER));

    public static final Block GOLD_TRANSPORT_PIPE =
            register("gold_transport_pipe", settings -> new PipeBlock(settings, PipeTypes.GOLD_TRANSPORT));

    public static final Block ITEM_FILTER_PIPE =
            register("item_filter_pipe", settings -> new PipeBlock(settings, PipeTypes.ITEM_FILTER));

    public static final Block ITEM_INSERTION_PIPE =
            register("item_insertion_pipe", settings -> new PipeBlock(settings, PipeTypes.ITEM_INSERTION));

    public static final Block ITEM_VOID_PIPE =
            register("item_void_pipe", settings -> new PipeBlock(settings, PipeTypes.ITEM_VOID));

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        return register(name, blockFactory, BlockItem::new);
    }

    private static Block register(
            String name,
            Function<BlockBehaviour.Properties, Block> blockFactory,
            BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        // Create a registry key for the block
        ResourceKey<Block> blockKey = keyOfBlock(name);

        // Create the block instance (1.21.2+ requires the key to be present in the settings at construction time)
        Block block = blockFactory.apply(BlockBehaviour.Properties.of().setId(blockKey));

        // Items need to be registered with a different type of registry key, but the ID can be the same.
        ResourceKey<Item> itemKey = keyOfItem(name);
        BlockItem blockItem =
                itemFactory.apply(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(
                Registries.BLOCK, Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering pipe blocks");

        // Set back-references so pipes can derive their model identifiers from the block registry name
        PipeTypes.STONE_TRANSPORT_PIPE.setPipeBlock((PipeBlock) STONE_TRANSPORT_PIPE);
        PipeTypes.ITEM_PASSTHROUGH_PIPE.setPipeBlock((PipeBlock) ITEM_PASSTHROUGH_PIPE);
        PipeTypes.COPPER_TRANSPORT_PIPE.setPipeBlock((PipeBlock) COPPER_TRANSPORT_PIPE);
        PipeTypes.ITEM_EXTRACTOR.setPipeBlock((PipeBlock) ITEM_EXTRACTOR_PIPE);
        PipeTypes.ITEM_MERGER.setPipeBlock((PipeBlock) ITEM_MERGER_PIPE);
        PipeTypes.GOLD_TRANSPORT.setPipeBlock((PipeBlock) GOLD_TRANSPORT_PIPE);
        PipeTypes.ITEM_FILTER.setPipeBlock((PipeBlock) ITEM_FILTER_PIPE);
        PipeTypes.ITEM_INSERTION.setPipeBlock((PipeBlock) ITEM_INSERTION_PIPE);
        PipeTypes.ITEM_VOID.setPipeBlock((PipeBlock) ITEM_VOID_PIPE);

        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.1 => v0.2
        addBlockAlias("item_sensor_pipe", COPPER_TRANSPORT_PIPE);
        addItemAlias("item_sensor_pipe", COPPER_TRANSPORT_PIPE.asItem());

        // v0.2 => v0.3
        addBlockAlias("stone_transport_pipe", STONE_TRANSPORT_PIPE);
        addBlockAlias("item_passthrough_pipe", ITEM_PASSTHROUGH_PIPE);
        addBlockAlias("copper_transport_pipe", COPPER_TRANSPORT_PIPE);
        addBlockAlias("item_extractor_pipe", ITEM_EXTRACTOR_PIPE);
        addBlockAlias("item_merger_pipe", ITEM_MERGER_PIPE);
        addBlockAlias("gold_transport_pipe", GOLD_TRANSPORT_PIPE);
        addBlockAlias("item_filter_pipe", ITEM_FILTER_PIPE);
        addBlockAlias("item_insertion_pipe", ITEM_INSERTION_PIPE);
        addBlockAlias("item_void_pipe", ITEM_VOID_PIPE);

        addItemAlias("stone_transport_pipe", STONE_TRANSPORT_PIPE.asItem());
        addItemAlias("item_passthrough_pipe", ITEM_PASSTHROUGH_PIPE.asItem());
        addItemAlias("copper_transport_pipe", COPPER_TRANSPORT_PIPE.asItem());
        addItemAlias("item_extractor_pipe", ITEM_EXTRACTOR_PIPE.asItem());
        addItemAlias("item_merger_pipe", ITEM_MERGER_PIPE.asItem());
        addItemAlias("gold_transport_pipe", GOLD_TRANSPORT_PIPE.asItem());
        addItemAlias("item_filter_pipe", ITEM_FILTER_PIPE.asItem());
        addItemAlias("item_insertion_pipe", ITEM_INSERTION_PIPE.asItem());
        addItemAlias("item_void_pipe", ITEM_VOID_PIPE.asItem());
    }

    private static void addBlockAlias(String name, Block block) {
        BuiltInRegistries.BLOCK.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, name), BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void addItemAlias(String name, Item item) {
        BuiltInRegistries.ITEM.addAlias(
                Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, name), BuiltInRegistries.ITEM.getKey(item));
    }
}
