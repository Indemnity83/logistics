package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.bootstrap.DomainBootstraps;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import org.jspecify.annotations.NonNull;
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

        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(container ->
            ResourceManagerHelper.registerBuiltinResourcePack(
                modId("classic_crafting").toIdentifier(),
                container,
                ResourcePackActivationType.NORMAL
            )
        );
    }

    /**
     * Creates a ResourceId for a Logistics-namespaced identifier.
     * <p>
     * Example: {@code modId("pipe/copper")} → {@code logistics:pipe/copper}
     *
     * @param path the resource path
     * @return a new ResourceId in the Logistics namespace
     */
    public static ResourceId modId(String path) {
        return ResourceId.in(MOD_ID, path);
    }


    // TODO(pre-1.0): Consider if domain/ separator should be flattened to domain_
    //  Current: logistics:pipe/copper_transport_pipe
    //  Alternative: logistics:pipe_copper_transport_pipe
    //  Trade-off: Slash aids internal organization but is unconventional for mod IDs.
    //  Changing requires alias migration for existing worlds.
    @NonNull protected ResourceId domainResource(String name) {
        String d = domain();
        return modId(d.isEmpty() ? name : d + "/" + name);
    }

    /**
     * Creates an identifier for a block model resource.
     * Automatically prepends "block/" and the domain path.
     * Example: LogisticsPipe.blockModelIdentifier("copper_pipe_core") → logistics:block/pipe/copper_pipe_core
     */
    @NonNull protected ResourceId domainModelResource(String name) {
        String d = domain();
        return modId(d.isEmpty() ? "block/" + name : "block/" + d + "/" + name);
    }

    protected Item registerItem(String name, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, domainResource(name).toIdentifier());
        Item item = itemFactory.apply(new Item.Properties().setId(itemKey));

        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    protected Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, domainResource(name).toIdentifier());
        Block block = blockFactory.apply(BlockBehaviour.Properties.of().setId(blockKey));

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    protected Block registerBlockWithItem(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        return registerBlockWithItem(name, blockFactory, BlockItem::new);
    }

    protected Block registerBlockWithItem(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        Block block = registerBlock(name, blockFactory);
        registerItem(name, props -> itemFactory.apply(block, props.useBlockDescriptionPrefix()));

        return block;
    }

    protected <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(
            String name,
            FabricBlockEntityTypeBuilder.Factory<T> factory,
            Block... blocks) {
        BlockEntityType<T> blockEntityType = FabricBlockEntityTypeBuilder.create(factory, blocks).build();
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, domainResource(name).toIdentifier(), blockEntityType);
    }

    protected <T extends AbstractContainerMenu> MenuType<T> registerMenuType(
            String name,
            MenuType.MenuSupplier<T> factory) {
        MenuType<T> menuType = new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS);
        return Registry.register(BuiltInRegistries.MENU, domainResource(name).toIdentifier(), menuType);
    }

    protected void registerItemAlias(String name, Item item) {
        var newItem = BuiltInRegistries.ITEM.getKey(item);
        if (newItem != null) {
            BuiltInRegistries.ITEM.addAlias(modId(name).toIdentifier(), newItem);
        }
    }

    protected void registerBlockAlias(String name, Block block) {
        var newBlock = BuiltInRegistries.BLOCK.getKey(block);
        if (newBlock != null) {
            BuiltInRegistries.BLOCK.addAlias(modId(name).toIdentifier(), newBlock);
        }
    }

    protected void registerBlockEntityAlias(String name, BlockEntityType<?> blockEntityType) {
        var newBlockEntity = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType);
        if(newBlockEntity != null) {
            BuiltInRegistries.BLOCK_ENTITY_TYPE.addAlias(modId(name).toIdentifier(), newBlockEntity);
        }
    }
}
