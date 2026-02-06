package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.bootstrap.DomainBootstraps;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
        LOGGER.info("Initializing" + MOD_ID);

        for (DomainBootstrap bootstrap : DomainBootstraps.all()) {
            bootstrap.initCommon();
        }
    }

    public static Identifier getIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // TODO(pre-1.0): Consider if domain/ separator should be flattened to domain_
    //  Current: logistics:pipe/copper_transport_pipe
    //  Alternative: logistics:pipe_copper_transport_pipe
    //  Trade-off: Slash aids internal organization but is unconventional for mod IDs.
    //  Changing requires alias migration for existing worlds.
    @NonNull protected Identifier getDomainIdentifier(String name) {
        return getIdentifier(domain() + "/" + name);
    }

    protected Item registerItem(String name, Function<Item.Properties, Item> itemFactory) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, getDomainIdentifier(name));
        Item item = itemFactory.apply(new Item.Properties().setId(itemKey));

        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    protected Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> blockFactory) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, getDomainIdentifier(name));
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
        Identifier identifier = getDomainIdentifier(name);
        BlockEntityType<T> blockEntities = FabricBlockEntityTypeBuilder.create(factory, blocks).build();
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, identifier, blockEntities);
    }

    protected void registerItemAlias(String name, Item item) {
        Identifier oldItem = getIdentifier(name);
        Identifier newItem = BuiltInRegistries.ITEM.getKey(item);

        BuiltInRegistries.ITEM.addAlias(oldItem, newItem);
    }

    protected void registerBlockAlias(String name, Block block) {
        Identifier oldBlock = getIdentifier(name);
        Identifier newBlock = BuiltInRegistries.BLOCK.getKey(block);

        BuiltInRegistries.BLOCK.addAlias(oldBlock, newBlock);
    }

    protected void registerBlockEntityAlias(String name, BlockEntityType<?> blockEntityType) {
        Identifier oldBlockEntity = getIdentifier(name);
        Identifier newBlockEntity = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType);

        if(newBlockEntity != null) {
            BuiltInRegistries.BLOCK_ENTITY_TYPE.addAlias(oldBlockEntity, newBlockEntity);
        }
    }
}
