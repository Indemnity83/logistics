package com.logistics;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.block.BlockEntitySupplier;
import com.logistics.core.lib.block.BlockEntityTypeFactory;
import com.logistics.core.lib.platform.PlatformService;
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

public class LogisticsMod {
    public static final String MOD_ID = "logistics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    protected String domain() {
        return "";
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
            BlockEntitySupplier<T> factory,
            Block... blocks) {
        BlockEntityType<T> blockEntityType = BlockEntityTypeFactory.INSTANCE.build(factory, blocks);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, domainResource(name).toIdentifier(), blockEntityType);
    }

    protected <T extends AbstractContainerMenu> MenuType<T> registerMenuType(
            String name,
            MenuType.MenuSupplier<T> factory) {
        MenuType<T> menuType = new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS);
        return Registry.register(BuiltInRegistries.MENU, domainResource(name).toIdentifier(), menuType);
    }

    protected void registerItemAlias(String name, Item item) {
        PlatformService.INSTANCE.registerAlias(BuiltInRegistries.ITEM, modId(name).toIdentifier(), item);
    }

    protected void registerBlockAlias(String name, Block block) {
        PlatformService.INSTANCE.registerAlias(BuiltInRegistries.BLOCK, modId(name).toIdentifier(), block);
    }

    protected void registerBlockEntityAlias(String name, BlockEntityType<?> blockEntityType) {
        PlatformService.INSTANCE.registerAlias(
                BuiltInRegistries.BLOCK_ENTITY_TYPE, modId(name).toIdentifier(), blockEntityType);
    }
}
