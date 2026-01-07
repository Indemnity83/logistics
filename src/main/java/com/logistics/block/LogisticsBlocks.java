package com.logistics.block;

import com.logistics.LogisticsMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class LogisticsBlocks {
    // Transport Pipes
    public static final Block COBBLESTONE_PIPE = registerBlock("cobblestone_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(2.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
        )
    );

    public static final Block STONE_PIPE = registerBlock("stone_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(1.5f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
        )
    );

    public static final Block WOOD_PIPE = registerBlock("wood_pipe",
        () -> new WoodenPipeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.OAK_TAN)
            .strength(0.5f)
            .sounds(BlockSoundGroup.WOOD)
            .nonOpaque()
        )
    );

    public static final Block IRON_PIPE = registerBlock("iron_pipe",
        () -> new IronPipeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.IRON_GRAY)
            .strength(3.0f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
        )
    );

    public static final Block GOLD_PIPE = registerBlock("gold_pipe",
        () -> new GoldPipeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.GOLD)
            .strength(0.5f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
        )
    );

    public static final Block DIAMOND_PIPE = registerBlock("diamond_pipe",
        () -> new PipeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.DIAMOND_BLUE)
            .strength(3.0f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
        )
    );

    public static final Block COPPER_PIPE = registerBlock("copper_pipe",
        () -> new CopperPipeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.ORANGE)
            .strength(2.5f, 6.0f)
            .sounds(BlockSoundGroup.METAL)
            .nonOpaque()
        )
    );

    public static final Block VOID_PIPE = registerBlock("void_pipe",
        () -> new VoidPipeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.BLACK)
            .strength(50.0f, 1200.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque()
        )
    );

    private static Block registerBlock(String name, Supplier<Block> supplier) {
        Block block = supplier.get();
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(LogisticsMod.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(LogisticsMod.MOD_ID, name),
            new BlockItem(block, new Item.Settings()));
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering blocks");
    }
}
