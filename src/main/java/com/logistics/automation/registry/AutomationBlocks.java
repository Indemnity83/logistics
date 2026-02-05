package com.logistics.automation.registry;

import com.logistics.LogisticsMod;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
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

public final class AutomationBlocks {
    private AutomationBlocks() {}

    private static final String DOMAIN = "automation/";

    public static final Block LASER_QUARRY = register("laser_quarry", LaserQuarryBlock::new);
    public static final Block LASER_QUARRY_FRAME = registerNoItem(
            "laser_quarry_frame", LaserQuarryFrameBlock::new, settings -> settings.strength(-1.0f, 3600000.0f)
                    .noOcclusion()
                    .noLootTable()
                    .randomTicks());

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

    private static Block registerNoItem(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        return registerNoItem(name, blockFactory, Function.identity());
    }

    private static Block registerNoItem(
            String name,
            Function<BlockBehaviour.Properties, Block> blockFactory,
            Function<BlockBehaviour.Properties, BlockBehaviour.Properties> settingsFactory) {
        ResourceKey<Block> blockKey = keyOfBlock(name);
        BlockBehaviour.Properties settings =
                settingsFactory.apply(BlockBehaviour.Properties.of().setId(blockKey));
        Block block = blockFactory.apply(settings);
        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, DOMAIN + name));
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
        BuiltInRegistries.BLOCK.addAlias(Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, name), BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void addItemAlias(String name, Item item) {
        BuiltInRegistries.ITEM.addAlias(Identifier.fromNamespaceAndPath(LogisticsMod.MOD_ID, name), BuiltInRegistries.ITEM.getKey(item));
    }
}
