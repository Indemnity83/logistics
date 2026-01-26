package com.logistics.quarry;

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

public final class QuarryBlocks {
    private QuarryBlocks() {}

    public static final Block QUARRY = register("quarry", QuarryBlock::new);
    public static final Block QUARRY_FRAME = registerNoItem("quarry_frame", QuarryFrameBlock::new);

    private static Block registerNoItem(String name, java.util.function.Function<AbstractBlock.Settings, Block> blockFactory) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, name));
        // Hardness -1 = unbreakable by players, high blast resistance = immune to explosions
        Block block = blockFactory.apply(AbstractBlock.Settings.create()
                .registryKey(blockKey)
                .strength(-1.0f, 3600000.0f)
                .nonOpaque()
                .dropsNothing());
        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static Block register(String name, java.util.function.Function<AbstractBlock.Settings, Block> blockFactory) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, name));
        Block block = blockFactory.apply(AbstractBlock.Settings.create().registryKey(blockKey));

        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, name));
        BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
        Registry.register(Registries.ITEM, itemKey, blockItem);

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering quarry blocks");
    }
}
