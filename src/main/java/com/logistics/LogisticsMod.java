package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.bootstrap.DomainBootstraps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;
import java.util.function.Function;

public class LogisticsMod implements ModInitializer {
    public static final String MOD_ID = "logistics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    protected String domain() {
        return "";
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing {}", MOD_ID);

        for (DomainBootstrap bootstrap : DomainBootstraps.all()) {
            bootstrap.initCommon();
        }
    }

    // TODO(post-1.0): Consider creating ResourceHelper in core.lib.resource (like NbtCompat)
    //  This would centralize all Identifier API calls in a compatibility layer to better
    //  facilitate cross-version cherry-picking. Proposed API:
    //    ResourceHelper.id("path") // logistics namespace
    //    ResourceHelper.parse("namespace:path") // from strings
    //    ResourceHelper.of("namespace", "path") // arbitrary
    //  Benefits: cleaner API, consistent with NbtCompat pattern, easier version migrations
    //  Trade-off: requires creating new package and moving helpers from LogisticsMod

    public static ResourceLocation getIdentifier(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Parse an identifier from a string like "namespace:path".
     * Use when reading identifiers from JSON, NBT, or other external sources.
     */
    public static ResourceLocation parseIdentifier(String id) {
        return ResourceLocation.parse(id);
    }

    /**
     * Create an identifier from namespace and path.
     * Use when you need non-Logistics namespaces (e.g., "minecraft", other mods).
     */
    public static ResourceLocation createIdentifier(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    // TODO(pre-1.0): Consider if domain/ separator should be flattened to domain_
    //  Current: logistics:pipe/copper_transport_pipe
    //  Alternative: logistics:pipe_copper_transport_pipe
    //  Trade-off: Slash aids internal organization but is unconventional for mod IDs.
    //  Changing requires alias migration for existing worlds.
    protected ResourceLocation getDomainResourceLocation(String name) {
        String d = domain();
        return getIdentifier(d.isEmpty() ? name : d + "/" + name);
    }

    /**
     * Creates an identifier for a block model resource.
     * Automatically prepends "block/" and the domain path.
     * Example: LogisticsPipe.blockModelResourceLocation("copper_pipe_core") → logistics:block/pipe/copper_pipe_core
     */
    protected ResourceLocation getBlockModelResourceLocation(String name) {
        String d = domain();
        return getIdentifier(d.isEmpty() ? "block/" + name : "block/" + d + "/" + name);
    }

    protected Item registerItem(String name, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, getDomainResourceLocation(name));
        Item item = itemFactory.apply(new Item.Properties());

        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    protected Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, getDomainResourceLocation(name));
        Block block = blockFactory.apply(BlockBehaviour.Properties.of());

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    protected Block registerBlockWithItem(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        return registerBlockWithItem(name, blockFactory, BlockItem::new);
    }

    protected Block registerBlockWithItem(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        Block block = registerBlock(name, blockFactory);
        registerItem(name, props -> itemFactory.apply(block, props));

        return block;
    }

    protected <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(
            String name,
            FabricBlockEntityTypeBuilder.Factory<T> factory,
            Block... blocks) {
        ResourceLocation identifier = getDomainResourceLocation(name);
        BlockEntityType<T> blockEntityType = FabricBlockEntityTypeBuilder.create(factory, blocks).build();
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, identifier, blockEntityType);
    }

    protected <T extends AbstractContainerMenu> MenuType<T> registerMenuType(
            String name,
            MenuType.MenuSupplier<T> factory) {
        ResourceLocation identifier = getDomainResourceLocation(name);
        MenuType<T> menuType = new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS);
        return Registry.register(BuiltInRegistries.MENU, identifier, menuType);
    }

    protected void registerItemAlias(String name, Item item) {
        ResourceLocation oldItem = getIdentifier(name);
        ResourceLocation newItem = BuiltInRegistries.ITEM.getKey(item);

        if (newItem != null) {
            BuiltInRegistries.ITEM.addAlias(oldItem, newItem);
        }
    }

    protected void registerBlockAlias(String name, Block block) {
        ResourceLocation oldBlock = getIdentifier(name);
        ResourceLocation newBlock = BuiltInRegistries.BLOCK.getKey(block);

        if (newBlock != null) {
            BuiltInRegistries.BLOCK.addAlias(oldBlock, newBlock);
        }
    }

    protected void registerBlockEntityAlias(String name, BlockEntityType<?> blockEntityType) {
        ResourceLocation oldBlockEntity = getIdentifier(name);
        ResourceLocation newBlockEntity = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType);

        if(newBlockEntity != null) {
            BuiltInRegistries.BLOCK_ENTITY_TYPE.addAlias(oldBlockEntity, newBlockEntity);
        }
    }
}
