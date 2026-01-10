package com.logistics.block;

import com.logistics.LogisticsMod;
import com.logistics.pipe.PipeTypes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class LogisticsBlocks {
    // Transport Pipes
    public static final Block COBBLESTONE_TRANSPORT_PIPE = registerBlock("cobblestone_transport_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "cobblestone_transport_pipe")))
            .mapColor(MapColor.STONE_GRAY)
            .strength(2.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque(),
            PipeTypes.BASIC_TRANSPORT
        )
    );

    public static final Block STONE_TRANSPORT_PIPE = registerBlock("stone_transport_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "stone_transport_pipe")))
            .mapColor(MapColor.STONE_GRAY)
            .strength(1.5f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque(),
            PipeTypes.BASIC_TRANSPORT
        )
    );

    public static final Block WOODEN_TRANSPORT_PIPE = registerBlock("wooden_transport_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "wooden_transport_pipe")))
            .mapColor(MapColor.OAK_TAN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.WOOD)
            .nonOpaque(),
            PipeTypes.WOOD_TRANSPORT
        )
    );

    public static final Block IRON_TRANSPORT_PIPE = registerBlock("iron_transport_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "iron_transport_pipe")))
            .mapColor(MapColor.IRON_GRAY)
            .strength(3.0f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque(),
            PipeTypes.IRON_TRANSPORT
        )
    );

    public static final Block GOLD_TRANSPORT_PIPE = registerBlock("gold_transport_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "gold_transport_pipe")))
            .mapColor(MapColor.GOLD)
            .strength(0.5f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque(),
            PipeTypes.GOLD_TRANSPORT
        )
    );

    public static final Block DIAMOND_TRANSPORT_PIPE = registerBlock("diamond_transport_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "diamond_transport_pipe")))
            .mapColor(MapColor.DIAMOND_BLUE)
            .strength(3.0f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque(),
            PipeTypes.DIAMOND_TRANSPORT
        )
    );

    public static final Block COPPER_TRANSPORT_PIPE = registerBlock("copper_transport_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "copper_transport_pipe")))
            .mapColor(MapColor.ORANGE)
            .strength(2.5f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque(),
            PipeTypes.COPPER_TRANSPORT
        )
    );

    public static final Block QUARTZ_TRANSPORT_PIPE = registerBlock("quartz_transport_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "quartz_transport_pipe")))
            .mapColor(MapColor.OFF_WHITE)
            .strength(2.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque(),
            PipeTypes.QUARTZ_TRANSPORT
        )
    );

    public static final Block VOID_TRANSPORT_PIPE = registerBlock("void_transport_pipe",
            () -> new PipeBlock(AbstractBlock.Settings.create()
            .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, "void_transport_pipe")))
            .mapColor(MapColor.BLACK)
            .strength(50.0f, 1200.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque(),
            PipeTypes.VOID_TRANSPORT
        )
    );

    private static Block registerBlock(String name, Supplier<Block> supplier) {
        Block block = supplier.get();
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(LogisticsMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(LogisticsMod.MOD_ID, name),
            new BlockItem(block, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, name)))));
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering blocks");
    }
}
