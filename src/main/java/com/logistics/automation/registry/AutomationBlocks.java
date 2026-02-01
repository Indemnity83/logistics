package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
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

public final class AutomationBlocks {
    private AutomationBlocks() {}

    private static final String DOMAIN = "automation/";

    public static final Block LASER_QUARRY = register("laser_quarry", LaserQuarryBlock::new);
    public static final Block LASER_QUARRY_FRAME = registerNoItem(
            "laser_quarry_frame", LaserQuarryFrameBlock::new, settings -> settings.strength(-1.0f, 3600000.0f)
                    .nonOpaque()
                    .dropsNothing()
                    .ticksRandomly());

    private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory) {
        return register(name, blockFactory, BlockItem::new);
    }

    private static Block register(
            String name,
            Function<AbstractBlock.Settings, Block> blockFactory,
            java.util.function.BiFunction<Block, Item.Settings, BlockItem> itemFactory) {
        // Create a registry key for the block
        RegistryKey<Block> blockKey = keyOfBlock(name);

        // Create the block instance (1.21.2+ requires the key to be present in the settings at construction time)
        Block block = blockFactory.apply(AbstractBlock.Settings.create().registryKey(blockKey));

        // Items need to be registered with a different type of registry key, but the ID can be the same.
        RegistryKey<Item> itemKey = keyOfItem(name);
        BlockItem blockItem = itemFactory.apply(
                block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
        Registry.register(Registries.ITEM, itemKey, blockItem);

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static Block registerNoItem(String name, Function<AbstractBlock.Settings, Block> blockFactory) {
        return registerNoItem(name, blockFactory, Function.identity());
    }

    private static Block registerNoItem(
            String name,
            Function<AbstractBlock.Settings, Block> blockFactory,
            Function<AbstractBlock.Settings, AbstractBlock.Settings> settingsFactory) {
        RegistryKey<Block> blockKey = keyOfBlock(name);
        AbstractBlock.Settings settings =
                settingsFactory.apply(AbstractBlock.Settings.create().registryKey(blockKey));
        Block block = blockFactory.apply(settings);
        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static RegistryKey<Block> keyOfBlock(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    private static RegistryKey<Item> keyOfItem(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    public static void initialize() {
        LogisticsMod.LOGGER.info("Registering automation blocks");
        registerLegacyAliases();
    }

    private static void registerLegacyAliases() {
        // v0.2 => v0.3
        addBlockAlias("quarry", LASER_QUARRY);
        addItemAlias("quarry", LASER_QUARRY.asItem());
        addBlockAlias("quarry_frame", LASER_QUARRY_FRAME);
        // v0.3 => v0.4 (quarry renamed to laser_quarry)
        addBlockAlias("automation/quarry", LASER_QUARRY);
        addItemAlias("automation/quarry", LASER_QUARRY.asItem());
        addBlockAlias("automation/quarry_frame", LASER_QUARRY_FRAME);
    }

    private static void addBlockAlias(String name, Block block) {
        Registries.BLOCK.addAlias(Identifier.of(LogisticsMod.MOD_ID, name), Registries.BLOCK.getId(block));
    }

    private static void addItemAlias(String name, Item item) {
        Registries.ITEM.addAlias(Identifier.of(LogisticsMod.MOD_ID, name), Registries.ITEM.getId(item));
    }
}
