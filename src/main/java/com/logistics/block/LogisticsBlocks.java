package com.logistics.block;

import com.logistics.LogisticsMod;
import com.logistics.pipe.PipeTypes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class LogisticsBlocks {
    // Transport Pipes
    public static final Block STONE_TRANSPORT_PIPE = register(
            "stone_transport_pipe",
            settings -> new PipeBlock(settings, PipeTypes.STONE_TRANSPORT_PIPE),
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.GRAY)
                    .strength(2.0f, 6.0f)
                    .sounds(BlockSoundGroup.STONE)
                    .nonOpaque()
    );

    public static final Block COPPER_TRANSPORT_PIPE = register(
        "copper_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.COPPER_TRANSPORT_PIPE),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.GRAY)
            .strength(2.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );

    public static final Block ITEM_EXTRACTOR_PIPE = register(
        "item_extractor_pipe",
        settings -> new PipeBlock(settings, PipeTypes.ITEM_EXTRACTOR),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.OAK_TAN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.WOOD)
            .nonOpaque()
    );

    public static final Block ITEM_MERGER_PIPE = register(
        "item_merger_pipe",
        settings -> new PipeBlock(settings, PipeTypes.ITEM_MERGER),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.IRON_GRAY)
            .strength(3.0f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
    );

    public static final Block GOLD_TRANSPORT_PIPE = register(
        "gold_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.GOLD_TRANSPORT),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.GOLD)
            .strength(0.5f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
    );

    public static final Block ITEM_FILTER_PIPE = register(
        "item_filter_pipe",
        settings -> new PipeBlock(settings, PipeTypes.ITEM_FILTER),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.DIAMOND_BLUE)
            .strength(3.0f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
    );

    public static final Block ITEM_SENSOR_PIPE = register(
        "item_sensor_pipe",
        settings -> new PipeBlock(settings, PipeTypes.ITEM_SENSOR),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.OFF_WHITE)
            .strength(2.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );

    public static final Block ITEM_VOID_PIPE = register(
        "item_void_pipe",
        settings -> new PipeBlock(settings, PipeTypes.ITEM_VOID),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.BLACK)
            .strength(50.0f, 1200.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );

    private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings settings) {
        // Create a registry key for the block
        RegistryKey<Block> blockKey = keyOfBlock(name);

        // Create the block instance (1.21.2+ requires the key to be present in the settings at construction time)
        Block block = blockFactory.apply(settings.registryKey(blockKey));

        // Items need to be registered with a different type of registry key, but the ID can be the same.
        RegistryKey<Item> itemKey = keyOfItem(name);
        BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
        Registry.register(Registries.ITEM, itemKey, blockItem);

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static RegistryKey<Block> keyOfBlock(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, name));
    }

    private static RegistryKey<Item> keyOfItem(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, name));
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering blocks");

        // Set back-references so pipes can derive their model identifiers from the block registry name
        PipeTypes.STONE_TRANSPORT_PIPE.setPipeBlock((PipeBlock) STONE_TRANSPORT_PIPE);
        PipeTypes.COPPER_TRANSPORT_PIPE.setPipeBlock((PipeBlock) COPPER_TRANSPORT_PIPE);
        PipeTypes.ITEM_EXTRACTOR.setPipeBlock((PipeBlock) ITEM_EXTRACTOR_PIPE);
        PipeTypes.ITEM_MERGER.setPipeBlock((PipeBlock) ITEM_MERGER_PIPE);
        PipeTypes.GOLD_TRANSPORT.setPipeBlock((PipeBlock) GOLD_TRANSPORT_PIPE);
        PipeTypes.ITEM_FILTER.setPipeBlock((PipeBlock) ITEM_FILTER_PIPE);
        PipeTypes.ITEM_SENSOR.setPipeBlock((PipeBlock) ITEM_SENSOR_PIPE);
        PipeTypes.ITEM_VOID.setPipeBlock((PipeBlock) ITEM_VOID_PIPE);
    }
}
