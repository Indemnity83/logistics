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
    public static final Block COBBLESTONE_TRANSPORT_PIPE = register(
        "cobblestone_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.BASIC_TRANSPORT),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(2.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );

    public static final Block STONE_TRANSPORT_PIPE = register(
        "stone_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.BASIC_TRANSPORT),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(1.5f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );

    public static final Block WOODEN_TRANSPORT_PIPE = register(
        "wooden_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.WOOD_TRANSPORT),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.OAK_TAN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.WOOD)
            .nonOpaque()
    );

    public static final Block IRON_TRANSPORT_PIPE = register(
        "iron_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.IRON_TRANSPORT),
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

    public static final Block DIAMOND_TRANSPORT_PIPE = register(
        "diamond_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.DIAMOND_TRANSPORT),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.DIAMOND_BLUE)
            .strength(3.0f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
    );

    public static final Block COPPER_TRANSPORT_PIPE = register(
        "copper_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.COPPER_TRANSPORT),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.ORANGE)
            .strength(2.5f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
    );

    public static final Block QUARTZ_TRANSPORT_PIPE = register(
        "quartz_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.QUARTZ_TRANSPORT),
        AbstractBlock.Settings.create()
            .mapColor(MapColor.OFF_WHITE)
            .strength(2.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
    );

    public static final Block VOID_TRANSPORT_PIPE = register(
        "void_transport_pipe",
        settings -> new PipeBlock(settings, PipeTypes.VOID_TRANSPORT),
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
    }
}
